package com.github.ephemient.logcat

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogcatTest {
    @Test
    fun test() {
        val messages = Logcat(nonblocking = true).use { logcat ->
            logcat.open()
            Log.d("LogcatTest", logcat.toString())
            buildList {
                do {
                    val message = logcat.readMessage()!!
                    add(message)
                } while (message.tag != "LogcatTest")
            }
        }
        Log.i("LogcatTest", messages.joinToString("\n", "messages = [\n", "\n]"))
    }
}
