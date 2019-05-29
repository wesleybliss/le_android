package com.logentries.net

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.logentries.logger.AndroidLogger
import timber.log.Timber

class LogentriesTimberLogger(
    private val context: Context,
    private val gson: Gson)
    : Timber.Tree() {
    
    private val logentriesLogger by lazy {
        AndroidLogger.createInstance(
            context,
            true,
            true,
            false,
            null,
            0,
            BuildConfig.LOGENTRIES_TOKEN,
            false
        )
    }
    
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        
        val priorityTag = when (priority) {
            Log.VERBOSE -> "VERBOSE"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR -> "ERROR"
            Log.ASSERT -> "ASSERT"
            else -> "UNKNOWN"
        }
        
        if (priority != Log.ERROR)
            return
        
        val payload = LogentriesMessage(
            priority = priorityTag,
            message = message
        )
        
        if (!tag.isNullOrBlank())
            payload.tag = tag
        
        if (t != null && !t.localizedMessage.isNullOrBlank())
            payload.error = t.localizedMessage
        
        val formattedMessage = gson.toJson(payload, LogentriesMessage::class.java)
        
        logentriesLogger.log(formattedMessage)
        
    }
    
}

