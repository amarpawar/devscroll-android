package dev.devscroll.navigation

object Screen {
    const val FEED = "feed"
    const val SAVED = "saved"
    const val SOURCES = "sources"
    const val SEARCH = "search"
    const val READER = "reader/{articleId}"
    const val READER_ARG = "articleId"

    fun reader(articleId: Long) = "reader/$articleId"

    val bottomNavRoutes = setOf(FEED, SAVED, SOURCES)
}
