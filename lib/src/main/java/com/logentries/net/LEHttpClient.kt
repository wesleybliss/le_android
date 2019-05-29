package com.logentries.net

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object LEHttpClient {
    
    fun postData(url: String, data: String) {
        
        GlobalScope.launch(Dispatchers.IO) {
            
            var request: HttpURLConnection? = null
            
            try {
                
                Log.e("LOGENTRIES", "URL is $url")
                
                val dataSample = if (data.length > 200) data.substring(0, 200) else data
                Log.e("LOGENTRIES", "data is $dataSample...")
                
                val http = URL(url)
                request = http.openConnection() as HttpURLConnection
                
                request.readTimeout = 20000
                request.connectTimeout = 20000
                request.requestMethod = "POST"
                request.doInput = true
                request.doOutput = true
                request.setChunkedStreamingMode(0)
                
                val os = request.outputStream
                val writer = BufferedWriter(OutputStreamWriter(os, LogentriesClient.UTF8))
                
                writer.write(data)
                writer.flush()
                writer.close()
                os.close()
                
                request.responseCode.also { code ->
                    
                    if (code == HttpsURLConnection.HTTP_OK) {
                        
                        val responseMessage = BufferedReader(InputStreamReader(request.inputStream))
                            .use(BufferedReader::readText)
                        
                        Log.d("LOGENTRIES", "$code HTTP SUCCESS $responseMessage")
                        
                    } else {
                        
                        val errorMessage = try {
                            BufferedReader(InputStreamReader(request.errorStream))
                                .use(BufferedReader::readText)
                        } catch (e: Exception) {
                            "(UNKNOWN ERROR)"
                        }
                        
                        Log.e("LOGENTRIES", "$code Failed to send HTTP request -- " +
                            "${request.responseMessage} -- $errorMessage")
                        
                    }
                }
                
            } catch (ex: Exception) {
                
                Log.e("LOGENTRIES", "Error message:" + ex.message, ex)
                ex.printStackTrace()
                
            } finally {
                
                request?.disconnect()
                
            }
            
        }
        
    }
    
}
