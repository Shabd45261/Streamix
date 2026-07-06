package com.streamix.scraper.cloudstream

import com.streamix.scraper.cloudstream.utils.AppUtils.parseJson
import com.streamix.scraper.cloudstream.utils.AppUtils.toJson
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.ResponseParser
import kotlin.reflect.KClass

private val jsonResponseParser = object : ResponseParser {
    override fun <T : Any> parse(text: String, kClass: KClass<T>): T {
        return parseJson(text, kClass)
    }

    override fun <T : Any> parseSafe(text: String, kClass: KClass<T>): T? {
        return try {
            parse(text, kClass)
        } catch (_: Exception) {
            null
        }
    }

    override fun writeValueAsString(obj: Any): String {
        return obj.toJson()
    }
}

const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"

val app = Requests(responseParser = jsonResponseParser).apply {
    defaultHeaders = mapOf("user-agent" to USER_AGENT)
}
