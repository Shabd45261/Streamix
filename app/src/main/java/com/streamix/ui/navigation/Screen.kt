package com.streamix.ui.navigation

sealed class Screen(val route: String) {
    object Home           : Screen("home")
    object Search         : Screen("search?query={query}")
    object Detail         : Screen("detail/{mediaId}/{mediaType}")
    object Library        : Screen("library")
    object Settings       : Screen("settings")
    object Shorts         : Screen("shorts")
    object YoutubeDetail  : Screen("youtube_detail?videoId={videoId}")
    object YoutubeChannel : Screen("youtube_channel?channelUrl={channelUrl}")
    object YoutubeLogin   : Screen("youtube_login")
    object AdultDetail    : Screen("adult_detail/{pageUrl}")
    object MoviesDetail   : Screen("movies_detail?movieId={movieId}&apiName={apiName}")
}
