package com.expensetracker.sms

object CategoryClassifier {

    private val categoryKeywords = mapOf(
        "Food and drink" to listOf(
            "swiggy", "zomato", "dominos", "pizza", "mcdonald", "burger", "kfc",
            "subway", "starbucks", "cafe", "restaurant", "food", "dining", "eat",
            "breakfast", "lunch", "dinner", "snack", "tea", "coffee", "juice",
            "biryani", "dosa", "bake", "cook", "grocery", "bigbasket", "blinkit",
            "zepto", "instamart", "dunzo", "fresh", "organic", "milk", "bread",
            "fruit", "vegetable", "meat", "fish", "egg", "chicken", "mutton",
            "bar", "pub", "liquor", "wine", "beer", "beverage"
        ),
        "Transportation" to listOf(
            "uber", "ola", "rapido", "metro", "bus", "train", "irctc", "flight",
            "airline", "indigo", "spicejet", "vistara", "air india", "makemytrip",
            "redbus", "fuel", "petrol", "diesel", "gas", "parking", "toll",
            "auto", "rickshaw", "taxi", "cab", "ride", "transport", "travel",
            "booking", "ticket", "pass", "commute"
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho", "tatacliq",
            "mall", "store", "shop", "mart", "market", "bazaar", "retail",
            "clothing", "fashion", "shoe", "dress", "shirt", "pant", "jeans",
            "electronic", "mobile", "laptop", "gadget", "appliance", "furniture",
            "decor", "gift", "jewelry", "watch", "bag", "accessory", "lens",
            "spectacle", "cosmetic", "beauty", "personal care", "hygiene"
        ),
        "Entertainment" to listOf(
            "netflix", "prime", "hotstar", "disney", "sony", "zee5", "voot",
            "spotify", "youtube", "gaana", "jiosaavan", "movie", "cinema",
            "theatre", "pvr", "inox", "bookmyshow", "ticket", "concert",
            "event", "game", "gaming", "playstation", "xbox", "nintendo",
            "steam", "epic", "sport", "gym", "fitness", "yoga", "salon",
            "spa", "massage", "parlour"
        ),
        "Bills and utilities" to listOf(
            "electricity", "power", "water", "gas", "bill", "recharge",
            "broadband", "wifi", "internet", "mobile", "postpaid", "prepaid",
            "jio", "airtel", "vi", "bsnl", "tata", "insurance", "premium",
            "emi", "loan", "rent", "maintenance", "society", "property",
            "tax", "gst", "income tax", "mutual fund", "sip", "investment",
            "electric", "utility", "cable", "dth", "tata sky", "dish"
        )
    )

    fun classify(description: String): String {
        val lower = description.lowercase()

        var bestCategory = "Other"
        var bestScore = 0

        for ((category, keywords) in categoryKeywords) {
            var score = 0
            for (keyword in keywords) {
                if (lower.contains(keyword)) {
                    score += keyword.length
                }
            }
            if (score > bestScore) {
                bestScore = score
                bestCategory = category
            }
        }

        return bestCategory
    }
}
