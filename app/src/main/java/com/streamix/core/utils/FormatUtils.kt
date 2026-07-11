package com.streamix.core.utils

import java.util.Locale

object FormatUtils {
    fun formatViews(views: Long): String {
        return when {
            views >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", views / 1_000_000_000.0)
            views >= 1_000_000 -> String.format(Locale.US, "%.1fM", views / 1_000_000.0)
            views >= 1_000 -> String.format(Locale.US, "%.1fK", views / 1_000.0)
            else -> views.toString()
        }
    }

    fun formatViewsString(viewsString: String): String {
        val views = viewsString.toLongOrNull() ?: return viewsString
        return formatViews(views)
    }

    fun formatDuration(durationSeconds: Long): String {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }
}
