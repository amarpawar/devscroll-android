package dev.devscroll.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import dev.devscroll.icons.computer
import dev.devscroll.icons.event_list
import dev.devscroll.icons.format_align_left
import dev.devscroll.icons.view_headline

/** How [ArticleCard] lays out its title, description and thumbnail. Chosen from the feed's top-bar menu. */
enum class FeedStyle(val label: String, val icon: ImageVector) {
    BIG_THUMBNAILS("big thumbnails", computer),
    SMALL_THUMBNAILS("small thumbnails", event_list),
    TEXT_ONLY("text only", format_align_left),
    COMPACT_LIST("compact list", view_headline),
}
