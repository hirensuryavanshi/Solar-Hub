package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.solarhub.data.ContentItemEntity
import com.example.solarhub.data.DepartmentEntity
import com.example.solarhub.data.SessionManager
import com.example.solarhub.data.SolarHubDatabase
import com.example.solarhub.data.SolarHubRepository
import com.example.solarhub.data.UserRequestEntity
import com.example.solarhub.model.ContentType
import com.example.solarhub.model.RequestStatus
import com.example.solarhub.model.UserRole
import com.example.solarhub.network.ApiException
import com.example.solarhub.network.SolarHubApiClient
import com.example.solarhub.network.SolarHubRealtimeClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SolarHubSyncTest {

  @Test
  fun `migration 1 to 2 preserves existing departments and content while adding user_requests`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbName = "migration_test_${System.currentTimeMillis()}.db"

    // 1. Create a v1 database using FrameworkSQLiteOpenHelper directly
    val config = SupportSQLiteOpenHelper.Configuration.builder(context)
      .name(dbName)
      .callback(object : SupportSQLiteOpenHelper.Callback(1) {
        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
          db.execSQL("""
            CREATE TABLE IF NOT EXISTS departments (
              id TEXT NOT NULL PRIMARY KEY,
              name TEXT NOT NULL,
              iconKey TEXT NOT NULL,
              orderIndex INTEGER NOT NULL,
              createdAt INTEGER NOT NULL
            )
          """.trimIndent())
          db.execSQL("""
            CREATE TABLE IF NOT EXISTS content_items (
              id TEXT NOT NULL PRIMARY KEY,
              departmentId TEXT NOT NULL,
              category TEXT NOT NULL,
              subcategory TEXT NOT NULL,
              title TEXT NOT NULL,
              description TEXT NOT NULL,
              contentBody TEXT NOT NULL,
              contentType TEXT NOT NULL,
              attachmentName TEXT,
              attachmentUrlOrPath TEXT,
              attachmentSizeBytes INTEGER NOT NULL,
              attachmentMimeType TEXT,
              isPublished INTEGER NOT NULL,
              publishedAt INTEGER NOT NULL,
              updatedAt INTEGER NOT NULL,
              adminAuthor TEXT NOT NULL
            )
          """.trimIndent())
          db.execSQL("INSERT INTO departments (id, name, iconKey, orderIndex, createdAt) VALUES ('dept_custom', 'Custom Dept', 'factory', 99, 12345)")
          db.execSQL("INSERT INTO content_items (id, departmentId, category, subcategory, title, description, contentBody, contentType, attachmentName, attachmentUrlOrPath, attachmentSizeBytes, attachmentMimeType, isPublished, publishedAt, updatedAt, adminAuthor) VALUES ('item_1', 'dept_custom', 'SOP', '', 'Legacy SOP', 'Preserved', 'Body', 'TEXT', NULL, NULL, 0, NULL, 1, 1000, 1000, 'Admin')")
        }

        override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
      })
      .build()
    val helper = FrameworkSQLiteOpenHelperFactory().create(config)
    helper.writableDatabase.close()

    // 2. Open with Room v2 + MIGRATION_1_2
    val roomDb = Room.databaseBuilder(context, SolarHubDatabase::class.java, dbName)
      .addMigrations(SolarHubDatabase.MIGRATION_1_2)
      .allowMainThreadQueries()
      .build()

    try {
      val dao = roomDb.solarHubDao()
      val depts = dao.getAllDepartments().first()
      assertTrue(depts.any { it.id == "dept_custom" && it.name == "Custom Dept" })

      val items = dao.getPublishedContentByDepartment("dept_custom").first()
      assertEquals(1, items.size)
      assertEquals("Legacy SOP", items.first().title)

      // Insert into new user_requests table
      dao.insertRequest(
        UserRequestEntity(
          id = "req_1",
          clientRequestId = "c_1",
          userId = "user_a",
          userName = "User A",
          userMobile = "9876543210",
          departmentId = "dept_custom",
          departmentName = "Custom Dept",
          title = "Test Request",
          details = "Details",
          status = RequestStatus.PENDING.name,
          adminReply = "",
          createdAt = 5000L,
          updatedAt = 5000L,
          repliedAt = null,
          isPendingSync = false
        )
      )
      val userAReqs = dao.getRequestsByUser("user_a").first()
      assertEquals(1, userAReqs.size)
    } finally {
      roomDb.close()
      context.deleteDatabase(dbName)
    }
  }

  @Test
  fun `private requests are strictly isolated per user in local Room cache`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, SolarHubDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    try {
      val dao = db.solarHubDao()
      dao.insertRequest(
        UserRequestEntity(
          id = "req_a",
          clientRequestId = "ca",
          userId = "user_a",
          userName = "Alice",
          userMobile = "9876543210",
          departmentId = "dept_safety",
          departmentName = "Safety",
          title = "Private Request A",
          details = "Sensitive details",
          status = RequestStatus.PENDING.name,
          adminReply = "",
          createdAt = 100L,
          updatedAt = 100L,
          repliedAt = null,
          isPendingSync = false
        )
      )

      // User B query must return 0 items
      assertEquals(0, dao.getRequestsByUser("user_b").first().size)

      // Switching session to User B clears non-owned synced requests
      dao.clearRequestsNotOwnedBy("user_b")
      assertEquals(0, dao.getAllRequests().first().size)
    } finally {
      db.close()
    }
  }

  @Test
  fun `SolarHubApiClient authenticates, syncs, uploads and downloads binary files accurately`() = runBlocking {
    val samplePdf = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF".toByteArray()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

    server.createContext("/api/auth/login") { ex ->
      val body = """{"user":{"id":"u_1","name":"Rajesh","mobile":"9876543210","role":"EMPLOYEE"},"token":"tok_emp"}"""
      ex.responseHeaders.add("Content-Type", "application/json")
      ex.sendResponseHeaders(200, body.toByteArray().size.toLong())
      ex.responseBody.use { it.write(body.toByteArray()) }
    }

    server.createContext("/api/auth/admin/login") { ex ->
      val body = """{"error":"Invalid admin credentials."}"""
      ex.responseHeaders.add("Content-Type", "application/json")
      ex.sendResponseHeaders(401, body.toByteArray().size.toLong())
      ex.responseBody.use { it.write(body.toByteArray()) }
    }

    server.createContext("/api/sync") { ex ->
      val auth = ex.requestHeaders.getFirst("Authorization")
      assertEquals("Bearer tok_emp", auth)
      val body = """
        {
          "serverTime": 999999,
          "fullSync": true,
          "departments": [{"id":"dept_renewable_energy","name":"Renewable Energy","iconKey":"eco","orderIndex":10,"createdAt":1000,"updatedAt":1000}],
          "contentItems": [{"id":"c_1","departmentId":"dept_renewable_energy","category":"Notice","subcategory":"","title":"Plant Notice","description":"Read now","contentBody":"All shifts","contentType":"NOTICE","attachmentFileId":"file_1","attachmentName":"sop.pdf","attachmentUrlOrPath":"/api/files/file_1","attachmentSizeBytes":${samplePdf.size},"attachmentMimeType":"application/pdf","isPublished":true,"isAnnouncement":true,"publishedAt":2000,"updatedAt":2000,"adminAuthor":"Admin"}],
          "requests": [],
          "deletedDepartments": [],
          "deletedContent": [],
          "deletedRequests": []
        }
      """.trimIndent()
      ex.responseHeaders.add("Content-Type", "application/json")
      ex.sendResponseHeaders(200, body.toByteArray().size.toLong())
      ex.responseBody.use { it.write(body.toByteArray()) }
    }

    server.createContext("/api/files/file_1") { ex ->
      val auth = ex.requestHeaders.getFirst("Authorization")
      assertEquals("Bearer tok_emp", auth)
      ex.responseHeaders.add("Content-Type", "application/pdf")
      ex.sendResponseHeaders(200, samplePdf.size.toLong())
      ex.responseBody.use { it.write(samplePdf) }
    }

    server.start()
    val baseUrl = "http://127.0.0.1:${server.address.port}"
    val client = SolarHubApiClient()
    val tmpDir = kotlin.io.path.createTempDirectory("solarhub_dl_test").toFile()

    try {
      val session = client.loginEmployee(baseUrl, "Rajesh", "9876543210")
      assertEquals("u_1", session.userId)
      assertEquals(UserRole.EMPLOYEE, session.role)

      var rejected = false
      try {
        client.loginAdmin(baseUrl, "bad-password")
      } catch (e: ApiException) {
        rejected = e.code == 401
      }
      assertTrue(rejected)

      val sync = client.syncSince(baseUrl, session.token, 0L)
      assertEquals(1, sync.departments.size)
      assertEquals("Renewable Energy", sync.departments.first().name)
      assertEquals(1, sync.contentItems.size)
      assertTrue(sync.contentItems.first().isAnnouncement)
      assertEquals(ContentType.NOTICE, sync.contentItems.first().contentType)

      val dest = File(tmpDir, "sop.pdf")
      val downloaded = client.downloadFileToTarget(baseUrl, session.token, "/api/files/file_1", dest)
      assertTrue(downloaded.exists())
      assertArrayEquals(samplePdf, downloaded.readBytes())
    } finally {
      server.stop(0)
      tmpDir.deleteRecursively()
    }
  }

  @Test
  fun `offline employee login queues request locally then upgrades session and flushes when server connects`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, SolarHubDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val testScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val apiClient = SolarHubApiClient()
    val sessionManager = SessionManager(context)
    val realtimeClient = SolarHubRealtimeClient(context, apiClient.httpClient, testScope)
    val repo = SolarHubRepository(db.solarHubDao(), sessionManager, apiClient, realtimeClient, testScope)

    // Pick a closed port to simulate an unreachable server initially
    val closedPort = ServerSocket(0).use { it.localPort }
    repo.ensureInitialData()
    repo.setApiBaseUrl("http://127.0.0.1:$closedPort")

    val requestFlushed = AtomicBoolean(false)
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/api/auth/login") { ex ->
      val body = """{"user":{"id":"srv_emp_9998887776","name":"Hiren","mobile":"9998887776","role":"EMPLOYEE"},"token":"tok_upgraded"}"""
      ex.responseHeaders.add("Content-Type", "application/json")
      ex.sendResponseHeaders(200, body.toByteArray().size.toLong())
      ex.responseBody.use { it.write(body.toByteArray()) }
    }
    server.createContext("/api/requests") { ex ->
      val auth = ex.requestHeaders.getFirst("Authorization")
      assertEquals("Bearer tok_upgraded", auth)
      requestFlushed.set(true)
      val body = """{"request":{"id":"srv_req_1","clientRequestId":"cid_1","userId":"srv_emp_9998887776","userName":"Hiren","userMobile":"9998887776","departmentId":"dept_production","departmentName":"Production","title":"Line SOP","details":"Need cell line SOP","status":"PENDING","adminReply":"","createdAt":5000,"updatedAt":5000,"repliedAt":null},"duplicate":false}"""
      ex.responseHeaders.add("Content-Type", "application/json")
      ex.sendResponseHeaders(201, body.toByteArray().size.toLong())
      ex.responseBody.use { it.write(body.toByteArray()) }
    }
    server.createContext("/api/sync") { ex ->
      val body = """
        {
          "serverTime": 777777,
          "fullSync": true,
          "departments": [{"id":"dept_production","name":"Production","iconKey":"factory","orderIndex":1,"createdAt":1000,"updatedAt":1000}],
          "contentItems": [{"id":"c_ann_1","departmentId":"dept_production","category":"Notice","subcategory":"","title":"Centralized Live Update","description":"Synced","contentBody":"Shift A notice","contentType":"NOTICE","attachmentFileId":null,"attachmentName":null,"attachmentUrlOrPath":null,"attachmentSizeBytes":0,"attachmentMimeType":null,"isPublished":true,"isAnnouncement":true,"publishedAt":6000,"updatedAt":6000,"adminAuthor":"Admin"}],
          "requests": [{"id":"srv_req_1","clientRequestId":"cid_1","userId":"srv_emp_9998887776","userName":"Hiren","userMobile":"9998887776","departmentId":"dept_production","departmentName":"Production","title":"Line SOP","details":"Need cell line SOP","status":"PENDING","adminReply":"","createdAt":5000,"updatedAt":5000,"repliedAt":null}],
          "deletedDepartments": [],
          "deletedContent": [],
          "deletedRequests": []
        }
      """.trimIndent()
      ex.responseHeaders.add("Content-Type", "application/json")
      ex.sendResponseHeaders(200, body.toByteArray().size.toLong())
      ex.responseBody.use { it.write(body.toByteArray()) }
    }

    try {
      // 1. Login offline succeeds cleanly instead of blocking user on LoginScreen
      val loginRes = repo.loginEmployee("Hiren", "9998887776")
      assertTrue(loginRes.isSuccess)
      val offlineSession = loginRes.getOrThrow()
      assertTrue(offlineSession.isOfflineSession)
      assertEquals("", offlineSession.token)

      // 2. Submit request offline queues locally with isPendingSync = true
      val submitRes = repo.submitRequest("dept_production", "Production", "Line SOP", "Need cell line SOP")
      assertTrue(submitRes.isSuccess)
      val queuedReq = submitRes.getOrThrow()
      assertTrue(queuedReq.isPendingSync)
      assertTrue(queuedReq.id.startsWith("local_"))

      // 3. Connect to live backend and trigger syncNow() -> upgrades session, flushes queue, pulls announcements
      server.start()
      repo.setApiBaseUrl("http://127.0.0.1:${server.address.port}")
      val syncRes = repo.syncNow()
      assertTrue(syncRes.isSuccess)
      assertTrue(requestFlushed.get())

      val upgraded = repo.sessionFlow.first()
      assertNotNull(upgraded)
      assertFalse(upgraded!!.isOfflineSession)
      assertEquals("tok_upgraded", upgraded.token)
      assertEquals("srv_emp_9998887776", upgraded.userId)

      val reqs = repo.getRequestsByUser("srv_emp_9998887776").first()
      assertEquals(1, reqs.size)
      assertEquals("srv_req_1", reqs.first().id)
      assertFalse(reqs.first().isPendingSync)

      val announcements = repo.getPublishedAnnouncements().first()
      assertEquals(1, announcements.size)
      assertEquals("Centralized Live Update", announcements.first().content.title)
    } finally {
      repo.logout()
      server.stop(0)
      testScope.cancel()
      db.close()
    }
  }
}
