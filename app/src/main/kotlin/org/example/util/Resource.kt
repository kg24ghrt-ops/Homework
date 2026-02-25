// util/Resource.kt
package com.openanimelib.util

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}

// util/SeasonUtils.kt
package com.openanimelib.util

import com.openanimelib.domain.model.AnimeSeason
import java.util.Calendar

object SeasonUtils {
    fun getCurrentSeason(): AnimeSeason {
        return when (Calendar.getInstance().get(Calendar.MONTH)) {
            in 0..2 -> AnimeSeason.WINTER
            in 3..5 -> AnimeSeason.SPRING
            in 6..8 -> AnimeSeason.SUMMER
            else -> AnimeSeason.FALL
        }
    }

    fun getCurrentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    fun getSeasonName(season: AnimeSeason): String {
        return when (season) {
            AnimeSeason.WINTER -> "❄️ Winter"
            AnimeSeason.SPRING -> "🌸 Spring"
            AnimeSeason.SUMMER -> "☀️ Summer"
            AnimeSeason.FALL -> "🍂 Fall"
        }
    }
}

// util/Constants.kt
package com.openanimelib.util

object Constants {
    const val ANILIST_BASE_URL = "https://graphql.anilist.co/"
    const val JIKAN_BASE_URL = "https://api.jikan.moe/v4/"
    const val ITEMS_PER_PAGE = 20
    const val SEARCH_DEBOUNCE_MS = 300L
    const val CACHE_DURATION_HOURS = 6

    val ALL_GENRES = listOf(
        "Action", "Adventure", "Comedy", "Drama", "Ecchi",
        "Fantasy", "Horror", "Mahou Shoujo", "Mecha", "Music",
        "Mystery", "Psychological", "Romance", "Sci-Fi",
        "Slice of Life", "Sports", "Supernatural", "Thriller"
    )
}

// util/Extensions.kt
package com.openanimelib.util

import java.text.SimpleDateFormat
import java.util.*

fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }
}

fun Float.toRatingString(): String = String.format("%.1f", this)