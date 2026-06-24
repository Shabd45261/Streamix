package com.streamix.ui.navigation

sealed class Screen(val route: String) {
    object Home           : Screen("home")
    object Search         : Screen("search?query={query}")
    object Detail         : Screen("detail/{mediaId}/{mediaType}")
    object Library        : Screen("library")
    object Settings       : Screen("settings")
    object Passcode       : Screen("passcode")
    object YoutubeDetail  : Screen("youtube_detail/{videoId}")
    object AdultDetail    : Screen("adult_detail/{pageUrl}")
}
