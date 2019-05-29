package com.logentries.storage

import android.content.Context
import android.util.Log
import java.io.*
import java.util.*

class LEFileDataSource(private val context: Context) : LEDataSource {
    
    companion object {
        
        private val TAG = LEFileDataSource::class.java.simpleName
        private const val STORAGE_FILE_NAME = "LogentriesLogStorage.log"
        private const val MAX_QUEUE_FILE_SIZE = (10 * 1024 * 1024).toLong() // 10 MBytes.
        
    }
    
    // We keep the ptr permanently, because frequently accessing
    private var storageFilePtr: File? = null
    
    @Suppress("MemberVisibilityCanBePrivate")
    val currentStorageFileSize: Long
        @Throws(IOException::class)
        get() {
            if (storageFilePtr == null) create()
            return storageFilePtr!!.length()
        }
    
    init {
        create()
    }
    
    @Throws(IOException::class)
    override fun create() {
        
        storageFilePtr = File(context.filesDir, STORAGE_FILE_NAME)
        
    }
    
    override fun destroy() {
        
        if (!storageFilePtr!!.delete())
            throw IOException("Cannot delete $STORAGE_FILE_NAME")
        
    }
    
    override fun recreate() {
        super.recreate()
        destroy()
        create()
    }
    
    override fun writeLog(message: String) {
        
        var msg = message
        
        // Fix line endings for ingesting the log to the local storage.
        if (!msg.endsWith("\n"))
            msg += "\n"
        
        var writer: FileOutputStream? = null
        
        try {
            
            val raw = msg.toByteArray()
            val currSize = currentStorageFileSize + raw.size
            val sizeStr = java.lang.Long.toString(currSize)
            
            Log.d(TAG, "Current size: $sizeStr")
            
            if (currSize >= MAX_QUEUE_FILE_SIZE) {
                Log.d(TAG, "Log storage will be cleared because threshold of $MAX_QUEUE_FILE_SIZE bytes has been reached")
                recreate()
            }
            
            Log.e("LOGENTRIES", "writing data to log $msg")
            
            writer = context.openFileOutput(STORAGE_FILE_NAME, Context.MODE_APPEND)
            writer!!.write(raw)
            
        } finally {
            
            writer?.close()
            
        }
        
    }
    
    override fun readAllLogs(): Queue<String> {
        
        val logs = ArrayDeque<String>()
        var input: FileInputStream? = null
        
        try {
            
            input = context.openFileInput(STORAGE_FILE_NAME)
        
            val inputStream = DataInputStream(input!!)
            val bufReader = BufferedReader(InputStreamReader(inputStream))
            var logLine: String? = bufReader.readLine()
        
            while (logLine != null) {
                logs.offer(logLine)
                logLine = bufReader.readLine()
            }
        
        } catch (ex: IOException) {
            
            // Basically, ignore the exception - if something has gone wrong - just return empty logs list.
            Log.e(TAG, "Cannot load logs from the local storage: " + ex.message)
        
        } finally {
        
            try { input?.close() }
            catch (ex2: IOException) {
                Log.e(TAG, "Cannot close the local storage file: " + ex2.message)
            }
        
        }
    
        return logs
        
    }
    
}
