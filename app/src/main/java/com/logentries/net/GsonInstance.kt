package com.logentries.net

import com.google.gson.GsonBuilder

object GsonInstance {
    
    val gson by lazy {
        GsonBuilder()
            .serializeNulls()
            .enableComplexMapKeySerialization()
            .setLenient()
            .setPrettyPrinting()
            .create()
    }
    
}
