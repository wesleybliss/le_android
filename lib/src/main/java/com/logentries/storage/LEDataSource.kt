package com.logentries.storage

import java.util.*

interface LEDataSource {
    
    //region Lifecycle
    
    fun create()
    
    fun destroy()
    
    fun recreate() {}
    
    //endregion Lifecycle
    
    //region Storage
    
    fun writeLog(message: String)
    
    fun readAllLogs() : Queue<String>
    
    //endregion Storage
    
}
