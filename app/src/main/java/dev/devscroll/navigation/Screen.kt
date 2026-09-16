package dev.devscroll.navigation

object Screen {
    const val FEED = "feed"
    const val SAVED = "saved"
    const val SOURCES = "sources"
    const val PROFILE = "profile"
    const val SEARCH = "search"

    /** Deliberately absent from [bottomNavRoutes] — not a tab, so no bottom bar or swipe gesture. */
    const val CONTACT = "contact"
    const val READER = "reader/{articleId}"
    const val READER_ARG = "articleId"

    fun reader(articleId: Long) = "reader/$articleId"

    val bottomNavRoutes = setOf(FEED, SAVED, SOURCES, PROFILE)
}
