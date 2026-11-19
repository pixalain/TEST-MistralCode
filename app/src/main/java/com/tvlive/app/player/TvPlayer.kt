package com.tvlive.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.OkHttpClient

/** Fabrique d'ExoPlayer : lecture des flux clairs, avec cache pour les replays téléchargés. */
@OptIn(UnstableApi::class)
object TvPlayer {

    fun create(context: Context): ExoPlayer {
        val okFactory = OkHttpDataSource.Factory(OkHttpClient())
        val cache = TvDownloads.cacheOrNull()
        val upstream: DataSource.Factory = if (cache != null) {
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(okFactory)
        } else {
            okFactory
        }
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(upstream))
            .build()
    }
}
