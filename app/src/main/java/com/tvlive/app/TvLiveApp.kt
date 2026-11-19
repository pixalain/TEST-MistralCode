package com.tvlive.app

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.tvlive.app.player.TvDownloads

class TvLiveApp : Application() {
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        TvDownloads.init(this)
    }
}
