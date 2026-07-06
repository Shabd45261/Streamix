package com.streamix.core.network

import retrofit2.http.GET
import retrofit2.http.Url

data class UpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val updateUrl: String,
    val isMandatory: Boolean,
    val changelog: String,
    val instructions: String? = null
)

interface UpdateApiService {
    @GET
    suspend fun checkUpdate(@Url url: String): UpdateInfo
}
