package com.logentries.misc

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.InetAddress
import java.net.UnknownHostException
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.regex.Pattern

object Utils {
    
    private const val TAG = "LogentriesAndroidLogger"
    
    private var traceID = ""
    private var hostName = ""
    
    // Requires at least API level 9 (v. >= 2.3).
    init {
        
        traceID = try {
            IDUtils.computeTraceID()
        } catch (ex: NoSuchAlgorithmException) {
            Log.e(TAG, "Cannot get traceID from device's properties!")
            "unknown"
        }
        
        hostName = try {
            OSUtils.getProp("net.hostname").let {
                // If we failed to get the real host name, use the default one
                if (it.isBlank()) InetAddress.getLocalHost().hostName else it
            }
        } catch (e: UnknownHostException) {
            // We cannot resolve local host name - so won't use it at all.
            ""
        }
        
    }
    
    fun getFormattedTraceID(toJSON: Boolean) : String =
        if (toJSON) "\"TraceID\": \"$traceID\""
        else "TraceID=$traceID"
    
    fun getFormattedHostName(toJSON: Boolean) : String =
        if (toJSON) "\"Host\": \"$hostName\""
        else "Host=$hostName"
    
    fun isJSONValid(message: String): Boolean {
        try { JSONObject(message) }
        catch (ex: JSONException) {
            try { JSONArray(message) }
            catch (ex1: JSONException) { return false }
        }
        return true
    }
    
    fun splitStringToChunks(source: String, chunkLength: Int) : Array<String> {
        
        if (chunkLength < 0)
            throw IllegalArgumentException("Chunk length must be greater or equal to zero!")
        
        val srcLength = source.length
        
        if (chunkLength == 0 || srcLength <= chunkLength)
            return arrayOf(source)
        
        val chunkBuffer = ArrayList<String>()
        val splitSteps = srcLength / chunkLength + if (srcLength % chunkLength > 0) 1 else 0
        var lastCutPosition = 0
        
        repeat(splitSteps) {
            
            if (it < splitSteps - 1)
                // Cut out the chunk of the requested size.
                chunkBuffer.add(source.substring(lastCutPosition, lastCutPosition + chunkLength))
            else
                // Cut out all that left to the end of the string.
                chunkBuffer.add(source.substring(lastCutPosition))
            
            lastCutPosition += chunkLength
            
        }
        
        return chunkBuffer.toTypedArray()
        
    }
    
}
