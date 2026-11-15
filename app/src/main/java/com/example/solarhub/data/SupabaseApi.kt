package com.example.solarhub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Very small Supabase client using plain HTTPS (no extra library needed).
 * The "publishable" key is meant to be inside apps. Admin actions are protected
 * on the server: every admin function checks the admin password again.
 */
object SupabaseApi {

  const val BASE_URL = "https://mfovpqkhhtfpywvsslyu.supabase.co"
  private const val PUBLISHABLE_KEY = "sb_publishable_6BT5cZKjynh-Byw9t16kqw_q3JJmEKH"
  private const val BUCKET = "attachments"

  private fun open(url: String, method: String): HttpURLConnection {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = method
    connection.connectTimeout = 15000
    connection.readTimeout = 60000
    connection.setRequestProperty("apikey", PUBLISHABLE_KEY)
    return connection
  }

  private fun readBody(connection: HttpURLConnection): String? {
    val code = connection.responseCode
    return if (code in 200..299) {
      connection.inputStream.bufferedReader().use { it.readText() }
    } else {
      null
    }
  }

  /** Calls a database function. Returns the response text, or null when it failed. */
  suspend fun rpc(function: String, body: JSONObject): String? = withContext(Dispatchers.IO) {
    try {
      val connection = open("$BASE_URL/rest/v1/rpc/$function", "POST")
      connection.doOutput = true
      connection.setRequestProperty("Content-Type", "application/json")
      connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
      val result = readBody(connection)
      connection.disconnect()
      result
    } catch (e: Exception) {
      null
    }
  }

  /** Reads rows from a table, e.g. get("content_items?select=*"). */
  suspend fun get(path: String): String? = withContext(Dispatchers.IO) {
    try {
      val connection = open("$BASE_URL/rest/v1/$path", "GET")
      val result = readBody(connection)
      connection.disconnect()
      result
    } catch (e: Exception) {
      null
    }
  }

  /** Uploads a file to online storage. Returns its public web address, or null when it failed. */
  suspend fun upload(objectPath: String, file: File, mime: String?): String? =
    withContext(Dispatchers.IO) {
      try {
        val connection = open("$BASE_URL/storage/v1/object/$BUCKET/$objectPath", "POST")
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", mime ?: "application/octet-stream")
        connection.setRequestProperty("x-upsert", "true")
        connection.setFixedLengthStreamingMode(file.length())
        file.inputStream().use { input ->
          connection.outputStream.use { output -> input.copyTo(output) }
        }
        val code = connection.responseCode
        connection.disconnect()
        if (code in 200..299) "$BASE_URL/storage/v1/object/public/$BUCKET/$objectPath" else null
      } catch (e: Exception) {
        null
      }
    }

  /** Downloads a web address into a file. Returns true when finished. */
  suspend fun download(url: String, destination: File): Boolean = withContext(Dispatchers.IO) {
    try {
      destination.parentFile?.mkdirs()
      val temp = File(destination.parentFile, destination.name + ".part")
      val connection = open(url, "GET")
      if (connection.responseCode !in 200..299) {
        connection.disconnect()
        return@withContext false
      }
      connection.inputStream.use { input ->
        temp.outputStream().use { output -> input.copyTo(output) }
      }
      connection.disconnect()
      if (destination.exists()) destination.delete()
      temp.renameTo(destination)
    } catch (e: Exception) {
      false
    }
  }
}
