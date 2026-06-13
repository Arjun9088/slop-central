package com.expensetracker.sms

import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ParsedTransaction(
    val amount: Double,
    val merchant: String,
    val date: String,
    val paymentMethod: String,
    val rawDescription: String,
    val dedupHash: String
)

object TransactionParser {

    private val amountPatterns = listOf(
        Regex("""(?i)(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)"""),
        Regex("""(?i)([\d,]+(?:\.\d{1,2})?)\s*(?:rs\.?|inr|₹)"""),
        Regex("""(?i)amount[:\s]*(?:rs\.?|inr|₹)?\s*([\d,]+(?:\.\d{1,2})?)"""),
        Regex("""(?i)debited\s*(?:by|with|for)?\s*(?:rs\.?|inr|₹)?\s*([\d,]+(?:\.\d{1,2})?)"""),
        Regex("""(?i)(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)\s*(?:debited|spent|paid|transferred)"""),
    )

    private val merchantPatterns = listOf(
        Regex("""(?i)(?:spent|paid|transferred)\s+(?:rs\.?|inr|₹)?\s*[\d,]+(?:\.\d{1,2})?\s+(?:at|to|from|@)\s+([A-Za-z0-9][A-Za-z0-9\s&'_/-]{1,40}?)(?:\s*[.,]|\s+on|\s+via|\s+ref|\s+\d|\s+available|\s+balance|\s*$)"""),
        Regex("""(?i)(?:spent|paid|transferred)\s+(?:at|to|from|@)\s+([A-Za-z0-9][A-Za-z0-9\s&'_/-]{1,40}?)(?:\s*[.,]|\s+on|\s+via|\s+ref|\s+\d|\s+available|\s+balance|\s*$)"""),
        Regex("""(?i)(?:at|to|from|@)\s+([A-Za-z0-9][A-Za-z0-9\s&'_/-]{1,40}?)(?:\s+on|\s+via|\s+ref|\s+\d|\s*$)"""),
        Regex("""(?i)(?:merchant|shop|store|vendor)[:\s]+([A-Za-z0-9][A-Za-z0-9\s&'_/-]{1,40})"""),
    )

    private val cardPatterns = listOf(
        Regex("""(?i)(?:card|a/c)\s*(?:no\.?)?\s*(?:xx|X)?(\d{4})"""),
        Regex("""(?i)(?:ending|ending in|last)\s+(\d{4})"""),
    )

    private val upiKeywords = setOf("upi", "gpay", "google pay", "phonepe", "paytm", "bhim", "bharatpe")
    private val cardKeywords = setOf("card", "debit", "credit", "visa", "mastercard", "rupay", "atm")
    private val walletKeywords = setOf("wallet", "paytm wallet", "mobikwik", "freecharge")

    fun parse(sender: String, body: String): ParsedTransaction? {
        val text = "$sender $body"

        if (!isTransactionSms(text)) return null

        val amount = extractAmount(body) ?: return null
        val merchant = extractMerchant(body)
        val date = extractDate(body) ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val paymentMethod = detectPaymentMethod(text)
        val rawDescription = buildDescription(merchant, sender)
        val dedupHash = computeHash(amount, date, merchant)

        return ParsedTransaction(
            amount = amount,
            merchant = merchant,
            date = date,
            paymentMethod = paymentMethod,
            rawDescription = rawDescription,
            dedupHash = dedupHash
        )
    }

    private fun isTransactionSms(text: String): Boolean {
        val lower = text.lowercase()
        val transactionKeywords = listOf(
            "debited", "spent", "paid", "transferred", "withdrawn",
            "purchase", "txn", "transaction", "payment",
            "sent rs", "sent inr", "debited by", "debited with"
        )
        val bankIndicators = listOf(
            "bank", "a/c", "account", "card", "upi", "imps", "neft", "rtgs"
        )
        return transactionKeywords.any { lower.contains(it) } ||
            (bankIndicators.any { lower.contains(it) } && lower.contains(Regex("""(?:rs\.?|inr|₹)\s*[\d,]""")))
    }

    private fun extractAmount(text: String): Double? {
        for (pattern in amountPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0) return amount
            }
        }
        return null
    }

    private fun extractMerchant(text: String): String {
        for (pattern in merchantPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val merchant = match.groupValues[1].trim()
                    .replace(Regex("""\s+"""), " ")
                    .take(50)
                if (merchant.isNotBlank() && merchant.length > 2) return merchant
            }
        }
        return "Unknown"
    }

    private fun extractDate(text: String): String? {
        val datePatterns = listOf(
            Regex("""(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})"""),
            Regex("""(\d{1,2})\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s*(\d{2,4})""", RegexOption.IGNORE_CASE),
        )

        for (pattern in datePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                try {
                    val groups = match.groupValues
                    if (groups.size >= 4) {
                        val day = groups[1].toIntOrNull() ?: continue
                        val monthStr = groups[2]
                        val yearStr = groups[3]

                        val month = monthStr.toIntOrNull() ?: parseMonth(monthStr) ?: continue
                        val year = normalizeYear(yearStr.toIntOrNull() ?: continue)

                        return LocalDate.of(year, month, day).format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                } catch (_: Exception) {
                    continue
                }
            }
        }
        return null
    }

    private fun parseMonth(month: String): Int? {
        return when (month.lowercase().take(3)) {
            "jan" -> 1; "feb" -> 2; "mar" -> 3; "apr" -> 4
            "may" -> 5; "jun" -> 6; "jul" -> 7; "aug" -> 8
            "sep" -> 9; "oct" -> 10; "nov" -> 11; "dec" -> 12
            else -> null
        }
    }

    private fun normalizeYear(year: Int): Int {
        return if (year < 100) 2000 + year else year
    }

    private fun detectPaymentMethod(text: String): String {
        val lower = text.lowercase()
        return when {
            upiKeywords.any { lower.contains(it) } -> "gpay"
            cardKeywords.any { lower.contains(it) } && lower.contains("credit") -> "Credit Card"
            cardKeywords.any { lower.contains(it) } -> "Debit Card"
            walletKeywords.any { lower.contains(it) } -> "Digital Wallet"
            lower.contains("cash") -> "Cash"
            else -> "Debit Card"
        }
    }

    private fun buildDescription(merchant: String, sender: String): String {
        return if (merchant != "Unknown") merchant else {
            val cleanSender = sender.replace(Regex("""[^A-Za-z0-9\s]"""), "").trim()
            if (cleanSender.isNotBlank()) cleanSender.take(30) else "SMS Transaction"
        }
    }

    private fun computeHash(amount: Double, date: String, merchant: String): String {
        val input = "$amount|$date|${merchant.lowercase().trim()}"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray())
        return bytes.take(8).joinToString("") { "%02x".format(it) }
    }
}
