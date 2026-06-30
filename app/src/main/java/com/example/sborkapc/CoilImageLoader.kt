/*
© Жиляков Д.Э., 2026. Все права защищены.
*/
package com.example.sborkapc.network

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

fun buildCoilImageLoader(context: Context): ImageLoader {
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            // Логируем каждый запрос на картинку
            Log.d("CoilNet", "Запрос к: ${request.url}")
            val response = chain.proceed(request)
            Log.d("CoilNet", "Ответ от ${request.url}: код ${response.code}")
            response
        }
        .build()

    return ImageLoader.Builder(context)
        .okHttpClient(okHttpClient)
        .logger(DebugLogger())
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.20)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(150L * 1024 * 1024)
                .build()
        }
        .build()
}
