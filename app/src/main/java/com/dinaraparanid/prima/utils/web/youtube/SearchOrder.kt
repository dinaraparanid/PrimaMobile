package com.dinaraparanid.prima.utils.web.youtube

/** Search order of videos in http search query */

@Deprecated(
    "The YouTube API key is very limited in resources, " +
            "and it will not be enough for users from the Play Market"
)
sealed class SearchOrder(internal val value: String) {
    /** Sorts by date (last uploaded) */
    internal class Date : SearchOrder("date")

    /** Sorts by rating (more likes -> higher position) */
    internal class Rating : SearchOrder("rating")

    /** Sorts by view count (more views -> higher position) */
    internal class ViewCount : SearchOrder("viewCount")

    /** Sorts by relevance */
    internal class Relevance : SearchOrder("relevance")

    /** Sorts by title (ascending) */
    internal class Title : SearchOrder("title")

    /** Sorts by video count */
    internal class VideoCount : SearchOrder("videoCount")
}