package com.example.solarhub.data

import android.content.Context
import com.example.solarhub.model.ContentItem
import com.example.solarhub.ui.components.AttachmentUtils
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Keeps this phone and the online database in step:
 *  - everyone downloads what the admin published (text, pictures, videos, documents)
 *  - the admin uploads posts, sees all users and requests, blocks users
 * The local database stays the place the screens read from, so the app also works offline.
 */
class CloudSync(
  context: Context,
  private val dao: SolarHubDao
) {
  private val appContext = context.applicationContext
  private val prefs = appContext.getSharedPreferences("solar_hub_cloud", Context.MODE_PRIVATE)
  private val contentLock = Mutex()

  // ---------- small helpers ----------

  private fun knownIds(): MutableSet<String> =
    HashSet(prefs.getStringSet("cloud_ids", emptySet()) ?: emptySet())

  private fun saveKnownIds(ids: Set<String>) {
    prefs.edit().putStringSet("cloud_ids", HashSet(ids)).apply()
  }

  fun isKnown(id: String): Boolean = knownIds().contains(id)

  private fun pendingRequestIds(): MutableSet<String> =
    HashSet(prefs.getStringSet("pending_requests", emptySet()) ?: emptySet())

  private fun savePendingRequestIds(ids: Set<String>) {
    prefs.edit().putStringSet("pending_requests", HashSet(ids)).apply()
  }

  private fun adminBody(pw: String) = JSONObject().put("pw", pw)

  private fun safeName(name: String) = name.replace(Regex("[^A-Za-z0-9._-]"), "_")

  // ---------- users ----------

  /** Saves this user online. Returns true if blocked, false if fine, null if no internet. */
  suspend fun registerUser(name: String, mobile: String): Boolean? {
    val result = SupabaseApi.rpc(
      "register_user",
      JSONObject().put("p_name", name).put("p_mobile", mobile)
    )?.trim() ?: return null
    return when (result) {
      "true" -> true
      "false" -> false
      else -> null
    }
  }

  suspend fun adminSetBlocked(pw: String, mobile: String, blocked: Boolean): Boolean =
    SupabaseApi.rpc(
      "admin_set_blocked",
      adminBody(pw).put("p_mobile", mobile).put("p_blocked", blocked)
    ) != null

  suspend fun adminDeleteUser(pw: String, mobile: String): Boolean =
    SupabaseApi.rpc("admin_delete_user", adminBody(pw).put("p_mobile", mobile)) != null

  // ---------- requests ----------

  suspend fun submitRequest(id: String, name: String, mobile: String, message: String): Boolean {
    val ok = SupabaseApi.rpc(
      "submit_request",
      JSONObject()
        .put("p_id", id)
        .put("p_name", name)
        .put("p_mobile", mobile)
        .put("p_message", message)
    ) != null
    val pending = pendingRequestIds()
    if (ok) pending.remove(id) else pending.add(id)
    savePendingRequestIds(pending)
    return ok
  }

  suspend fun adminUpdateRequest(pw: String, id: String, status: String, reply: String): Boolean =
    SupabaseApi.rpc(
      "admin_update_request",
      adminBody(pw).put("p_id", id).put("p_status", status).put("p_reply", reply)
    ) != null

  suspend fun adminDeleteRequest(pw: String, id: String): Boolean =
    SupabaseApi.rpc("admin_delete_request", adminBody(pw).put("p_id", id)) != null

  private fun parseRequests(text: String): List<UserRequestEntity> {
    val array = JSONArray(text)
    val list = ArrayList<UserRequestEntity>()
    for (i in 0 until array.length()) {
      val o = array.getJSONObject(i)
      list.add(
        UserRequestEntity(
          id = o.getString("id"),
          userName = o.optString("user_name"),
          userMobile = o.optString("user_mobile"),
          message = o.optString("message"),
          status = o.optString("status", "PENDING"),
          adminReply = o.optString("admin_reply"),
          createdAt = o.optLong("created_at"),
          updatedAt = o.optLong("updated_at")
        )
      )
    }
    return list
  }

  /** A normal user: get my requests (with the admin's status / reply) and retry unsent ones. */
  suspend fun pullMyRequests(mobile: String) {
    // Retry requests that could not be sent earlier
    val pending = pendingRequestIds()
    for (id in pending.toList()) {
      val local = dao.getRequestOnce(id) ?: continue
      submitRequest(local.id, local.userName, local.userMobile, local.message)
    }
    val text = SupabaseApi.rpc("my_requests", JSONObject().put("p_mobile", mobile)) ?: return
    val list = try { parseRequests(text) } catch (e: Exception) { return }
    dao.insertRequests(list)
    val keep = list.map { it.id } + pendingRequestIds()
    dao.deleteRequestsNotIn(keep)
  }

  /** The admin: get all users and all requests. */
  suspend fun pullAdminData(pw: String) {
    SupabaseApi.rpc("admin_list_users", adminBody(pw))?.let { text ->
      try {
        val array = JSONArray(text)
        val users = ArrayList<AppUserEntity>()
        for (i in 0 until array.length()) {
          val o = array.getJSONObject(i)
          users.add(
            AppUserEntity(
              mobile = o.getString("mobile"),
              name = o.optString("name"),
              isBlocked = o.optBoolean("is_blocked"),
              createdAt = o.optLong("created_at"),
              lastLoginAt = o.optLong("last_login_at")
            )
          )
        }
        dao.insertUsers(users)
        dao.deleteUsersNotIn(users.map { it.mobile })
      } catch (e: Exception) {
        // ignore, try again next time
      }
    }
    SupabaseApi.rpc("admin_list_requests", adminBody(pw))?.let { text ->
      try {
        val list = parseRequests(text)
        dao.insertRequests(list)
        dao.deleteRequestsNotIn(list.map { it.id })
      } catch (e: Exception) {
        // ignore, try again next time
      }
    }
  }

  // ---------- posts ----------

  /** Downloads everything the admin published, so every user sees it. */
  suspend fun pullContent() = contentLock.withLock {
    val text = SupabaseApi.get(
      "content_items?select=*&is_published=eq.true&order=published_at.desc"
    ) ?: return@withLock
    val array = try { JSONArray(text) } catch (e: Exception) { return@withLock }

    val cloudIds = HashSet<String>()
    for (i in 0 until array.length()) {
      val o = array.getJSONObject(i)
      val id = o.getString("id")
      cloudIds.add(id)
      val updatedAt = o.optLong("updated_at")
      val local = dao.getContentOnce(id)
      if (local != null && local.updatedAt >= updatedAt) continue

      val attachments = o.optJSONArray("attachments") ?: JSONArray()
      val files = ArrayList<AttachmentUtils.Attachment>()
      var failed = false
      for (j in 0 until attachments.length()) {
        val a = attachments.getJSONObject(j)
        val url = a.optString("url")
        val name = a.optString("name", "file")
        val mime = a.optString("mime").ifBlank { null }
        val destination = File(
          File(appContext.filesDir, "attachments/cloud_${id}_$updatedAt"),
          "${j}_${safeName(name)}"
        )
        if (!destination.exists() || destination.length() == 0L) {
          if (!SupabaseApi.download(url, destination)) {
            failed = true
            break
          }
        }
        files.add(AttachmentUtils.Attachment(destination.absolutePath, name, mime))
      }
      if (failed) continue // try again next time

      val encoded = AttachmentUtils.encode(files)
      dao.insertContent(
        ContentItemEntity(
          id = id,
          departmentId = o.optString("department_id"),
          category = "",
          subcategory = "",
          title = o.optString("title"),
          description = o.optString("description"),
          contentBody = o.optString("content_body"),
          contentType = o.optString("content_type", "TEXT"),
          attachmentName = encoded.names,
          attachmentUrlOrPath = encoded.paths,
          attachmentSizeBytes = encoded.totalSize,
          attachmentMimeType = encoded.mimes,
          isPublished = true,
          publishedAt = o.optLong("published_at"),
          updatedAt = updatedAt,
          adminAuthor = o.optString("admin_author", "Admin")
        )
      )
    }

    // The admin removed or hid a post: remove it from this phone too
    val previouslyKnown = knownIds()
    for (local in dao.getPublishedOnce()) {
      if (local.id in previouslyKnown && local.id !in cloudIds) {
        dao.deleteContent(local.id)
      }
    }
    saveKnownIds(cloudIds)
  }

  /** The admin: upload a post (with all its files). Returns true when finished. */
  suspend fun pushContent(item: ContentItem, pw: String): Boolean = contentLock.withLock {
    val attachments = JSONArray()
    val files = AttachmentUtils.parse(item)
    for ((index, a) in files.withIndex()) {
      val url: String
      if (a.path.startsWith("http")) {
        url = a.path
      } else {
        val file = a.file
        if (!file.exists()) continue
        val objectPath = "${item.id}/${System.currentTimeMillis()}_${index}_${safeName(a.name)}"
        url = SupabaseApi.upload(objectPath, file, a.mime) ?: return@withLock false
      }
      attachments.put(
        JSONObject()
          .put("url", url)
          .put("name", a.name)
          .put("mime", a.mime ?: "")
          .put("size", if (a.file.exists()) a.file.length() else 0L)
      )
    }

    val json = JSONObject()
      .put("id", item.id)
      .put("department_id", item.departmentId)
      .put("title", item.title)
      .put("description", item.description)
      .put("content_body", item.contentBody)
      .put("content_type", item.contentType.name)
      .put("attachments", attachments)
      .put("is_published", item.isPublished)
      .put("published_at", item.publishedAt)
      .put("updated_at", item.updatedAt)
      .put("admin_author", item.adminAuthor)

    val ok = SupabaseApi.rpc("admin_upsert_content", adminBody(pw).put("p_item", json)) != null
    if (ok) {
      val known = knownIds()
      if (item.isPublished) known.add(item.id) else known.remove(item.id)
      saveKnownIds(known)
    }
    ok
  }

  suspend fun deleteContent(id: String, pw: String): Boolean = contentLock.withLock {
    val ok = SupabaseApi.rpc("admin_delete_content", adminBody(pw).put("p_id", id)) != null
    if (ok) {
      val known = knownIds()
      known.remove(id)
      saveKnownIds(known)
    }
    ok
  }

  /** The admin: upload published posts that are only on this phone (for example old posts). */
  suspend fun pushUnsynced(pw: String): Int {
    var pushed = 0
    for (local in dao.getPublishedOnce()) {
      if (!isKnown(local.id)) {
        if (pushContent(local.toModel(), pw)) pushed++
      }
    }
    return pushed
  }
}
