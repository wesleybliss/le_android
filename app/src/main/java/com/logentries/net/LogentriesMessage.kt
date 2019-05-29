package com.logentries.net

data class LogentriesMessage(

    var priority: String? = null,
    var tag: String? = null,
    var userId: Long? = null,
    var userEmail: String? = null,
    var eventId: Long? = null,
    var message: String? = null,
    var error: String? = null

)
