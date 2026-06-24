package com.streamix.core.utils

object UrlUtils {
    /**
     * Resolves an image URL by handling relative paths, missing protocols, 
     * and TMDB specific paths.
     */
    fun resolveImageUrl(path: String?, mediaType: String? = null, contextUrl: String? = null): String {
        if (path.isNullOrBlank()) return ""
        
        val cleanPath = path.trim().replace("\\/", "/")
        
        if (cleanPath.startsWith("http")) return cleanPath
        if (cleanPath.startsWith("//")) return "https:$cleanPath"

        val domain = when {
            contextUrl?.contains("pornhat") == true -> "https://www.pornhat.com"
            contextUrl?.contains("ok.xxx") == true || contextUrl?.contains("okxxx") == true -> "https://ok.xxx"
            contextUrl?.contains("fikfap") == true -> "https://fikfap.com"
            contextUrl?.contains("pornhub") == true -> "https://www.pornhub.com"
            contextUrl?.contains("spankbang") == true -> "https://spankbang.com"
            contextUrl?.contains("xvideos") == true -> "https://www.xvideos.com"
            contextUrl?.contains("redtube") == true -> "https://www.redtube.com"
            contextUrl?.contains("youporn") == true -> "https://www.youporn.com"
            mediaType == "adult" -> {
                when {
                    cleanPath.contains("pornhat") -> "https://www.pornhat.com"
                    cleanPath.contains("ok.xxx") -> "https://ok.xxx"
                    cleanPath.contains("fikfap") -> "https://fikfap.com"
                    else -> "https://www.pornhat.com"
                }
            }
            else -> "https://image.tmdb.org/t/p/w500"
        }

        return if (cleanPath.startsWith("/")) {
            "$domain$cleanPath"
        } else {
            if (mediaType == "adult" && !cleanPath.contains("/")) {
                "$domain/$cleanPath"
            } else if (mediaType != "adult" && !cleanPath.contains(".") && cleanPath.length > 5) {
                "https://image.tmdb.org/t/p/w500/$cleanPath"
            } else if (mediaType == "adult") {
                "$domain/$cleanPath"
            } else {
                cleanPath
            }
        }
    }

    fun getDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (e: Exception) { "" }
    }
}
