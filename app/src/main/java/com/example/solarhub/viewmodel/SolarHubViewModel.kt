package com.example.solarhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solarhub.data.SolarHubRepository
import com.example.solarhub.model.ContentItem
import com.example.solarhub.model.ContentItemWithDepartment
import com.example.solarhub.model.ContentType
import com.example.solarhub.model.Department
import com.example.solarhub.model.RequestStatus
import com.example.solarhub.model.SyncConnectionState
import com.example.solarhub.model.UserRequest
import com.example.solarhub.model.UserRole
import com.example.solarhub.model.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed interface SessionUiState {
  data object Loading : SessionUiState
  data object Unauthenticated : SessionUiState
  data class Authenticated(val session: UserSession) : SessionUiState
}

sealed interface Screen {
  data object Home : Screen
  data class DepartmentDetail(val departmentId: String) : Screen
  data class ContentDetail(val contentId: String) : Screen
  data object AdminDashboard : Screen
  data class AdminEditContent(
    val contentId: String? = null,
    val initialDepartmentId: String? = null
  ) : Screen
  data object AdminManageDepartments : Screen
}

class SolarHubViewModel(
  private val repository: SolarHubRepository
) : ViewModel() {

  private val screenBackStack = mutableListOf<Screen>(Screen.Home)
  private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
  val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _toastMessage = MutableStateFlow<String?>(null)
  val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  val sessionState: StateFlow<SessionUiState> = repository.sessionFlow
    .map { s -> if (s == null) SessionUiState.Unauthenticated else SessionUiState.Authenticated(s) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionUiState.Loading)

  val session: StateFlow<UserSession?> = repository.sessionFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val userRole: StateFlow<UserRole> = repository.sessionFlow
    .map { it?.role ?: UserRole.EMPLOYEE }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserRole.EMPLOYEE)

  val syncState: StateFlow<SyncConnectionState> = repository.syncStateFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncConnectionState.DISCONNECTED)

  val lastSyncError: StateFlow<String?> = repository.lastSyncError
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val apiBaseUrl: StateFlow<String> = repository.apiBaseUrlFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

  val departments: StateFlow<List<Department>> = repository.departments
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val latestUpdates: StateFlow<List<ContentItemWithDepartment>> = repository.getLatestPublishedContent(20)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val announcements: StateFlow<List<ContentItemWithDepartment>> = repository.getPublishedAnnouncements(10)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allDrafts: StateFlow<List<ContentItemWithDepartment>> = repository.getAllDrafts()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allPublished: StateFlow<List<ContentItemWithDepartment>> = repository.getAllPublished()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allRequests: StateFlow<List<UserRequest>> = repository.getAllRequests()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val myRequests: StateFlow<List<UserRequest>> = repository.sessionFlow
    .flatMapLatest { s ->
      if (s == null) flowOf(emptyList()) else repository.getRequestsByUser(s.userId)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val searchResults: StateFlow<List<ContentItemWithDepartment>> = _searchQuery
    .flatMapLatest { query ->
      if (query.isBlank()) flowOf(emptyList()) else repository.searchPublishedContent(query.trim())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  init {
    viewModelScope.launch {
      repository.ensureInitialData()
    }
  }

  fun clearToast() {
    _toastMessage.value = null
  }

  fun showToast(message: String) {
    _toastMessage.value = message
  }

  fun navigateTo(screen: Screen) {
    if (_currentScreen.value != screen) {
      screenBackStack.add(screen)
      _currentScreen.value = screen
    }
  }

  fun navigateBack(): Boolean {
    if (screenBackStack.size > 1) {
      screenBackStack.removeAt(screenBackStack.lastIndex)
      _currentScreen.value = screenBackStack.last()
      return true
    }
    return false
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun testServerConnection(url: String, onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val res = repository.testServerConnection(url)
      if (res.isSuccess) {
        val health = res.getOrThrow()
        onResult(true, "Connected to SOLAR HUB server (${health.storageMode}).")
      } else {
        onResult(false, res.exceptionOrNull()?.message ?: "Unable to connect to server.")
      }
    }
  }

  fun loginEmployee(
    name: String,
    mobile: String,
    serverUrlOverride: String? = null,
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.loginEmployee(name, mobile, serverUrlOverride)
      _isBusy.value = false
      if (res.isSuccess) {
        val user = res.getOrThrow()
        if (user.isOfflineSession) {
          _toastMessage.value = "Signed in (Offline Mode). Connect server URL in Account settings for live sync."
        }
        onResult(true, null)
      } else {
        onResult(false, res.exceptionOrNull()?.message ?: "Sign in failed.")
      }
    }
  }

  fun loginAdmin(
    password: String,
    serverUrlOverride: String? = null,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.loginAdmin(password, serverUrlOverride)
      _isBusy.value = false
      if (res.isSuccess) {
        _toastMessage.value = "Admin Access Granted (Live Sync Active)"
        onResult(true, null)
      } else {
        onResult(false, res.exceptionOrNull()?.message ?: "Invalid admin password.")
      }
    }
  }

  fun logout() {
    viewModelScope.launch {
      screenBackStack.clear()
      screenBackStack.add(Screen.Home)
      _currentScreen.value = Screen.Home
      repository.logout()
    }
  }

  fun updateServerUrl(url: String) {
    viewModelScope.launch {
      repository.setApiBaseUrl(url)
      val res = repository.syncNow()
      _toastMessage.value = if (res.isSuccess) {
        "Connected & synced with server."
      } else {
        res.exceptionOrNull()?.message ?: "Server URL saved; sync will retry automatically."
      }
    }
  }

  fun triggerSync() {
    viewModelScope.launch {
      val res = repository.syncNow()
      if (res.isFailure) {
        _toastMessage.value = res.exceptionOrNull()?.message ?: "Server unreachable. Using cached data."
      } else {
        _toastMessage.value = "Synced with centralized database."
      }
    }
  }

  fun getDepartment(id: String) = repository.getDepartmentById(id)

  fun getPublishedContentByDepartment(deptId: String) =
    repository.getPublishedContentByDepartment(deptId)

  fun getAllContentByDepartment(deptId: String) =
    repository.getAllContentByDepartment(deptId)

  fun getContentById(id: String) = repository.getContentById(id)

  fun saveContentWithAttachment(
    id: String?,
    departmentId: String,
    category: String,
    subcategory: String,
    title: String,
    description: String,
    contentBody: String,
    contentType: ContentType,
    isPublished: Boolean,
    isAnnouncement: Boolean,
    existingAttachmentFileId: String?,
    existingAttachmentName: String?,
    existingAttachmentUrlOrPath: String?,
    existingAttachmentSizeBytes: Long,
    existingAttachmentMimeType: String?,
    pickedFileName: String?,
    pickedFileMime: String?,
    pickedFileBytes: ByteArray?,
    onComplete: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      var fileId = existingAttachmentFileId
      var fileName = existingAttachmentName
      var fileUrl = existingAttachmentUrlOrPath
      var fileSize = existingAttachmentSizeBytes
      var fileMime = existingAttachmentMimeType

      if (pickedFileBytes != null && !pickedFileName.isNullOrBlank()) {
        val uploadRes = repository.uploadFile(
          fileName = pickedFileName,
          mimeType = pickedFileMime ?: "application/octet-stream",
          bytes = pickedFileBytes
        )
        if (uploadRes.isFailure) {
          _isBusy.value = false
          val err = uploadRes.exceptionOrNull()?.message ?: "File upload failed."
          onComplete(false, err)
          return@launch
        }
        val uploaded = uploadRes.getOrThrow()
        fileId = uploaded.id
        fileName = uploaded.originalName
        fileUrl = uploaded.url
        fileSize = uploaded.sizeBytes
        fileMime = uploaded.mimeType
      }

      val now = System.currentTimeMillis()
      val item = ContentItem(
        id = id ?: UUID.randomUUID().toString(),
        departmentId = departmentId,
        category = category.trim(),
        subcategory = subcategory.trim(),
        title = title.trim(),
        description = description.trim(),
        contentBody = contentBody.trim(),
        contentType = contentType,
        attachmentFileId = fileId,
        attachmentName = fileName?.ifBlank { null },
        attachmentUrlOrPath = fileUrl,
        attachmentSizeBytes = fileSize,
        attachmentMimeType = fileMime,
        isPublished = isPublished,
        isAnnouncement = isAnnouncement,
        publishedAt = now,
        updatedAt = now,
        adminAuthor = session.value?.name ?: "Admin"
      )
      val saveRes = repository.saveContent(item)
      _isBusy.value = false
      if (saveRes.isSuccess) {
        onComplete(true, null)
      } else {
        onComplete(false, saveRes.exceptionOrNull()?.message ?: "Failed to publish content.")
      }
    }
  }

  fun deleteContent(id: String) {
    viewModelScope.launch {
      val res = repository.deleteContent(id)
      if (res.isFailure) {
        _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to delete content."
      }
    }
  }

  fun setContentPublishStatus(id: String, isPublished: Boolean) {
    viewModelScope.launch {
      val res = repository.setContentPublishStatus(id, isPublished)
      if (res.isFailure) {
        _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to update publish status."
      }
    }
  }

  fun downloadAttachment(content: ContentItem, destFile: File, onComplete: (Result<File>) -> Unit) {
    viewModelScope.launch {
      val res = repository.downloadAttachmentToFile(content, destFile)
      onComplete(res)
    }
  }

  fun submitRequest(
    departmentId: String,
    departmentName: String,
    title: String,
    details: String,
    onComplete: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.submitRequest(departmentId, departmentName, title, details)
      _isBusy.value = false
      if (res.isSuccess) {
        val req = res.getOrThrow()
        _toastMessage.value = if (req.isPendingSync) {
          "Request saved offline — will sync automatically when server connects."
        } else {
          "Request submitted live to Admin."
        }
        onComplete(true, null)
      } else {
        val msg = res.exceptionOrNull()?.message ?: "Failed to submit request."
        _toastMessage.value = msg
        onComplete(false, msg)
      }
    }
  }

  fun updateRequestByAdmin(requestId: String, status: RequestStatus, adminReply: String?) {
    viewModelScope.launch {
      val res = repository.updateRequestByAdmin(requestId, status, adminReply)
      _toastMessage.value = if (res.isSuccess) {
        "Request marked ${status.label}."
      } else {
        res.exceptionOrNull()?.message ?: "Failed to update request."
      }
    }
  }

  fun deleteRequestByAdmin(requestId: String) {
    viewModelScope.launch {
      val res = repository.deleteRequestByAdmin(requestId)
      if (res.isFailure) {
        _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to delete request."
      }
    }
  }

  fun addDepartment(name: String, iconKey: String) {
    viewModelScope.launch {
      val res = repository.addDepartment(name, iconKey)
      if (res.isFailure) {
        _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to add department."
      }
    }
  }

  fun updateDepartment(department: Department) {
    viewModelScope.launch {
      val res = repository.updateDepartment(department)
      if (res.isFailure) {
        _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to update department."
      }
    }
  }

  fun deleteDepartment(id: String) {
    viewModelScope.launch {
      val res = repository.deleteDepartment(id)
      if (res.isFailure) {
        _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to delete department."
      }
    }
  }
}

class SolarHubViewModelFactory(
  private val repository: SolarHubRepository
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(SolarHubViewModel::class.java)) {
      return SolarHubViewModel(repository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
