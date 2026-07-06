package com.streamix

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.streamix.core.utils.NewPipeUtils
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class StreamixApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        NewPipeUtils.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request()
                        val url = request.url.toString()
                        
                        val referer = when {
                            url.contains("okxxx") -> "https://okxxx1.com/"
                            url.contains("pornhat") -> "https://www.pornhat.com/"
                            url.contains("fikfap") -> "https://fikfap.com/"
                            url.contains("pornhub") -> "https://www.pornhub.com/"
                            url.contains("redtube") -> "https://www.redtube.com/"
                            url.contains("hlowb") -> "https://api.hlowb.com/"
                            url.contains("aoneroom") -> "https://api3.aoneroom.com/"
                            else -> "https://www.google.com/"
                        }
                        
                        val newRequest = request.newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                            .header("Referer", referer)
                            .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                            .build()
                        chain.proceed(newRequest)
                    }
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
