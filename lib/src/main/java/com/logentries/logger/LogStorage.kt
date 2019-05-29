package com.logentries.logger

import android.util.Log
import com.logentries.storage.LEDataSource
import java.io.IOException
import java.util.*

class LogStorage(private val dataSource: LEDataSource) {
    
    @Throws(IOException::class, RuntimeException::class)
    fun putLogToStorage(message: String) {
        dataSource.writeLog(message)
    }
    
    fun getAllLogsFromStorage(needToRemoveStorageFile: Boolean) : Queue<String> =
        dataSource.readAllLogs().also {
            if (needToRemoveStorageFile)
                try { removeStorageFile() }
                catch (e: Exception) { e.printStackTrace() }
        }
    
    @Throws(IOException::class)
    fun removeStorageFile() {
        dataSource.destroy()
    }
    
    @Throws(IOException::class)
    fun reCreateStorageFile() {
        Log.d(this::class.java.simpleName, "Log storage has been re-created.")
        dataSource.recreate()
    }
    
}
