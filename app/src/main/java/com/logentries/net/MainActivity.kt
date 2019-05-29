package com.logentries.net

import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import timber.log.Timber
import java.util.*

class MainActivity : AppCompatActivity() {
    
    companion object {
        
        private val SP_ID = "LOGENTRIES_DEMO_APP"
        private const val KEY_COUNT_DEBUG = "key_count_debug"
        private const val KEY_COUNT_WARN = "key_count_warn"
        private const val KEY_COUNT_ERROR = "key_count_error"
        
        private val bucket = Calendar.getInstance(Locale.ENGLISH).let {
            it.timeInMillis = Date().time
            var date = DateFormat.format("dd-MM-yyyy hh:mm:ss", it).toString()
            if (date.contains("1970")) {
                it.timeInMillis = Date().time * 1000L
                date = DateFormat.format("dd-MM-yyyy hh:mm:ss", it).toString()
            }
            date
        }
        
    }
    
    private var countDebug = 0
    private var countWarn = 0
    private var countError = 0
    
    private val prefs by lazy { getSharedPreferences(SP_ID, MODE_PRIVATE) }
    
    private lateinit var buttonDebug: AppCompatButton
    private lateinit var buttonWarn: AppCompatButton
    private lateinit var buttonError: AppCompatButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        Timber.d("Logging demo started")
        
        buttonDebug = findViewById(R.id.buttonDebug)
        buttonWarn = findViewById(R.id.buttonWarn)
        buttonError = findViewById(R.id.buttonError)
    
        countDebug = prefs.getInt(KEY_COUNT_DEBUG, 0)
        countWarn = prefs.getInt(KEY_COUNT_WARN, 0)
        countError = prefs.getInt(KEY_COUNT_ERROR, 0)
        
        buttonDebug.setOnClickListener { logDebug() }
        buttonWarn.setOnClickListener { logWarn() }
        buttonError.setOnClickListener { logError() }
        
    }
    
    private fun toast(m: String) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    }
    
    private fun saveCount(key: String, value: Int) {
        prefs.edit().apply {
            putInt(key, value)
            apply()
        }
    }
    
    private fun logDebug() {
        Timber.d("$bucket Debug Message")
        saveCount(KEY_COUNT_DEBUG, ++countDebug)
        buttonDebug.text = getString(R.string.button_debug_label, countDebug)
        toast("Logged debug message")
    }
    
    private fun logWarn() {
        Timber.w("$bucket Warning Message")
        saveCount(KEY_COUNT_WARN, ++countWarn)
        buttonWarn.text = getString(R.string.button_warn_label, countWarn)
        toast("Logged warn message")
    }
    
    private fun logError() {
        val sampleError = RuntimeException("I'm an error")
        Timber.e(sampleError, "$bucket Error Message")
        saveCount(KEY_COUNT_ERROR, ++countError)
        buttonError.text = getString(R.string.button_error_label, countError)
        toast("Logged error message")
    }
    
}
