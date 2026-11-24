@file:OptIn(UnstableApi::class)

package com.tvlive.app.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.OkHttpClient

/**
 * Fabrique d'ExoPlayer : lecture des flux clairs (HLS via le module HLS),
 * avec le cache de téléchargements en amont pour relire les replays hors ligne.
 */
object PlayerFactory {

    fun create(context: Context): ExoPlayer {
        val okHttpFactory = OkHttpDataSource.Factory(OkHttpClient())
        val upstream: DataSource.Factory = TvDownloads.cacheOrNull()?.let { cache ->
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(okHttpFactory)
        } ?: okHttpFactory
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(upstream))
            .build()
    }
}
