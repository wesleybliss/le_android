package com.logentries.misc

object FormattingUtils {
    
    /**
     * Formats given message to make it suitable for ingestion by Logentris endpoint.
     * If isUsingHttp == true, the method produces such structure:
     * {"event": {"Host": "SOMEHOST", "Timestamp": 12345, "DeviceID": "DEV_ID", "Message": "MESSAGE"}}
     * <p>
     * If isUsingHttp == false the output will be like this:
     * Host=SOMEHOST Timestamp=12345 DeviceID=DEV_ID MESSAGE
     *
     * @param message     Message to be sent to Logentries
     * @param logHostName - if set to true - "Host"=HOSTNAME parameter is appended to the message.
     * @param isUsingHttp will be using http
     * @return
     */
    fun formatMessage(message: String, logHostName: Boolean, isUsingHttp: Boolean): String {
        
        val sb = StringBuilder()
        
        if (isUsingHttp)
            // Add 'event' structure.
            sb.append("{\"event\": {")
        
        if (logHostName) {
            sb.append(Utils.getFormattedHostName(isUsingHttp))
            sb.append(if (isUsingHttp) ", " else " ")
        }
        
        sb.append(Utils.getFormattedTraceID(isUsingHttp)).append(" ")
        sb.append(if (isUsingHttp) ", " else " ")
        
        sb.append(IDUtils.getFormattedDeviceId(isUsingHttp)).append(" ")
        sb.append(if (isUsingHttp) ", " else " ")
        
        val timestamp = System.currentTimeMillis() // Current time in UTC in milliseconds.
        
        if (isUsingHttp)
            sb.append("\"Timestamp\": ").append(timestamp).append(", ")
        else
            sb.append("Timestamp=").append(timestamp).append(" ")
        
        // Append the event data
        if (isUsingHttp) {
            if (Utils.isJSONValid(message)) {
                sb.append("\"Message\":").append(message)
                sb.append("}}")
            } else {
                sb.append("\"Message\": \"").append(message)
                sb.append("\"}}")
            }
        } else {
            sb.append(message)
        }
        
        return sb.toString()
        
    }
    
}
