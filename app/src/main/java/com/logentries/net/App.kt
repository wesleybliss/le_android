package com.logentries.net

import android.app.Application
import timber.log.Timber

class App : Application() {
    
    companion object {
        
        lateinit var instance: App
        
    }
    
    override fun onCreate() {
        
        super.onCreate()
        
        instance = this
        
        Timber.plant(Timber.DebugTree())
        
        Timber.plant(
            LogentriesTimberLogger(
                instance.applicationContext,
                GsonInstance.gson
            )
        )
        
        Timber.d("Using Logentries token ${BuildConfig.LOGENTRIES_TOKEN}")
        
    }
    
}
