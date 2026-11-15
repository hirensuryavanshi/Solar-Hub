package com.example.solarhub.network

import com.example.solarhub.model.ContentItem
import com.example.solarhub.model.ContentType
import com.example.solarhub.model.Department
import com.example.solarhub.model.RequestStatus
import com.example.solarhub.model.UserRequest
import com.example.solarhub.model.UserRole
import com.example.solarhub.model.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class UploadedFileResult(
  val id: String,
  val originalName: String,
  val mimeType: String,
  val sizeBytes: Long,
  val url: String
)

data class SyncPayload(
  val serverTime: Long,
  val fullSync: Boolean,
  val departments: List<Department>,
  val contentItems: List<ContentItem>,
  val requests: List<UserRequest>,
  val deletedDepartments: List<String>,
  val deletedContent: List<String>,
  val deletedRequests: List<String>
)

data class ServerHealthResult(
  val ok: Boolean,
  val storageMode: String,
  val serverTime: Long
)

class ApiException(message: String, val code: Int = 0) : IOException(message)

class SolarHubApiClient(
  val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .pingInterval(20, TimeUnit.SECONDS)
    .build()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun checkHealth(baseUrl: String): Boolean = checkHealthDetailed(baseUrl).ok

  suspend fun checkHealthDetailed(baseUrl: String): ServerHealthResult = withContext(Dispatchers.IO) {
    val cleanUrl = baseUrl.trim().trimEnd('/')
    if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
      throw IllegalArgumentException("Server URL must start with http:// or https://")
    }
    val req = Request.Builder()
      .url("$cleanUrl/api/health")
      .get()
      .build()
    val json = executeJson(req)
    ServerHealthResult(
      ok = json.optBoolean("ok", true),
      storageMode = json.optString("storageMode", "persistent-disk"),
      serverTime = json.optLong("serverTime", System.currentTimeMillis())
    )
  }

  suspend fun loginEmployee(baseUrl: String, name: String, mobile: String): UserSession =
    withContext(Dispatchers.IO) {
      val bodyJson = JSONObject().apply {
        put("name", name.trim())
        put("mobile", mobile.trim())
      }
      val respJson = executeJson(
        Request.Builder()
          .url("${baseUrl.trimEnd('/')}/api/auth/login")
          .post(bodyJson.toString().toRequestBody(jsonMediaType))
          .build()
      )
      parseSession(respJson)
    }

  suspend fun loginAdmin(baseUrl: String, password: String): UserSession =
    withContext(Dispatchers.IO) {
      val bodyJson = JSONObject().apply {
        put("password", password)
      }
      val respJson = executeJson(
        Request.Builder()
          .url("${baseUrl.trimEnd('/')}/api/auth/admin/login")
          .post(bodyJson.toString().toRequestBody(jsonMediaType))
          .build()
      )
      parseSession(respJson)
    }

  suspend fun syncSince(baseUrl: String, token: String, since: Long): SyncPayload =
    withContext(Dispatchers.IO) {
      val respJson = executeJson(
        Request.Builder()
          .url("${baseUrl.trimEnd('/')}/api/sync?since=$since")
          .header("Authorization", "Bearer $token")
          .get()
          .build()
      )
      SyncPayload(
        serverTime = respJson.optLong("serverTime", System.currentTimeMillis()),
        fullSync = respJson.optBoolean("fullSync", since == 0L),
        departments = parseDepartmentList(respJson.optJSONArray("departments")),
        contentItems = parseContentList(respJson.optJSONArray("contentItems")),
        requests = parseRequestList(respJson.optJSONArray("requests")),
        deletedDepartments = parseStringList(respJson.optJSONArray("deletedDepartments")),
        deletedContent = parseStringList(respJson.optJSONArray("deletedContent")),
        deletedRequests = parseStringList(respJson.optJSONArray("deletedRequests"))
      )
    }

  suspend fun saveDepartment(
    baseUrl: String,
    token: String,
    id: String?,
    name: String,
    iconKey: String,
    orderIndex: Int? = null
  ): Department = withContext(Dispatchers.IO) {
    val bodyJson = JSONObject().apply {
      if (!id.isNullOrBlank()) put("id", id)
      put("name", name.trim())
      put("iconKey", iconKey)
      if (orderIndex != null) put("orderIndex", orderIndex)
    }
    val req = if (id.isNullOrBlank()) {
      Request.Builder()
        .url("${baseUrl.trimEnd('/')}/api/departments")
        .header("Authorization", "Bearer $token")
        .post(bodyJson.toString().toRequestBody(jsonMediaType))
        .build()
    } else {
      Request.Builder()
        .url("${baseUrl.trimEnd('/')}/api/departments/$id")
        .header("Authorization", "Bearer $token")
        .put(bodyJson.toString().toRequestBody(jsonMediaType))
        .build()
    }
    val resp = executeJson(req)
    parseDepartment(resp.getJSONObject("department"))
  }

  suspend fun deleteDepartment(baseUrl: String, token: String, id: String) =
    withContext(Dispatchers.IO) {
      executeJson(
        Request.Builder()
          .url("${baseUrl.trimEnd('/')}/api/departments/$id")
          .header("Authorization", "Bearer $token")
          .delete()
          .build()
      )
      Unit
    }

  suspend fun saveContent(baseUrl: String, token: String, item: ContentItem): ContentItem =
    withContext(Dispatchers.IO) {
      val bodyJson = JSONObject().apply {
        if (item.id.isNotBlank()) put("id", item.id)
        put("departmentId", item.departmentId)
        put("category", item.category)
        put("subcategory", item.subcategory)
        put("title", item.title)
        put("description", item.description)
        put("contentBody", item.contentBody)
        put("contentType", item.contentType.name)
        put("attachmentFileId", item.attachmentFileId ?: JSONObject.NULL)
        put("attachmentName", item.attachmentName ?: JSONObject.NULL)
        put("attachmentUrlOrPath", item.attachmentUrlOrPath ?: JSONObject.NULL)
        put("attachmentSizeBytes", item.attachmentSizeBytes)
        put("attachmentMimeType", item.attachmentMimeType ?: JSONObject.NULL)
        put("isPublished", item.isPublished)
        put("isAnnouncement", item.isAnnouncement)
      }
      val resp = executeJson(
        Request.Builder()
          .url("${baseUrl.trimEnd('/')}/api/content")
          .header("Authorization", "Bearer $token")
          .post(bodyJson.toString().toRequestBody(jsonMediaType))
          .build()
      )
      parseContentItem(resp.getJSONObject("contentItem"))
    }

  suspend fun setContentPublishStatus(
    baseUrl: String,
    token: String,
    id: String,
    isPublished: Boolean
  ): ContentItem = withContext(Dispatchers.IO) {
    val bodyJson = JSONObject().apply { put("isPublished", isPublished) }
    val resp = executeJson(
      Request.Builder()
        .url("${baseUrl.trimEnd('/')}/api/content/$id/publish")
        .header("Authorization", "Bearer $token")
        .patch(bodyJson.toString().toRequestBody(jsonMediaType))
        .build()
    )
    parseContentItem(resp.getJSONObject("contentItem"))
  }

  suspend fun deleteContent(baseUrl: String, token: String, id: String) =
    withContext(Dispatchers.IO) {
      executeJson(
        Request.Builder()
          .url("${baseUrl.trimEnd('/')}/api/content/$id")
          .header("Authorization", "Bearer $token")
          .delete()
          .build()
      )
      Unit
    }

  suspend fun uploadFile(
    baseUrl: String,
    token: String,
    fileName: String,
    mimeType: String,
    bytes: ByteArray,
    idempotencyKey: String
  ): UploadedFileResult = withContext(Dispatchers.IO) {
    val mediaType = mimeType.toMediaType()
    val multipart = MultipartBody.Builder()
      .setType(MultipartBody.FORM)
      .addFormDataPart("file", fileName, bytes.toRequestBody(mediaType))
      .build()
    val req = Request.Builder()
      .url("${baseUrl.trimEnd('/')}/api/files/upload")
      .header("Authorization", "Bearer $token")
      .header("X-Idempotency-Key", idempotencyKey)
      .post(multipart)
      .build()
    val resp = executeJson(req)
    val f = resp.getJSONObject("file")
    UploadedFileResult(
      id = f.getString("id"),
      originalName = f.getString("originalName"),
      mimeType = f.optString("mimeType", mimeType),
      sizeBytes = f.optLong("sizeBytes", bytes.size.toLong()),
      url = f.getString("url")
    )
  }

  suspend fun downloadFileToTarget(
    baseUrl: String,
    token: String,
    urlOrPath: String,
    destFile: File
  ): File = withContext(Dispatchers.IO) {
    val fullUrl = if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
      urlOrPath
    } else {
      "${baseUrl.trimEnd('/')}${if (urlOrPath.startsWith("/")) urlOrPath else "/$urlOrPath"}"
    }
    val req = Request.Builder()
      .url(fullUrl)
      .header("Authorization", "Bearer $token")
      .get()
      .build()
    httpClient.newCall(req).execute().use { resp ->
      if (!resp.isSuccessful) {
        throw ApiException("Download failed (${resp.code})", resp.code)
      }
      val body = resp.body ?: throw ApiException("Empty file response", resp.code)
      destFile.parentFile?.mkdirs()
      val tmp = File(destFile.parentFile, "${destFile.name}.tmp")
      body.byteStream().use { input ->
        tmp.outputStream().use { output ->
          input.copyTo(output)
        }
      }
      if (destFile.exists()) destFile.delete()
      tmp.renameTo(destFile)
      destFile
    }
  }

  suspend fun submitRequest(
    baseUrl: String,
    token: String,
    clientRequestId: String,
    departmentId: String,
    departmentName: String,
    title: String,
    details: String
  ): UserRequest = withContext(Dispatchers.IO) {
    val bodyJson = JSONObject().apply {
      put("clientRequestId", clientRequestId)
      put("departmentId", departmentId)
      put("departmentName", departmentName)
      put("title", title)
      put("details", details)
    }
    val resp = executeJson(
      Request.Builder()
        .url("${baseUrl.trimEnd('/')}/api/requests")
        .header("Authorization", "Bearer $token")
        .post(bodyJson.toString().toRequestBody(jsonMediaType))
        .build()
    )
    parseUserRequest(resp.getJSONObject("request"))
  }

  suspend fun updateRequestByAdmin(
    baseUrl: String,
    token: String,
    requestId: String,
    status: RequestStatus,
    adminReply: String?
  ): UserRequest = withContext(Dispatchers.IO) {
    val bodyJson = JSONObject().apply {
      put("status", status.name)
      if (adminReply != null) put("adminReply", adminReply)
    }
    val resp = executeJson(
      Request.Builder()
        .url("${baseUrl.trimEnd('/')}/api/requests/$requestId")
        .header("Authorization", "Bearer $token")
        .patch(bodyJson.toString().toRequestBody(jsonMediaType))
        .build()
    )
    parseUserRequest(resp.getJSONObject("request"))
  }

  suspend fun deleteRequestByAdmin(baseUrl: String, token: String, requestId: String) =
    withContext(Dispatchers.IO) {
      executeJson(
        Request.Builder()
          .url("${baseUrl.trimEnd('/')}/api/requests/$requestId")
          .header("Authorization", "Bearer $token")
          .delete()
          .build()
      )
      Unit
    }

  private fun executeJson(request: Request): JSONObject {
    httpClient.newCall(request).execute().use { resp ->
      val text = resp.body?.string().orEmpty()
      val json = if (text.isNotBlank()) {
        try {
          JSONObject(text)
        } catch (_: Exception) {
          JSONObject()
        }
      } else {
        JSONObject()
      }
      if (!resp.isSuccessful) {
        val msg = json.optString("error").ifBlank { "Server error (${resp.code})" }
        throw ApiException(msg, resp.code)
      }
      return json
    }
  }

  private fun parseSession(json: JSONObject): UserSession {
    val user = json.getJSONObject("user")
    val role = try {
      UserRole.valueOf(user.optString("role", UserRole.EMPLOYEE.name))
    } catch (_: Exception) {
      UserRole.EMPLOYEE
    }
    return UserSession(
      userId = user.getString("id"),
      name = user.getString("name"),
      mobile = user.optString("mobile", ""),
      role = role,
      token = json.getString("token")
    )
  }

  companion object {
    fun parseDepartment(obj: JSONObject): Department = Department(
      id = obj.getString("id"),
      name = obj.getString("name"),
      iconKey = obj.optString("iconKey", "factory"),
      orderIndex = obj.optInt("orderIndex", 0),
      createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
      updatedAt = obj.optLong("updatedAt", obj.optLong("createdAt", System.currentTimeMillis()))
    )

    fun parseDepartmentList(arr: JSONArray?): List<Department> {
      if (arr == null) return emptyList()
      return (0 until arr.length()).map { parseDepartment(arr.getJSONObject(it)) }
    }

    fun parseContentItem(obj: JSONObject): ContentItem {
      val typeStr = obj.optString("contentType", ContentType.TEXT.name)
      val type = try { ContentType.valueOf(typeStr) } catch (_: Exception) { ContentType.TEXT }
      return ContentItem(
        id = obj.getString("id"),
        departmentId = obj.getString("departmentId"),
        category = obj.optString("category", ""),
        subcategory = obj.optString("subcategory", ""),
        title = obj.getString("title"),
        description = obj.optString("description", ""),
        contentBody = obj.optString("contentBody", ""),
        contentType = type,
        attachmentFileId = obj.optString("attachmentFileId").takeIf { it.isNotBlank() && it != "null" },
        attachmentName = obj.optString("attachmentName").takeIf { it.isNotBlank() && it != "null" },
        attachmentUrlOrPath = obj.optString("attachmentUrlOrPath").takeIf { it.isNotBlank() && it != "null" },
        attachmentSizeBytes = obj.optLong("attachmentSizeBytes", 0L),
        attachmentMimeType = obj.optString("attachmentMimeType").takeIf { it.isNotBlank() && it != "null" },
        isPublished = obj.optBoolean("isPublished", true),
        isAnnouncement = obj.optBoolean("isAnnouncement", false),
        publishedAt = obj.optLong("publishedAt", System.currentTimeMillis()),
        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
        adminAuthor = obj.optString("adminAuthor", "Admin")
      )
    }

    fun parseContentList(arr: JSONArray?): List<ContentItem> {
      if (arr == null) return emptyList()
      return (0 until arr.length()).map { parseContentItem(arr.getJSONObject(it)) }
    }

    fun parseUserRequest(obj: JSONObject): UserRequest {
      val statusStr = obj.optString("status", RequestStatus.PENDING.name)
      val status = try { RequestStatus.valueOf(statusStr) } catch (_: Exception) { RequestStatus.PENDING }
      return UserRequest(
        id = obj.getString("id"),
        clientRequestId = obj.optString("clientRequestId", ""),
        userId = obj.getString("userId"),
        userName = obj.optString("userName", ""),
        userMobile = obj.optString("userMobile", ""),
        departmentId = obj.optString("departmentId", ""),
        departmentName = obj.optString("departmentName", ""),
        title = obj.optString("title", "Request"),
        details = obj.optString("details", ""),
        status = status,
        adminReply = obj.optString("adminReply", ""),
        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
        repliedAt = if (obj.isNull("repliedAt")) null else obj.optLong("repliedAt"),
        isPendingSync = false
      )
    }

    fun parseRequestList(arr: JSONArray?): List<UserRequest> {
      if (arr == null) return emptyList()
      return (0 until arr.length()).map { parseUserRequest(arr.getJSONObject(it)) }
    }

    fun parseStringList(arr: JSONArray?): List<String> {
      if (arr == null) return emptyList()
      return (0 until arr.length()).map { arr.getString(it) }
    }
  }
}
