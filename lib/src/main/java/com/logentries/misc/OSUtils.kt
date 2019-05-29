package com.logentries.misc

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object OSUtils {
    
    fun getProp(propertyName: String) : String =
        try {
            val proc = ProcessBuilder("/system/bin/getprop", propertyName)
                .redirectErrorStream(true).start()
            val br = BufferedReader(InputStreamReader(proc!!.inputStream))
            val value = br.readText()
            proc.destroy()
            value
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            ""
        }
    
}
