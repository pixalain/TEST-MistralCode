package com.tvlive.app.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.tvlive.app.R
import com.tvlive.app.data.model.Channel
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.Executors

/**
 * Téléchargements de replays HLS (flux clairs uniquement) via le DownloadManager
 * de Media3 : les segments sont stockés dans le cache de l'app et relisibles hors ligne.
 * Aucun DRM n'est traité : un flux chiffré échoue simplement.
 */
@OptIn(UnstableApi::class)
object TvDownloads {

    const val CHANNEL_ID = "tvlive_downloads"

    @Volatile
    private var managerRef: DownloadManager? = null

    @Volatile
    private var cacheRef: SimpleCache? = null

    val isInitialized: Boolean get() = managerRef != null
    val manager: DownloadManager get() = managerRef
        ?: throw IllegalStateException("TvDownloads.init() non appelé")

    fun cacheOrNull(): Cache? = cacheRef

    @Synchronized
    fun init(context: Context) {
        if (managerRef != null) return
        val app = context.applicationContext
        val db = StandaloneDatabaseProvider(app)
        val cache = SimpleCache(
            File(app.cacheDir, "tvlive_downloads"),
            LeastRecentlyUsedCacheEvictor(512L * 1024 * 1024),
            db,
        )
        val factory = OkHttpDataSource.Factory(OkHttpClient())
        managerRef = DownloadManager(
            app,
            db,
            cache,
            factory,
            Executors.newFixedThreadPool(2),
        )
        cacheRef = cache
        createNotificationChannel(app)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.download_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }
    }

    fun enqueue(context: Context, channel: Channel) {
        val request = DownloadRequest.Builder(channel.url, Uri.parse(channel.url))
            .setCustomCacheKey(channel.url)
            .setData(channel.name.toByteArray())
            .apply {
                if (channel.url.lowercase().contains(".m3u8")) {
                    setMimeType("application/x-mpegURL")
                }
            }
            .build()
        DownloadService.sendAddDownload(
            context.applicationContext,
            TvDownloadService::class.java,
            request,
            false,
        )
    }

    fun remove(context: Context, id: String) {
        DownloadService.sendRemoveDownload(
            context.applicationContext,
            TvDownloadService::class.java,
            id,
            false,
        )
    }

    fun currentDownloads(): List<Download> = manager.currentDownloads
}

@OptIn(UnstableApi::class)
class TvDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    TvDownloads.CHANNEL_ID,
    R.string.download_channel_name,
    R.string.download_channel_description,
) {
    override fun getDownloadManager(): DownloadManager = TvDownloads.manager

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int,
    ): Notification {
        val helper = DownloadNotificationHelper(this, TvDownloads.CHANNEL_ID)
        return helper.buildProgressNotification(
            this,
            android.R.drawable.stat_sys_download,
            null,
            null,
            downloads,
            notMetRequirements,
        )
    }

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1001
        const val FOREGROUND_NOTIFICATION_UPDATE_INTERVAL = 1000L
    }
}
