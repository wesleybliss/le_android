package com.logentries.logger

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.logentries.storage.LEDataSource
import com.logentries.storage.LEFileDataSource
import java.io.IOException
import java.util.*

class AndroidLogger @Throws(IOException::class)
private constructor(
    context: Context,
    useHttpPost: Boolean,
    useSsl: Boolean,
    isUsingDataHub: Boolean,
    dataHubAddr: String? = null,
    dataHubPort: Int,
    token: String,
    logHostName: Boolean,
    dataSource: LEDataSource) {
    
    companion object {
    
        var uniqueId: String? = ""
        private var instance: AndroidLogger? = null
        
        private const val SP_ID = "LogEntriesAndroidLogger"
        private const val KEY_UNIQUE_ID = "key_unique_id"
        
        @Synchronized
        @Throws(IOException::class)
        fun createInstance(
            context: Context,
            useHttpPost: Boolean,
            useSsl: Boolean,
            isUsingDataHub: Boolean,
            dataHubAddr: String? = null,
            dataHubPort: Int,
            token: String,
            logHostName: Boolean,
            dataSource: LEDataSource? = null) : AndroidLogger {
            
            instance?.loggingWorker?.close()
            
            instance = AndroidLogger(
                context,
                useHttpPost,
                useSsl,
                isUsingDataHub,
                dataHubAddr,
                dataHubPort,
                token,
                logHostName,
                dataSource ?: LEFileDataSource(context))
            
            return instance!!
            
        }
    
        @Synchronized
        @Suppress("unused")
        fun getInstance() : AndroidLogger = instance
            ?: throw IllegalArgumentException("Logger instance is not initialized. Call createInstance() first!")        
        
    }
    
    private val loggingWorker: AsyncLoggingWorker
    
    @Suppress("unused")
    var sendRawLogMessage: Boolean
        /**
         * Returns whether the logger is configured to send raw log messages or not.
         * @return
         */
        get() = loggingWorker.sendRawLogMessage
        /**
         * Set whether you wish to send your log message without additional meta data to Logentries.
         * @param sendRawLogMessage Set to true if you wish to send raw log messages
         */
        set(sendRawLogMessage) {
            loggingWorker.sendRawLogMessage = sendRawLogMessage
        }
    
    init {
        
        val prefs = context.getSharedPreferences(SP_ID, MODE_PRIVATE)
        
        uniqueId = prefs.getString(KEY_UNIQUE_ID, "")
        
        if (uniqueId!!.isEmpty()) {
            // Create a new id
            uniqueId = UUID.randomUUID().toString()
            val editor = prefs.edit()
            editor.putString(KEY_UNIQUE_ID, uniqueId)
            editor.apply()
        }
        
        loggingWorker = AsyncLoggingWorker(
            useSsl,
            useHttpPost,
            isUsingDataHub,
            token,
            dataHubAddr,
            dataHubPort,
            logHostName,
            dataSource
        )
        
    }
    
    fun log(message: String) {
        loggingWorker.addLineToQueue(message)
    }
    
}
