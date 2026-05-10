package com.batb4016.jarpick

import android.app.Application
import com.google.android.gms.ads.MobileAds

class JarPickApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}
