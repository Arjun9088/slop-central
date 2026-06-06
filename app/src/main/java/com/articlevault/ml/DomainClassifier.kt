package com.articlevault.ml

import java.net.URI

object DomainClassifier {

    enum class SiteType(val label: String) {
        BLOG("Blog"),
        CODE("Code"),
        ACADEMIC("Academic"),
        NEWS("News"),
        REFERENCE("Reference"),
        SOCIAL("Social"),
        VIDEO("Video"),
        DOCS("Documentation"),
        TECH_NEWS("Tech News"),
        SHOPPING("Shopping"),
        GOVERNMENT("Government"),
        EDUCATION("Education"),
        OTHER("Other")
    }

    private val domainTypes = mapOf(
        // Blog platforms
        "medium.com" to SiteType.BLOG,
        "substack.com" to SiteType.BLOG,
        "dev.to" to SiteType.BLOG,
        "hashnode.dev" to SiteType.BLOG,
        "wordpress.com" to SiteType.BLOG,
        "blogger.com" to SiteType.BLOG,
        "ghost.io" to SiteType.BLOG,
        "tumblr.com" to SiteType.BLOG,
        "typepad.com" to SiteType.BLOG,
        "bearblog.dev" to SiteType.BLOG,

        // Code
        "github.com" to SiteType.CODE,
        "gitlab.com" to SiteType.CODE,
        "bitbucket.org" to SiteType.CODE,
        "stackoverflow.com" to SiteType.CODE,
        "stackexchange.com" to SiteType.CODE,
        "codepen.io" to SiteType.CODE,
        "replit.com" to SiteType.CODE,
        "codesandbox.io" to SiteType.CODE,

        // Academic
        "arxiv.org" to SiteType.ACADEMIC,
        "pubmed.ncbi.nlm.nih.gov" to SiteType.ACADEMIC,
        "scholar.google.com" to SiteType.ACADEMIC,
        "researchgate.net" to SiteType.ACADEMIC,
        "semanticscholar.org" to SiteType.ACADEMIC,
        "academia.edu" to SiteType.ACADEMIC,
        "jstor.org" to SiteType.ACADEMIC,
        "springer.com" to SiteType.ACADEMIC,
        "nature.com" to SiteType.ACADEMIC,
        "sciencedirect.com" to SiteType.ACADEMIC,

        // News
        "nytimes.com" to SiteType.NEWS,
        "bbc.com" to SiteType.NEWS,
        "bbc.co.uk" to SiteType.NEWS,
        "cnn.com" to SiteType.NEWS,
        "reuters.com" to SiteType.NEWS,
        "theguardian.com" to SiteType.NEWS,
        "washingtonpost.com" to SiteType.NEWS,
        "apnews.com" to SiteType.NEWS,
        "aljazeera.com" to SiteType.NEWS,
        "bloomberg.com" to SiteType.NEWS,
        "economist.com" to SiteType.NEWS,
        "ft.com" to SiteType.NEWS,
        "wsj.com" to SiteType.NEWS,
        "axios.com" to SiteType.NEWS,
        "politico.com" to SiteType.NEWS,

        // Reference
        "wikipedia.org" to SiteType.REFERENCE,
        "wikimedia.org" to SiteType.REFERENCE,
        "wikidata.org" to SiteType.REFERENCE,
        "dictionary.com" to SiteType.REFERENCE,
        "merriam-webster.com" to SiteType.REFERENCE,
        "britannica.com" to SiteType.REFERENCE,
        "howstuffworks.com" to SiteType.REFERENCE,

        // Social
        "twitter.com" to SiteType.SOCIAL,
        "x.com" to SiteType.SOCIAL,
        "reddit.com" to SiteType.SOCIAL,
        "linkedin.com" to SiteType.SOCIAL,
        "facebook.com" to SiteType.SOCIAL,
        "mastodon.social" to SiteType.SOCIAL,
        "threads.net" to SiteType.SOCIAL,
        "hn.algolia.com" to SiteType.SOCIAL,
        "news.ycombinator.com" to SiteType.SOCIAL,

        // Video
        "youtube.com" to SiteType.VIDEO,
        "youtu.be" to SiteType.VIDEO,
        "vimeo.com" to SiteType.VIDEO,
        "twitch.tv" to SiteType.VIDEO,
        "dailymotion.com" to SiteType.VIDEO,

        // Documentation
        "docs.python.org" to SiteType.DOCS,
        "developer.android.com" to SiteType.DOCS,
        "developer.apple.com" to SiteType.DOCS,
        "developer.mozilla.org" to SiteType.DOCS,
        "kotlinlang.org" to SiteType.DOCS,
        "typescriptlang.org" to SiteType.DOCS,
        "rust-lang.org" to SiteType.DOCS,
        "go.dev" to SiteType.DOCS,

        // Tech News
        "techcrunch.com" to SiteType.TECH_NEWS,
        "theverge.com" to SiteType.TECH_NEWS,
        "arstechnica.com" to SiteType.TECH_NEWS,
        "wired.com" to SiteType.TECH_NEWS,
        "engadget.com" to SiteType.TECH_NEWS,
        "gizmodo.com" to SiteType.TECH_NEWS,
        "venturebeat.com" to SiteType.TECH_NEWS,
        "thenextweb.com" to SiteType.TECH_NEWS,
        "hackernews.com" to SiteType.TECH_NEWS,
    )

    fun classify(url: String): SiteType {
        val domain = extractDomain(url).lowercase()

        // Exact match
        domainTypes[domain]?.let { return it }

        // Subdomain match (e.g., blog.medium.com -> medium.com)
        for ((knownDomain, type) in domainTypes) {
            if (domain.endsWith(".$knownDomain")) return type
        }

        // Subdomain prefix match
        val baseDomain = domain.removePrefix("www.")
        domainTypes[baseDomain]?.let { return it }

        // TLD-based classification
        val tld = baseDomain.substringAfterLast(".", "")
        val subdomain = baseDomain.substringBefore(".", "")

        return when {
            tld == "edu" -> SiteType.EDUCATION
            tld == "gov" -> SiteType.GOVERNMENT
            subdomain == "docs" || subdomain == "developer" || subdomain == "dev" || subdomain == "learn" -> SiteType.DOCS
            subdomain == "blog" -> SiteType.BLOG
            subdomain == "news" -> SiteType.NEWS
            else -> SiteType.OTHER
        }
    }

    fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            uri.host?.removePrefix("www.") ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun extractReadingTimeMinutes(wordCount: Int): Int {
        return (wordCount / 238.0).toInt().coerceAtLeast(1)
    }
}
