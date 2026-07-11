package com.streamix.core.network

import com.streamix.core.model.UpdateInfo
import retrofit2.http.GET
import retrofit2.http.Url

interface UpdateApiService {
    @GET
    suspend fun getUpdateInfo(@Url url: String): UpdateInfo
}
