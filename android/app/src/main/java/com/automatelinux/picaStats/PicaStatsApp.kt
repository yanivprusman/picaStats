package com.automatelinux.picaStats

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.automatelinux.picaStats.work.StatsNotifier
import com.automatelinux.picaStats.work.StatsWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class PicaStatsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        StatsNotifier.ensureChannel(this)
        scheduleHourlyStats()
    }

    private fun scheduleHourlyStats() {
        val request = PeriodicWorkRequestBuilder<StatsWorker>(1, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            StatsWorker.NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
