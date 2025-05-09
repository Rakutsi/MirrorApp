package com.example.mirrorapp

// ICSFetcher.kt
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ICSFetcher {

    fun fetchICSFile(icsUrl: String): String {
        val result = StringBuilder()
        try {
            val url = URL(icsUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            // Läs in filinnehållet från URL:en
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                result.append(line).append("\n")
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result.toString()  // Returnera innehållet som en sträng
    }
}

