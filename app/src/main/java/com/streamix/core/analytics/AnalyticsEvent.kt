package com.streamix.core.analytics

object AnalyticsEvent {
    // Installation
    const val APP_OPENED = "app_opened"
    const val FIRST_OPEN = "first_open"
    const val APP_UPDATE = "app_update"
    
    // Video
    const val VIDEO_OPENED = "video_opened"
    const val VIDEO_STARTED = "video_started"
    const val VIDEO_PAUSED = "video_paused"
    const val VIDEO_RESUMED = "video_resumed"
    const val VIDEO_COMPLETED = "video_completed"
    const val VIDEO_STOPPED = "video_stopped"
    const val VIDEO_SEEK_FORWARD = "video_seek_forward"
    const val VIDEO_SEEK_BACKWARD = "video_seek_backward"
    const val BUFFERING_STARTED = "buffering_started"
    const val BUFFERING_FINISHED = "buffering_finished"
    const val QUALITY_SELECTED = "quality_selected"
    const val SUBTITLE_SELECTED = "subtitle_selected"
    const val AUDIO_TRACK_SELECTED = "audio_track_selected"
    const val PLAYBACK_SPEED_CHANGED = "playback_speed_changed"
    const val PIP_ENTERED = "pip_entered"
    const val PIP_EXITED = "pip_exited"

    // Music
    const val SONG_PLAYED = "song_played"
    const val SONG_PAUSED = "song_paused"
    const val SONG_RESUMED = "song_resumed"
    const val SONG_COMPLETED = "song_completed"
    const val SHUFFLE_CHANGED = "shuffle_changed"
    const val REPEAT_MODE_CHANGED = "repeat_mode_changed"
    const val PLAYLIST_OPENED = "playlist_opened"
    const val PLAYLIST_CREATED = "playlist_created"
    const val FAVORITE_ADDED = "favorite_added"
    const val FAVORITE_REMOVED = "favorite_removed"
    
    // Search
    const val SEARCH_STARTED = "search_started"
    const val SEARCH_QUERY = "search_query"
    const val SEARCH_RESULT_COUNT = "search_result_count"
    const val SEARCH_SUCCESS = "search_success"
    const val SEARCH_FAILED = "search_failed"
    const val SEARCH_FILTER_USED = "search_filter_used"

    // Download
    const val DOWNLOAD_STARTED = "download_started"
    const val DOWNLOAD_COMPLETED = "download_completed"
    const val DOWNLOAD_FAILED = "download_failed"
    const val DOWNLOAD_CANCELLED = "download_cancelled"

    // Settings
    const val SETTING_CHANGED = "setting_changed"

    // Errors
    const val ERROR_NETWORK = "error_network"
    const val ERROR_API = "error_api"
    const val ERROR_PLAYBACK = "error_playback"
    const val ERROR_EXCEPTION = "error_exception"

    // Custom
    const val CONTENT_OPENED = "content_opened"
    const val WATCHLIST_ADDED = "watchlist_added"
    const val WATCHLIST_REMOVED = "watchlist_removed"
    
    object Param {
        const val TITLE = "title"
        const val ID = "id"
        const val MEDIA_TYPE = "media_type"
        const val CATEGORY = "category"
        const val QUERY = "query"
        const val SEARCH_RESULT_COUNT = "search_result_count"
        const val DURATION = "duration"
        const val POSITION = "position"
        const val QUALITY = "quality"
        const val ERROR_MESSAGE = "error_message"
        const val SCREEN_NAME = "screen_name"
        const val SETTING_NAME = "setting_name"
        const val SETTING_VALUE = "setting_value"
        const val SOURCE = "source"
        const val STATUS = "status"
    }
}
