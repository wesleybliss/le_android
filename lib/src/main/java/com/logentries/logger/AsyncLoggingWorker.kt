package com.logentries.logger

import android.content.Context
import android.util.Log
import com.logentries.misc.IDUtils
import com.logentries.misc.Utils
import com.logentries.storage.LEDataSource
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue

class AsyncLoggingWorker @Throws(IOException::class)
constructor(
    useSsl: Boolean,
    useHttpPost: Boolean,
    useDataHub: Boolean,
    logToken: String,
    dataHubAddress: String?,
    dataHubPort: Int,
    logHostName: Boolean,
    dataSource: LEDataSource) {
    
    companion object {
        
        private const val TAG = "LogentriesAndroidLogger"
        
        /**
         * Size of the internal event queue.
         */
        private const val QUEUE_SIZE = 32768
        /**
         * Limit on individual log length ie. 2^16
         */
        private const val LOG_LENGTH_LIMIT = 65536
        
        /**
         * Error message displayed when invalid API key is detected.
         */
        private const val INVALID_TOKEN = "Given Token does not look right!"
        
        /**
         * Error message displayed when queue overflow occurs
         */
        private const val QUEUE_OVERFLOW = "Logentries Buffer Queue Overflow. Message Dropped!"
        
        private fun checkTokenFormat(token: String) : Boolean =
            IDUtils.checkValidUUID(token)
        
    }
    
    /**
     * Indicator if the socket appender has been started.
     */
    private var started: Boolean = false
    
    /**
     * Whether should send logs with or without meta data
     */
    var sendRawLogMessage = false
    
    /**
     * Asynchronous socket appender.
     */
    private val appender: SocketAppender by lazy {
        SocketAppender(
            localStorage,
            queue,
            useHttpPost,
            useSsl,
            useDataHub,
            dataHubAddress,
            dataHubPort,
            logToken,
            logHostName,
            sendRawLogMessage)
    }
    
    /**
     * Message queue.
     */
    private val queue: ArrayBlockingQueue<String>
    
    /**
     * Logs queue storage
     */
    private val localStorage: LogStorage by lazy { LogStorage(dataSource) }
    
    init {
        
        if (!checkTokenFormat(logToken))
            throw IllegalArgumentException(INVALID_TOKEN)
        
        queue = ArrayBlockingQueue(QUEUE_SIZE)
        appender.start()
        started = true
        
    }
    
    fun addLineToQueue(line: String) {
        
        // Check that we have all parameters set and socket appender running.
        if (!started) {
            appender.start()
            started = true
        }
        
        if (line.length > LOG_LENGTH_LIMIT)
            Utils.splitStringToChunks(line, LOG_LENGTH_LIMIT)
                .forEach { tryOfferToQueue(it) }
        else
            tryOfferToQueue(line)
        
    }
    
    /**
     * Stops the socket appender. queueFlushTimeout (if greater than 0) sets the maximum timeout in milliseconds for
     * the message queue to be flushed by the socket appender, before it is stopped. If queueFlushTimeout
     * is equal to zero - the method will wait until the queue is empty (which may be dangerous if the
     * queue is constantly populated by another thread mantime.
     *
     * @param queueFlushTimeout - max. wait time in milliseconds for the message queue to be flushed.
     */
    @JvmOverloads
    fun close(queueFlushTimeout: Long = 0) {
        
        if (queueFlushTimeout < 0)
            throw IllegalArgumentException("queueFlushTimeout must be greater or equal to zero")
        
        val now = System.currentTimeMillis()
        
        while (!queue.isEmpty()) {
            if (queueFlushTimeout != 0L &&
                System.currentTimeMillis() - now >= queueFlushTimeout)
                // The timeout expired - need to stop the appender.
                break
        }
        
        appender.interrupt()
        started = false
        
    }
    
    @Throws(RuntimeException::class)
    private fun tryOfferToQueue(line: String) {
        
        if (!queue.offer(line)) {
            
            Log.e(TAG, "The queue is full - will try to drop the oldest message in it.")
            queue.poll()
            
            /*
            FIXME: This code migrated from LE Java Library; currently, there is no a simple
            way to backup the queue in case of overflow due to requirements to max.
            memory consumption and max. possible size of the local logs storage. If use
            the local storage - the we have three problems: 1) Correct joining of logs from
            the queue and from the local storage (and we need some right event to trigger this joining);
            2) Correct order of logs after joining; 3) Data consistence problem, because we're
            accessing the storage from different threads, so sync. logic will increase overall
            complexity of the code. So, for now this logic is left AS IS, due to relatively
            rareness of the case with queue overflow.
             */
            
            if (!queue.offer(line))
                throw RuntimeException(QUEUE_OVERFLOW)
            
        }
        
    }
    
}
