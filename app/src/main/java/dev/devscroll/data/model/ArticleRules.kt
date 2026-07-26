package dev.devscroll.data.model

/** Shared rules for deriving presentation flags on [Article], applied by every entity mapper. */
object ArticleRules {
    /** An article is "hot" once it passes this many hits. */
    const val HOT_HIT_THRESHOLD = 50
}
