package com.automatelinux.picaStats.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.automatelinux.picaStats.data.StatsRepository

// Hourly background poll: fetch picawish stats, post a notification with the
// current visit count and the change since the last check.
class StatsWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val data = StatsRepository().fetch()
            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val lastToday = prefs.getInt(KEY_LAST_TODAY, -1)
            val today = data.summary.today

            val delta = if (lastToday >= 0) today - lastToday else 0
            val deltaStr = when {
                delta > 0 -> " (+$delta)"
                delta < 0 -> " ($delta)"
                else -> ""
            }
            val title = "picawish today: $today visits$deltaStr"
            val detail = buildString {
                append("7d ${data.summary.last7d} · all-time ${data.summary.allTime}")
                data.wishes?.let { append(" · $it cards") }
            }
            StatsNotifier.show(applicationContext, title, detail)

            prefs.edit()
                .putInt(KEY_LAST_TODAY, today)
                .putLong(KEY_LAST_TS, System.currentTimeMillis())
                .apply()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val NAME = "picastats_hourly_stats"
        private const val PREFS = "picastats"
        private const val KEY_LAST_TODAY = "last_today"
        private const val KEY_LAST_TS = "last_ts"
    }
}
