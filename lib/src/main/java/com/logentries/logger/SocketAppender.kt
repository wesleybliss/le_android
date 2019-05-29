package com.logentries.logger

import android.content.ContentValues.TAG
import android.util.Log
import com.logentries.misc.FormattingUtils
import com.logentries.net.LogentriesClient
import java.io.IOException
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class SocketAppender(
    private val localStorage: LogStorage,
    private val queue: ArrayBlockingQueue<String>,
    private val useHttpPost: Boolean,
    private val useSsl: Boolean,
    private val isUsingDataHub: Boolean,
    private val dataHubAddr: String? = null,
    private val dataHubPort: Int,
    private val token: String,
    private val logHostName: Boolean = true,
    private val sendRawLogMessage: Boolean = false)
    : Thread("Logentries Socket appender") {
    
    companion object {
    
        // Formatting constants
        private const val LINE_SEP_REPLACER = "\u2028"
        
        private const val RECONNECT_WAIT = 100 // milliseconds.
        private const val MAX_QUEUE_POLL_TIME = 1000 // milliseconds.
    
        private const val MAX_NETWORK_FAILURES_ALLOWED = 3
        private const val MAX_RECONNECT_ATTEMPTS = 3
        
    }
    
    private var leClient: LogentriesClient? = null
    
    init {
        // Don't block shut down
        isDaemon = true
    }
    
    @Throws(IOException::class, InstantiationException::class)
    private fun openConnection() {
        if (leClient == null)
            leClient = LogentriesClient(useHttpPost, useSsl, isUsingDataHub, dataHubAddr, dataHubPort, token)
        leClient!!.connect()
    }
    
    @Throws(InterruptedException::class, InstantiationException::class)
    private fun reopenConnection(maxReConnectAttempts: Int): Boolean {
        
        if (maxReConnectAttempts < 0)
            throw IllegalArgumentException("maxReConnectAttempts value must be greater or equal to zero")
        
        // Close the previous connection
        closeConnection()
        
        for (attempt in 0 until maxReConnectAttempts) {
            try {
                openConnection()
                return true
            } catch (e: IOException) {
                // Ignore the exception and go for the next
                // iteration.
            }
            sleep(RECONNECT_WAIT.toLong())
        }
        
        return false
        
    }
    
    private fun closeConnection() {
        leClient?.close()
    }
    
    private fun tryUploadSavedLogs(): Boolean {
        
        var logs: Queue<String> = ArrayDeque()
        
        try {
            
            logs = localStorage.getAllLogsFromStorage(false)
            
            var msg: String? = logs.peek()
            
            while (msg != null) {
                
                if (sendRawLogMessage)
                    leClient!!.write(
                        FormattingUtils.formatMessage(
                            msg.replace("\n", LINE_SEP_REPLACER),
                            logHostName,
                            useHttpPost)
                    )
                else
                    leClient!!.write(msg.replace("\n", LINE_SEP_REPLACER))
                
                logs.poll() // Remove the message after successful sending.
                msg = logs.peek()
                
            }
            
            // All logs have been uploaded - remove the storage file and create the blank one.
            try {
                localStorage.reCreateStorageFile()
            } catch (ex: IOException) {
                Log.e(TAG, ex.message)
            }
            
            return true
            
        } catch (ioEx: IOException) {
            
            Log.e(TAG, "Cannot upload logs to the server. Error: " + ioEx.message)
            
            // Try to save back all messages, that haven't been sent yet.
            try {
                localStorage.reCreateStorageFile()
                logs.forEach { msg -> localStorage.putLogToStorage(msg) }
            } catch (ioEx2: IOException) {
                Log.e(TAG, "Cannot save logs to the local storage - part of messages will be " +
                    "dropped! Error: " + ioEx2.message)
            }
            
        }
        
        return false
        
    }
    
    override fun run() {
        
        try {
            
            // Open connection
            reopenConnection(MAX_RECONNECT_ATTEMPTS)
            
            val prevSavedLogs = localStorage.getAllLogsFromStorage(true)
            var numFailures = 0
            var connectionIsBroken = false
            var message: String?
            
            // Send data in queue
            while (true) {
                
                // First we need to send the logs from the local storage -
                // they haven't been sent during the last session, so need to
                // come first.
                message = if (prevSavedLogs.isEmpty()) {
                    // Try to take data from the queue if there are no logs from
                    // the local storage left to send.
                    queue.poll(MAX_QUEUE_POLL_TIME.toLong(), TimeUnit.MILLISECONDS)
                } else {
                    // Getting messages from the previous session one by one.
                    prevSavedLogs.poll()
                }
                
                // Send data, reconnect if needed.
                while (true) {
                    
                    try {
                        
                        // If we have broken connection, then try to re-connect and send
                        // all logs from the local storage. If succeeded - reset numFailures.
                        if (connectionIsBroken && reopenConnection(MAX_RECONNECT_ATTEMPTS)) {
                            if (tryUploadSavedLogs()) {
                                connectionIsBroken = false
                                numFailures = 0
                            }
                        }
                        
                        if (message != null) {
                            leClient!!.write(
                                FormattingUtils.formatMessage(
                                    message.replace("\n", LINE_SEP_REPLACER),
                                    logHostName,
                                    useHttpPost)
                            )
                            message = null
                        }
                        
                    } catch (e: IOException) {
                        
                        if (numFailures >= MAX_NETWORK_FAILURES_ALLOWED) {
                            connectionIsBroken = true // Have tried to reconnect for MAX_NETWORK_FAILURES_ALLOWED
                            // times and failed, so assume, that we have no link to the
                            // server at all...
                            try {
                                // ... and put the current message to the local storage.
                                localStorage.putLogToStorage(message)
                                message = null
                            } catch (ex: IOException) {
                                Log.e(TAG, "Cannot save the log message to the local storage! Error: " + ex.message)
                            }
                        } else {
                            ++numFailures
                            // Try to re-open the lost connection.
                            reopenConnection(MAX_RECONNECT_ATTEMPTS)
                        }
                        
                        continue
                        
                    }
                    
                    break
                    
                }
                
            }
            
        } catch (e: InterruptedException) {
            // We got interrupted, stop.
        } catch (e: InstantiationException) {
            
            Log.e(TAG, "Cannot instantiate LogentriesClient due to improper configuration. Error: " + e.message)
            
            // Save all existing logs to the local storage.
            // There is nothing we can do else in this case.
            var message: String? = queue.poll()
            
            try {
                while (message != null) {
                    localStorage.putLogToStorage(message)
                    message = queue.poll()
                }
            } catch (ex: IOException) {
                Log.e(TAG, "Cannot save logs queue to the local storage - all log messages will be dropped! Error: " + e.message)
            }
            
        }
        
        closeConnection()
        
    }
    
}
