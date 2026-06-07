package com.automatelinux.picaStats.data

import com.automatelinux.picaStats.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// Fetches the reshaped stats from the picaStats backend. Usable both from the
// Hilt-injected ViewModel and from the background worker (which constructs its
// own instance via the no-arg secondary constructor).
@Singleton
class StatsRepository @Inject constructor(
    private val client: OkHttpClient,
) {
    constructor() : this(
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build(),
    )

    private val gson = Gson()

    suspend fun fetch(site: String? = null): StatsResponse = withContext(Dispatchers.IO) {
        val base = BuildConfig.STATS_BASE_URL.trimEnd('/')
        val url = buildString {
            append(base).append("/api/stats")
            if (!site.isNullOrBlank()) append("?site=").append(site)
        }
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${BuildConfig.STATS_TOKEN}")
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string()
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}${if (body != null) ": ${body.take(200)}" else ""}")
            }
            if (body.isNullOrBlank()) throw IOException("empty response")
            gson.fromJson(body, StatsResponse::class.java)
        }
    }
}
