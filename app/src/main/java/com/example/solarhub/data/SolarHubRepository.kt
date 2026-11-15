package com.example.solarhub.data

import com.example.solarhub.model.ContentItem
import com.example.solarhub.model.ContentItemWithDepartment
import com.example.solarhub.model.Department
import com.example.solarhub.model.RequestStatus
import com.example.solarhub.model.SyncConnectionState
import com.example.solarhub.model.UserRequest
import com.example.solarhub.model.UserRole
import com.example.solarhub.model.UserSession
import com.example.solarhub.network.ApiException
import com.example.solarhub.network.RealtimeEvent
import com.example.solarhub.network.ServerHealthResult
import com.example.solarhub.network.SolarHubApiClient
import com.example.solarhub.network.SolarHubRealtimeClient
import com.example.solarhub.network.UploadedFileResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID

class SolarHubRepository(
  private val dao: SolarHubDao,
  private val sessionManager: SessionManager,
  private val apiClient: SolarHubApiClient,
  private val realtimeClient: SolarHubRealtimeClient,
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

  private val syncMutex = Mutex()
  private val _isSyncing = MutableStateFlow(false)
  private val _lastSyncError = MutableStateFlow<String?>(null)
  val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

  val sessionFlow: Flow<UserSession?> = sessionManager.sessionFlow
  val apiBaseUrlFlow: Flow<String> = sessionManager.apiBaseUrlFlow

  val syncStateFlow: Flow<SyncConnectionState> = combine(
    realtimeClient.connectionState,
    _isSyncing
  ) { wsState, syncing ->
    if (syncing) SyncConnectionState.SYNCING else wsState
  }

  val departments: Flow<List<Department>> = dao.getAllDepartments().map { list ->
    list.map { it.toModel() }
  }

  private var activeRealtimeKey: String? = null

  init {
    scope.launch {
      combine(sessionManager.sessionFlow, sessionManager.apiBaseUrlFlow) { session, url ->
        session to url
      }.distinctUntilChanged().collect { (session, url) ->
        if (session == null) {
          activeRealtimeKey = null
          realtimeClient.stop()
        } else if (session.isOfflineSession || session.token.isBlank()) {
          activeRealtimeKey = null
          realtimeClient.stop()
          realtimeClient.updateConnectionState(SyncConnectionState.OFFLINE)
          syncNow()
        } else {
          val key = "$url|${session.token}"
          if (activeRealtimeKey != key) {
            activeRealtimeKey = key
            realtimeClient.start(url, session.token)
            syncNow()
          }
        }
      }
    }

    scope.launch {
      realtimeClient.events.collect { event ->
        handleRealtimeEvent(event)
      }
    }
  }

  private suspend fun handleRealtimeEvent(event: RealtimeEvent) {
    val currentSession = sessionManager.sessionFlow.first()
    when (event) {
      is RealtimeEvent.Connected -> {
        syncNow()
      }
      is RealtimeEvent.DepartmentUpdated -> {
        dao.insertDepartment(DepartmentEntity.fromModel(event.department))
        sessionManager.setLastSyncAt(event.serverTime)
      }
      is RealtimeEvent.DepartmentDeleted -> {
        dao.deleteContentByDepartment(event.id)
        dao.deleteDepartment(event.id)
        sessionManager.setLastSyncAt(event.serverTime)
      }
      is RealtimeEvent.ContentUpdated -> {
        if (event.item.isPublished || currentSession?.role == UserRole.ADMIN) {
          dao.insertContent(ContentItemEntity.fromModel(event.item))
        }
        sessionManager.setLastSyncAt(event.serverTime)
      }
      is RealtimeEvent.ContentUnpublished -> {
        if (currentSession?.role != UserRole.ADMIN) {
          dao.deleteContent(event.id)
        }
        sessionManager.setLastSyncAt(event.serverTime)
      }
      is RealtimeEvent.ContentDeleted -> {
        dao.deleteContent(event.id)
        sessionManager.setLastSyncAt(event.serverTime)
      }
      is RealtimeEvent.RequestUpdated -> {
        if (currentSession != null &&
          (currentSession.role == UserRole.ADMIN || event.request.userId == currentSession.userId)
        ) {
          dao.insertRequest(UserRequestEntity.fromModel(event.request))
        }
        sessionManager.setLastSyncAt(event.serverTime)
      }
      is RealtimeEvent.RequestDeleted -> {
        dao.deleteRequest(event.id)
        sessionManager.setLastSyncAt(event.serverTime)
      }
    }
  }

  suspend fun ensureInitialData() {
    val count = dao.getDepartmentCount()
    if (count == 0) {
      dao.insertDepartments(SolarHubDatabase.INITIAL_DEPARTMENTS)
    }
    // Remove the 3 extra default departments from earlier versions (keep only the official 14)
    listOf("dept_engineering", "dept_purchase", "dept_planning_inventory").forEach { oldId ->
      dao.deleteContentByDepartment(oldId)
      dao.deleteDepartment(oldId)
    }
  }

  suspend fun setApiBaseUrl(url: String) {
    sessionManager.setApiBaseUrl(url)
  }

  suspend fun getApiBaseUrl(): String = sessionManager.getApiBaseUrl()

  suspend fun testServerConnection(url: String): Result<ServerHealthResult> {
    return try {
      val result = apiClient.checkHealthDetailed(url)
      Result.success(result)
    } catch (e: Exception) {
      Result.failure(IOException(formatNetworkError(url, e), e))
    }
  }

  private fun normalizeMobile(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length > 10 && digits.startsWith("91")) digits.takeLast(10) else digits
  }

  private fun formatNetworkError(baseUrl: String, e: Throwable): String {
    return when (e) {
      is ApiException -> e.message ?: "Server error (${e.code})"
      is IllegalArgumentException -> e.message ?: "Invalid server URL"
      is UnknownHostException -> "Server address not found ($baseUrl). Check URL or internet."
      is ConnectException, is SocketTimeoutException ->
        "Cannot reach SOLAR HUB server at $baseUrl. Check Server Connection Settings."
      else -> e.message ?: "Unable to connect to $baseUrl."
    }
  }

  suspend fun loginEmployee(name: String, mobile: String, serverUrlOverride: String? = null): Result<UserSession> {
    val cleanName = name.trim()
    val cleanMobile = normalizeMobile(mobile)
    if (cleanName.length < 2) {
      return Result.failure(IllegalArgumentException("Please enter your full name."))
    }
    if (cleanMobile.length < 10) {
      return Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number."))
    }

    if (!serverUrlOverride.isNullOrBlank()) {
      sessionManager.setApiBaseUrl(serverUrlOverride)
    }
    val baseUrl = sessionManager.getApiBaseUrl()
    val offlineUserId = "emp_$cleanMobile"

    return try {
      val session = apiClient.loginEmployee(baseUrl, cleanName, cleanMobile)
      dao.reassignUserIdForRequests(offlineUserId, session.userId)
      dao.clearDrafts()
      dao.clearRequestsNotOwnedBy(session.userId)
      sessionManager.saveSession(session)
      _lastSyncError.value = null
      syncNow()
      Result.success(session)
    } catch (e: ApiException) {
      Result.failure(e)
    } catch (e: Exception) {
      // Allow employee into offline/cached mode when central server is temporarily unreachable
      val offlineSession = UserSession(
        userId = offlineUserId,
        name = cleanName,
        mobile = cleanMobile,
        role = UserRole.EMPLOYEE,
        token = "",
        isOfflineSession = true
      )
      dao.clearDrafts()
      dao.clearRequestsNotOwnedBy(offlineSession.userId)
      sessionManager.saveSession(offlineSession)
      realtimeClient.updateConnectionState(SyncConnectionState.OFFLINE)
      _lastSyncError.value = formatNetworkError(baseUrl, e)
      Result.success(offlineSession)
    }
  }

  suspend fun loginAdmin(
    password: String,
    serverUrlOverride: String? = null
  ): Result<UserSession> {
    if (!serverUrlOverride.isNullOrBlank()) {
      sessionManager.setApiBaseUrl(serverUrlOverride)
    }
    val baseUrl = sessionManager.getApiBaseUrl()
    return try {
      val session = apiClient.loginAdmin(baseUrl, password)
      sessionManager.saveSession(session)
      _lastSyncError.value = null
      syncNow()
      Result.success(session)
    } catch (e: ApiException) {
      Result.failure(e)
    } catch (e: Exception) {
      Result.failure(IOException(formatNetworkError(baseUrl, e), e))
    }
  }

  suspend fun logout() {
    activeRealtimeKey = null
    realtimeClient.stop()
    sessionManager.clearSession()
    dao.clearDrafts()
    dao.clearSyncedRequests()
    _lastSyncError.value = null
  }

  suspend fun syncNow(): Result<Unit> = syncMutex.withLock {
    var session = sessionManager.sessionFlow.first() ?: return Result.success(Unit)
    val baseUrl = sessionManager.getApiBaseUrl()
    _isSyncing.value = true
    return try {
      // Automatically upgrade an offline employee session once the central server becomes reachable
      if (session.isOfflineSession || session.token.isBlank()) {
        if (session.role != UserRole.EMPLOYEE) {
          return Result.failure(IllegalStateException("Admin session requires online verification."))
        }
        val upgraded = apiClient.loginEmployee(baseUrl, session.name, session.mobile)
        dao.reassignUserIdForRequests(session.userId, upgraded.userId)
        sessionManager.saveSession(upgraded)
        session = upgraded
      }

      // Flush any offline-queued requests first so server returns canonical records
      flushPendingRequestsLocked(baseUrl, session)

      val since = sessionManager.getLastSyncAt()
      val payload = apiClient.syncSince(baseUrl, session.token, since)

      if (payload.departments.isNotEmpty()) {
        dao.insertDepartments(payload.departments.map { DepartmentEntity.fromModel(it) })
      }
      payload.deletedDepartments.forEach { deptId ->
        dao.deleteContentByDepartment(deptId)
        dao.deleteDepartment(deptId)
      }

      if (payload.contentItems.isNotEmpty()) {
        val visibleItems = if (session.role == UserRole.ADMIN) {
          payload.contentItems
        } else {
          payload.contentItems.filter { it.isPublished }
        }
        if (visibleItems.isNotEmpty()) {
          dao.insertContentItems(visibleItems.map { ContentItemEntity.fromModel(it) })
        }
        if (session.role != UserRole.ADMIN) {
          payload.contentItems.filter { !it.isPublished }.forEach { dao.deleteContent(it.id) }
        }
      }
      payload.deletedContent.forEach { contentId ->
        dao.deleteContent(contentId)
      }

      if (session.role != UserRole.ADMIN) {
        dao.clearDrafts()
        dao.clearRequestsNotOwnedBy(session.userId)
      }

      val scopedRequests = if (session.role == UserRole.ADMIN) {
        payload.requests
      } else {
        payload.requests.filter { it.userId == session.userId }
      }
      if (scopedRequests.isNotEmpty()) {
        dao.insertRequests(scopedRequests.map { UserRequestEntity.fromModel(it.copy(isPendingSync = false)) })
      }
      payload.deletedRequests.forEach { reqId ->
        dao.deleteRequest(reqId)
      }

      sessionManager.setLastSyncAt(payload.serverTime)
      _lastSyncError.value = null
      Result.success(Unit)
    } catch (e: Exception) {
      realtimeClient.updateConnectionState(SyncConnectionState.OFFLINE)
      val friendly = formatNetworkError(baseUrl, e)
      _lastSyncError.value = friendly
      Result.failure(IOException(friendly, e))
    } finally {
      _isSyncing.value = false
    }
  }

  private suspend fun flushPendingRequestsLocked(baseUrl: String, session: UserSession) {
    val offlineUserId = "emp_${normalizeMobile(session.mobile)}"
    val pending = dao.getPendingSyncRequests().filter {
      it.userId == session.userId || it.userId == offlineUserId
    }
    for (localReq in pending) {
      try {
        val created = apiClient.submitRequest(
          baseUrl = baseUrl,
          token = session.token,
          clientRequestId = localReq.clientRequestId,
          departmentId = localReq.departmentId,
          departmentName = localReq.departmentName,
          title = localReq.title,
          details = localReq.details
        )
        dao.deleteRequest(localReq.id)
        dao.insertRequest(UserRequestEntity.fromModel(created.copy(isPendingSync = false)))
      } catch (e: ApiException) {
        // Validation/Auth errors won't succeed on retry unless session changes
        if (e.code in 400..499 && e.code != 408 && e.code != 429) {
          break
        }
      } catch (_: IOException) {
        // Still offline, keep queued for next reconnect
        break
      }
    }
  }

  // --- DEPARTMENTS ---
  fun getDepartmentById(id: String): Flow<Department?> {
    return dao.getDepartmentById(id).map { it?.toModel() }
  }

  suspend fun addDepartment(name: String, iconKey: String): Result<Department> {
    val baseUrl = sessionManager.getApiBaseUrl()
    return try {
      val session = sessionManager.sessionFlow.first()
        ?: return Result.failure(IllegalStateException("Not logged in"))
      val existing = departments.first()
      val order = (existing.maxOfOrNull { it.orderIndex } ?: 0) + 1
      val dept = apiClient.saveDepartment(baseUrl, session.token, null, name, iconKey, order)
      dao.insertDepartment(DepartmentEntity.fromModel(dept))
      Result.success(dept)
    } catch (e: ApiException) {
      Result.failure(e)
    } catch (e: Exception) {
      Result.failure(IOException(formatNetworkError(baseUrl, e), e))
    }
  }

  suspend fun updateDepartment(dept: Department): Result<Department> {
    val baseUrl = sessionManager.getApiBaseUrl()
    return try {
      val session = sessionManager.sessionFlow.first()
        ?: return Result.failure(IllegalStateException("Not logged in"))
      val saved = apiClient.saveDepartment(
        baseUrl,
        session.token,
        dept.id,
        dept.name,
        dept.iconKey,
        dept.orderIndex
      )
      dao.insertDepartment(DepartmentEntity.fromModel(saved))
      Result.success(saved)
    } catch (e: ApiException) {
      Result.failure(e)
    } catch (e: Exception) {
      Result.failure(IOException(formatNetworkError(baseUrl, e), e))
    }
  }

  suspend fun deleteDepartment(id: String): Result<Unit> {
    val baseUrl = sessionManager.getApiBaseUrl()
    return try {
      val session = sessionManager.sessionFlow.first()
        ?: return Result.failure(IllegalStateException("Not logged in"))
      apiClient.deleteDepartment(baseUrl, session.token, id)
      dao.deleteContentByDepartment(id)
      dao.deleteDepartment(id)
      Result.success(Unit)
    } catch (e: ApiException) {
      Result.failure(e)
    } catch (e: Exception) {
      Result.failure(IOException(formatNetworkError(baseUrl, e), e))
    }
  }

  // --- CONTENT & ANNOUNCEMENTS ---
  fun getPublishedContentByDepartment(deptId: String): Flow<List<ContentItem>> {
    return dao.getPublishedContentByDepartment(deptId).map { list ->
      list.map { it.toModel() }
    }
  }

  fun getAllContentByDepartment(deptId: String): Flow<List<ContentItem>> {
    return dao.getAllContentByDepartment(deptId).map { list ->
      list.map { it.toModel() }
    }
  }

  fun getLatestPublishedContent(limit: Int = 20): Flow<List<ContentItemWithDepartment>> {
    return combine(dao.getLatestPublishedContent(limit), dao.getAllDepartments()) { contents, depts ->
      val deptMap = depts.associateBy { it.id }
      contents.map { entity ->
        val dept = deptMap[entity.departmentId]
        ContentItemWithDepartment(
          content = entity.toModel(),
          departmentName = dept?.name ?: "Department",
          departmentIconKey = dept?.iconKey ?: "folder"
        )
      }
    }
  }

  fun getPublishedAnnouncements(limit: Int = 10): Flow<List<ContentItemWithDepartment>> {
    return combine(dao.getPublishedAnnouncements(limit), dao.getAllDepartments()) { contents, depts ->
      val deptMap = depts.associateBy { it.id }
      contents.map { entity ->
        val dept = deptMap[entity.departmentId]
        ContentItemWithDepartment(
          content = entity.toModel(),
          departmentName = dept?.name ?: "General",
          departmentIconKey = dept?.iconKey ?: "notifications"
        )
      }
    }
  }

  fun searchPublishedContent(query: String): Flow<List<ContentItemWithDepartment>> {
    return combine(dao.searchPublishedContent(query), dao.getAllDepartments()) { contents, depts ->
      val deptMap = depts.associateBy { it.id }
      contents.map { entity ->
        val dept = deptMap[entity.departmentId]
        ContentItemWithDepartment(
          content = entity.toModel(),
          departmentName = dept?.name ?: "Department",
          departmentIconKey = dept?.iconKey ?: "folder"
        )
      }
    }
  }

  fun getAllDrafts(): Flow<List<ContentItemWithDepartment>> {
    return combine(dao.getAllDrafts(), dao.getAllDepartments()) { contents, depts ->
      val deptMap = depts.associateBy { it.id }
      contents.map { entity ->
        val dept = deptMap[entity.departmentId]
        ContentItemWithDepartment(
          content = entity.toModel(),
          departmentName = dept?.name ?: "Department",
          departmentIconKey = dept?.iconKey ?: "folder"
        )
      }
    }
  }

  fun getAllPublished(): Flow<List<ContentItemWithDepartment>> {
    return combine(dao.getAllPublished(), dao.getAllDepartments()) { contents, depts ->
      val deptMap = depts.associateBy { it.id }
      contents.map { entity ->
        val dept = deptMap[entity.departmentId]
        ContentItemWithDepartment(
          content = entity.toModel(),
          departmentName = dept?.name ?: "Department",
          departmentIconKey = dept?.iconKey ?: "folder"
        )
      }
    }
  }

  fun getContentById(id: String): Flow<ContentItem?> {
    return dao.getContentById(id).map { it?.toModel() }
  }

  suspend fun uploadFile(
    fileName: String,
    mimeType: String,
    bytes: ByteArray,
    idempotencyKey: String = UUID.randomUUID().toString()
  ): Result<UploadedFileResult> {
    val baseUrl = sessionManager.getApiBaseUrl()
    return try {
      val session = sessionManager.sessionFlow.first()
        ?: return Result.failure(IllegalStateException("Not logged in"))
      val uploaded = apiClient.uploadFile(baseUrl, session.token, fileName, mimeType, bytes, idempotencyKey)
      Result.success(uploaded)
    } catch (e: ApiException) {
      Result.failure(e)
    } catch (e: Exception) {
      Result.failure(IOException(formatNetworkError(baseUrl, e), e))
    }
  }

  suspend fun downloadAttachmentToFile(content: ContentItem, destFile: File): Result<File> {
    val baseUrl = sessionManager.getApiBaseUrl()
    return try {
      val session = sessionManager.sessionFlow.first()
        ?: return Result.failure(IllegalStateException("Not logged in"))
      val urlOrPath = content.attachmentUrlOrPath
        ?: content.attachmentFileId?.let { "/api/files/$it" }
        ?: return Result.failure(IllegalStateException("No downloadable attachment URL"))
      val file = apiClient.downloadFileToTarget(baseUrl, session.token, urlOrPath, destFile)
      Result.success(file)
    } catch (e: ApiException) {
      Result.failure(e)
    } catch (e: Exception) {
      Result.failure(IOException(formatNetworkError(baseUrl, e), e))
    }
  }

  suspend fun saveContent(item: ContentItem): Result<ContentItem> {
    val baseUrl = sessionManager.getApiBaseUrl()
    return try {
      val session = sessionManager.sessionFlow.first()
        ?: return Result.failure(IllegalStateException("Not logged in"))
      val saved = apiClient.saveContent(baseUrl, session.token, item)
      dao.insertContent(ContentItemEntity.fromModel(saved))
      Result.success(saved)
    } catch (e: ApiException) {
      Result.failure(e)
    } catch (e: Exception) {
      Result.failure(IOException(formatNetworkError(baseUrl, e), e))
    }
  }

  suspend fun setContentPublishStatus(id: String, isPublished: Boolean): Result<ContentItem> {
    val baseUrl = sessionManager.getApiBaseUrl()
    return try {
      val session = sessionManager.sessionFlow.first()
        ?: return Result.failure(IllegalStateException("Not logged in"))
      val updated = apiClient.setContentPublishStatus(baseUrl, session.token, id, isPublished)
      dao.insertContent(ContentItemEntity.fromModel(updated))
      Result.success(updated)
    } catch (e: ApiException) {
      Result.failure(e)
    } catch (e: Exception) {
      Result.failure(IOException(formatNetworkError(baseUrl, e), e))
    }
  }

  suspend fun deleteContent(id: String): Result<Unit> {
    val baseUrl = sessionManager.getApiBaseUrl()
    return try {
      val session = sessionManager.sessionFlow.first()
        ?: return Result.failure(IllegalStateException("Not logged in"))
      apiClient.deleteContent(baseUrl, session.token, id)
      dao.deleteContent(id)
      Result.success(Unit)
    } catch (e: ApiException) {
      Result.failure(e)
    } catch (e: Exception) {
      Result.failure(IOException(formatNetworkError(baseUrl, e), e))
    }
  }

  // --- USER REQUESTS ---
  fun getAllRequests(): Flow<List<UserRequest>> {
    return dao.getAllRequests().map { list -> list.map { it.toModel() } }
  }

  fun getRequestsByUser(userId: String): Flow<List<UserRequest>> {
    return dao.getRequestsByUser(userId).map { list -> list.map { it.toModel() } }
  }

  suspend fun submitRequest(
    departmentId: String,
    departmentName: String,
    title: String,
    details: String
  ): Result<UserRequest> {
    var session = sessionManager.sessionFlow.first()
      ?: return Result.failure(IllegalStateException("Please log in first"))
    val baseUrl = sessionManager.getApiBaseUrl()
    val clientReqId = UUID.randomUUID().toString()
    val cleanTitle = title.trim().ifBlank { "General Request" }
    val cleanDetails = details.trim()
    if (cleanDetails.isBlank()) {
      return Result.failure(IllegalArgumentException("Please enter request details"))
    }

    if (session.isOfflineSession || session.token.isBlank()) {
      // Try upgrading session if server is now reachable
      syncNow()
      session = sessionManager.sessionFlow.first() ?: session
    }

    if (session.isOfflineSession || session.token.isBlank()) {
      val now = System.currentTimeMillis()
      val queued = UserRequest(
        id = "local_$clientReqId",
        clientRequestId = clientReqId,
        userId = session.userId,
        userName = session.name,
        userMobile = session.mobile,
        departmentId = departmentId,
        departmentName = departmentName,
        title = cleanTitle,
        details = cleanDetails,
        status = RequestStatus.PENDING,
        adminReply = "",
        createdAt = now,
        updatedAt = now,
        repliedAt = null,
        isPendingSync = true
      )
      dao.insertRequest(UserRequestEntity.fromModel(queued))
      return Result.success(queued)
    }

    return try {
      val created = apiClient.submitRequest(
        baseUrl = baseUrl,
        token = session.token,
        clientRequestId = clientReqId,
        departmentId = departmentId,
        departmentName = departmentName,
        title = cleanTitle,
        details = cleanDetails
      )
      dao.insertRequest(UserRequestEntity.fromModel(created.copy(isPendingSync = false)))
      Result.success(created)
    } catch (e: IOException) {
      // Queue offline if network unavailable (NOT ApiException 4xx)
      if (e is ApiException && e.code in 400..499) {
        return Result.failure(e)
      }
      val now = System.currentTimeMillis()
      val queued = UserRequest(
        id = "local_$clientReqId",
        clientRequestId = clientReqId,
        userId = session.userId,
        userName = session.name,
        userMobile = session.mobile,
        departmentId = departmentId,
        departmentName = departmentName,
        title = cleanTitle,
        details = cleanDetails,
        status = RequestStatus.PENDING,
        adminReply = "",
        createdAt = now,
        updatedAt = now,
        repliedAt = null,
        isPendingSync = true
      )
      dao.insertRequest(UserRequestEntity.fromModel(queued))
      Result.success(queued)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateRequestByAdmin(
    requestId: String,
    status: RequestStatus,
    adminReply: String?
  ): Result<UserRequest> {
    val baseUrl = sessionManager.getApiBaseUrl()
    return try {
      val session = sessionManager.sessionFlow.first()
        ?: return Result.failure(IllegalStateException("Not logged in"))
      val updated = apiClient.updateRequestByAdmin(baseUrl, session.token, requestId, status, adminReply)
      dao.insertRequest(UserRequestEntity.fromModel(updated))
      Result.success(updated)
    } catch (e: ApiException) {
      Result.failure(e)
    } catch (e: Exception) {
      Result.failure(IOException(formatNetworkError(baseUrl, e), e))
    }
  }

  suspend fun deleteRequestByAdmin(requestId: String): Result<Unit> {
    val baseUrl = sessionManager.getApiBaseUrl()
    return try {
      val session = sessionManager.sessionFlow.first()
        ?: return Result.failure(IllegalStateException("Not logged in"))
      apiClient.deleteRequestByAdmin(baseUrl, session.token, requestId)
      dao.deleteRequest(requestId)
      Result.success(Unit)
    } catch (e: ApiException) {
      Result.failure(e)
    } catch (e: Exception) {
      Result.failure(IOException(formatNetworkError(baseUrl, e), e))
    }
  }
}
