@file:OptIn(UnstableApi::class)

package com.tvlive.app

import android.app.Application
import androidx.media3.common.util.UnstableApi
import com.tvlive.app.player.TvDownloads

class TvLiveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TvDownloads.init(this)
    }
}
