package com.logentries.net

import java.io.IOException
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.Charset
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class LogentriesClient
@Throws(InstantiationException::class, IllegalArgumentException::class)
constructor(
    useHttpPost: Boolean,
    useSsl: Boolean,
    isUsingDataHub: Boolean,
    server: String?,
    port: Int,
    // Token, that points to the exact endpoint - the log object, where the data goes. 
    private val endpointToken: String?) {
    
    companion object {
        
        // Logentries server endpoints for logs data.
        // For token-based stream input
        private const val LE_TOKEN_API = "data.logentries.com"
        
        // For HTTP-based input.
        private const val LE_HTTP_API = "http://webhook.logentries.com/noformat/logs/"
        
        // For HTTP-based input.
        private const val LE_HTTPS_API = "https://webhook.logentries.com/noformat/logs/"
        
        // Port number for unencrypted HTTP PUT/Token TCP logging on Logentries server.
        private const val LE_PORT = 80
        
        // Port number for SSL HTTP PUT/TLS Token TCP logging on Logentries server.
        private const val LE_SSL_PORT = 443
        
        internal val UTF8 = Charset.forName("UTF-8")
        
    }
    
    private val sslFactory: SSLSocketFactory?
    
    // The socket, connected to the Token API endpoint (Token-based input only!)
    private var socket: Socket? = null
    
    // Data stream to the endpoint, where log messages go (Token-based input only!)
    private var stream: OutputStream? = null
    
    // Use SSL layering for the Socket?
    private var sslChoice = false
    
    // Use HTTP input instead of token-based stream input?
    private var httpChoice = false
    
    // Datahub-related attributes.
    private var dataHubServer: String? = null
    
    private var dataHubPort = 0
    private var useDataHub = false
    
    // The formatter used to prepend logs with the endpoint token for Token-based input.
    private val streamFormatter = StringBuilder()
    
    private val port: Int get() = when {
        useDataHub -> dataHubPort
        sslChoice -> LE_SSL_PORT
        else -> LE_PORT
    }
    
    private val address: String? get() =
        if (useDataHub)
            dataHubServer
        else
            if (httpChoice)
                if (sslChoice) LE_HTTPS_API else LE_HTTP_API
            else LE_TOKEN_API
    
    init {
        
        if (useHttpPost && isUsingDataHub)
            throw IllegalArgumentException("'httpPost' parameter cannot be set to true if 'isUsingDataHub' " + "is set to true.")
        
        if (endpointToken == null || endpointToken.isEmpty())
            throw IllegalArgumentException("Token parameter cannot be empty!")
        
        useDataHub = isUsingDataHub
        sslChoice = useSsl
        httpChoice = useHttpPost
        
        if (useDataHub) {
            
            if (server == null || server.isEmpty())
                throw InstantiationException("'server' parameter is mandatory if 'isUsingDatahub' parameter " + "is set to true.")
            
            if (port <= 0 || port > 65535)
                throw InstantiationException("Incorrect port number " + Integer.toString(port) + ". Port number must " +
                    "be greater than zero and less than 65535.")
            
            dataHubServer = server
            dataHubPort = port
            
        }
        
        if (!useSsl) sslFactory = null else try {
            sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
        } catch (e: Exception) {
            throw InstantiationException("Cannot create LogentriesClient instance. Error: " + e.message)
        }
        
    }
    
    @Throws(IOException::class, IllegalArgumentException::class)
    fun connect() {
        
        if (httpChoice) return
        
        if (sslChoice) {
            
            if (sslFactory == null)
                throw IllegalArgumentException("SSL Socket Factory is not initialized!")
            
            val sslSocket = sslFactory.createSocket(Socket(address, port), address, port, true) as SSLSocket
            sslSocket.tcpNoDelay = true
            
            socket = sslSocket
            
        } else {
            
            socket = Socket(address, port)
            
        }
        
        stream = socket!!.getOutputStream()
        
    }
    
    @Throws(IOException::class)
    fun write(data: String) {
        
        if (!httpChoice) {
            
            // Token-based or DataHub output mode - we're using plain stream forwarding via the socket.
            if (stream == null)
                throw IOException("OutputStream is not initialized!")
            
            streamFormatter.setLength(0) // Erase all previous data.
            streamFormatter.append(endpointToken).append(" ")
            streamFormatter.append(data)
            
            // For Token-based input it is mandatory for the message to has '\n' at the end to be
            // ingested by the endpoint correctly.
            if (!data.endsWith("\n"))
                streamFormatter.append("\n")
            
            stream!!.write(streamFormatter.toString().toByteArray(UTF8))
            stream!!.flush()
            
        } else {
            
            val url = address!! + endpointToken
            LEHttpClient.postData(url, data)
            
        }
        
    }
    
    fun close() {
        
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {}
        
    }
    
}
