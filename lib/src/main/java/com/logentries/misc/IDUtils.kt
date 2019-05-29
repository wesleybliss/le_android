package com.logentries.misc

import android.content.ContentValues.TAG
import android.util.Log
import com.logentries.logger.AndroidLogger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.experimental.and

object IDUtils {
    
    @Throws(NoSuchAlgorithmException::class)
    fun computeTraceID(): String {
        
        val fingerprint = OSUtils.getProp("ro.build.fingerprint")
        val displayId = OSUtils.getProp("ro.build.display.id")
        val hardware = OSUtils.getProp("ro.hardware")
        val device = OSUtils.getProp("ro.product.device")
        val rilImei = OSUtils.getProp("ril.IMEI")
        
        val hashGen = MessageDigest.getInstance("MD5")
        
        if (fingerprint.isEmpty()
            and displayId.isEmpty()
            and hardware.isEmpty()
            and device.isEmpty()
            and rilImei.isEmpty()) {
            
            Log.e(TAG, "Cannot obtain any of device's properties - will use default Trace ID source.")
            
            var randomTrace: Double? = Math.random() + Math.PI
            var defaultValue = randomTrace!!.toString()
            
            randomTrace = Math.random() + Math.PI
            defaultValue += randomTrace.toString().replace(".", "")
            
            // The code below fixes one strange bug, when call to a freshly installed app crashes at this
            // point, because random() produces too short sequence. Note, that this behavior does not
            // occur for the second and all further launches.
            defaultValue =
                if (defaultValue.length >= 36)
                    defaultValue.substring(2, 34)
                else
                    defaultValue.substring(2)
            
            hashGen.update(defaultValue.toByteArray())
            
        } else {
            
            val sb = StringBuilder()
            sb.append(fingerprint).append(displayId).append(hardware).append(device).append(rilImei)
            hashGen.update(sb.toString().toByteArray())
            
        }
        
        val conv = StringBuilder()
        
        hashGen.digest().forEach {
            conv.append(String.format("%02x", it and 0xff.toByte()).toUpperCase())
        }
        
        return conv.toString()
        
    }
    
    fun getFormattedDeviceId(toJSON: Boolean) : String =
        if (toJSON) "\"DeviceId\": \"" + AndroidLogger.uniqueId + "\""
        else "DeviceId=" + AndroidLogger.uniqueId
    
    fun checkValidUUID(uuid: String?): Boolean {
        if (uuid != null && uuid.isNotEmpty()) {
            return try {
                val u = UUID.fromString(uuid)
                true
    
            } catch (e: IllegalArgumentException) {
                false
            }
        }
        return false
    }
    
}

