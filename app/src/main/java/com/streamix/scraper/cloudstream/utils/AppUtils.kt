package com.streamix.scraper.cloudstream.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.streamix.scraper.cloudstream.mvvm.logError
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializerOrNull
import kotlin.reflect.KClass

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
}

val mapper = JsonMapper.builder().addModule(kotlinModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build()!!

@OptIn(InternalSerializationApi::class)
object AppUtils {
    fun Any.toJson(): String {
        if (this is String) return this
        val serializer = this::class.serializerOrNull()
        return if (serializer != null) {
            try {
                @Suppress("UNCHECKED_CAST")
                json.encodeToString(serializer as KSerializer<Any>, this)
            } catch (e: SerializationException) {
                logError(e)
                mapper.writeValueAsString(this)
            }
        } else {
            mapper.writeValueAsString(this)
        }
    }

    fun <T : Any> parseJson(value: String, kClass: KClass<T>): T {
        val serializer = kClass.serializerOrNull()
        if (serializer != null) {
            try {
                return json.decodeFromString(serializer, value)
            } catch (e: SerializationException) {
                logError(e)
            }
        }
        return mapper.readValue(value, kClass.java)
    }

    inline fun <reified T : Any> tryParseJson(value: String?): T? {
        return try {
            val text = value ?: return null
            mapper.readValue<T>(text)
        } catch (_: Exception) {
            null
        }
    }
}
