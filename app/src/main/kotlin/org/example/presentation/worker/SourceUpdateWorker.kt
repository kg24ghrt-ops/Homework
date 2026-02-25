// presentation/worker/SourceUpdateWorker.kt
package com.openanimelib.presentation.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.openanimelib.domain.repository.SourceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SourceUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val sourceRepository: SourceRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            sourceRepository.refreshAllSources()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry()
            else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "source_update_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<SourceUpdateWorker>(
                6, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}

// Notification worker for new episodes
@HiltWorker
class NewEpisodeNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val watchlistRepository: WatchlistRepository,
    private val animeRepository: AnimeRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Check for new episodes of watched anime
            val watching = watchlistRepository.getWatchingAnime()

            watching.forEach { item ->
                val detail = animeRepository.getAnimeDetailOnce(item.anime.id)
                if (detail != null && detail.anime.status == AnimeStatus.AIRING) {
                    // Could send notification about new episode
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}