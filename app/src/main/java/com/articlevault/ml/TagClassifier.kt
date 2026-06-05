package com.articlevault.ml

import android.content.Context
import android.util.Log
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class TagClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val entityExtractor by lazy {
        try {
            EntityExtraction.getClient(
                EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
            )
        } catch (e: Exception) {
            Log.w("TagClassifier", "ML Kit entity extraction unavailable", e)
            null
        }
    }

    private val categoryKeywords = mapOf(
        "Technology" to listOf(
            "software", "hardware", "programming", "developer", "code", "app",
            "android", "ios", "api", "cloud", "saas", "startup", "tech",
            "computer", "data", "algorithm", "machine learning", "ai",
            "artificial intelligence", "blockchain", "crypto", "cybersecurity",
            "chip", "semiconductor", "processor", "gpu", "cpu", "server",
            "database", "open source", "github", "silicon valley", "robotics"
        ),
        "Science" to listOf(
            "research", "study", "scientist", "physics", "chemistry", "biology",
            "experiment", "discovery", "space", "nasa", "astronomy", "genome",
            "climate", "quantum", "particle", "evolution", "fossil", "lab",
            "peer-reviewed", "journal", "hypothesis", "theory", "neuroscience"
        ),
        "Business" to listOf(
            "company", "revenue", "profit", "stock", "market", "ceo", "cfo",
            "investor", "funding", "ipo", "acquisition", "merger", "quarterly",
            "earnings", "valuation", "billion", "million", "venture", "capital",
            "startup", "enterprise", "b2b", "b2c", "economy", "gdp", "inflation"
        ),
        "Health" to listOf(
            "health", "medical", "hospital", "doctor", "patient", "disease",
            "treatment", "vaccine", "drug", "fda", "clinical", "therapy",
            "diagnosis", "symptom", "cancer", "mental health", "wellness",
            "nutrition", "exercise", "pandemic", "virus", "bacteria", "surgery"
        ),
        "Sports" to listOf(
            "game", "match", "team", "player", "score", "championship", "league",
            "tournament", "coach", "stadium", "athlete", "nfl", "nba", "mlb",
            "soccer", "football", "basketball", "baseball", "tennis", "golf",
            "olympics", "world cup", "medal", "season", "playoffs"
        ),
        "Politics" to listOf(
            "president", "senate", "congress", "election", "vote", "democrat",
            "republican", "legislation", "policy", "government", "federal",
            "supreme court", "bill", "lawmaker", "campaign", "political",
            "diplomatic", "sanctions", "tariff", "administration"
        ),
        "Entertainment" to listOf(
            "movie", "film", "music", "album", "song", "actor", "actress",
            "director", "netflix", "spotify", "youtube", "streaming", "concert",
            "celebrity", "award", "grammy", "oscar", "emmy", "box office",
            "tv show", "series", "podcast", "gaming", "video game"
        ),
        "Finance" to listOf(
            "bank", "interest rate", "fed", "federal reserve", "inflation",
            "currency", "forex", "bond", "treasury", "credit", "debt",
            "mortgage", "insurance", "pension", "retirement", "savings",
            "investment", "portfolio", "dividend", "wall street"
        ),
        "Education" to listOf(
            "university", "college", "student", "teacher", "professor",
            "curriculum", "degree", "scholarship", "academic", "classroom",
            "learning", "education", "school", "campus", "enrollment"
        ),
        "Environment" to listOf(
            "climate change", "global warming", "carbon", "emissions",
            "renewable", "solar", "wind energy", "sustainability", "pollution",
            "deforestation", "biodiversity", "ecosystem", "recycling",
            "conservation", "drought", "wildfire"
        )
    )

    suspend fun classify(text: String): List<String> = suspendCancellableCoroutine { cont ->
        val sampleText = if (text.length > 3000) text.take(3000) else text
        val lower = sampleText.lowercase()

        val categoryTags = categoryKeywords.map { (category, keywords) ->
            val matchCount = keywords.count { keyword -> lower.contains(keyword) }
            category to matchCount
        }
            .filter { it.second >= 2 }
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }

        val extractor = entityExtractor
        if (extractor == null) {
            if (!cont.isCancelled) cont.resume(categoryTags)
            return@suspendCancellableCoroutine
        }

        extractor.annotate(sampleText)
            .addOnSuccessListener { annotations ->
                val entityTags = annotations
                    .flatMap { annotation ->
                        annotation.entities.map { entity ->
                            annotation.annotatedText to entity.type
                        }
                    }
                    .filter { (_, type) ->
                        type != Entity.TYPE_ADDRESS &&
                        type != Entity.TYPE_DATE_TIME &&
                        type != Entity.TYPE_EMAIL &&
                        type != Entity.TYPE_FLIGHT_NUMBER &&
                        type != Entity.TYPE_IBAN &&
                        type != Entity.TYPE_ISBN &&
                        type != Entity.TYPE_MONEY &&
                        type != Entity.TYPE_PAYMENT_CARD &&
                        type != Entity.TYPE_PHONE &&
                        type != Entity.TYPE_TRACKING_NUMBER &&
                        type != Entity.TYPE_URL
                    }
                    .distinctBy { (text, _) -> text }
                    .take(10)
                    .map { (text, _) -> text }

                val combined = (categoryTags + entityTags).distinct().take(10)
                if (!cont.isCancelled) cont.resume(combined)
            }
            .addOnFailureListener { _ ->
                if (!cont.isCancelled) cont.resume(categoryTags)
            }

        cont.invokeOnCancellation {
            try { extractor.close() } catch (_: Exception) {}
        }
    }
}
