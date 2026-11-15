package com.example.solarhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solarhub.data.CloudSync
import com.example.solarhub.data.AppUserEntity
import com.example.solarhub.data.SolarHubRepository
import com.example.solarhub.data.UserRequestEntity
import com.example.solarhub.model.ContentItem
import com.example.solarhub.model.ContentItemWithDepartment
import com.example.solarhub.model.ContentType
import com.example.solarhub.model.Department
import com.example.solarhub.model.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

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
  data object UserRequests : Screen
  data object AdminUsers : Screen
  data object Notifications : Screen
}

class SolarHubViewModel(
  private val repository: SolarHubRepository,
  private val cloud: CloudSync
) : ViewModel() {

  // Admin password is kept in memory only while the admin is logged in (the server checks it again)
  private var adminPassword: String = ""

  private val _syncMessage = MutableStateFlow<String?>(null)
  val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

  fun clearSyncMessage() {
    _syncMessage.value = null
  }

  private var syncing = false

  /** Downloads new posts / requests, uploads admin changes. Called every few seconds. */
  fun syncNow(name: String, mobile: String) {
    if (syncing) return
    syncing = true
    viewModelScope.launch {
      try {
        val blocked = cloud.registerUser(name, mobile)
        if (blocked != null) repository.registerLogin(name, mobile, blocked)
        cloud.pullContent()
        val pw = adminPassword
        if (pw.isNotEmpty()) {
          cloud.pullAdminData(pw)
          val pushed = cloud.pushUnsynced(pw)
          if (pushed > 0) _syncMessage.value = "$pushed old post(s) published for all users"
        } else {
          cloud.pullMyRequests(mobile)
        }
      } finally {
        syncing = false
      }
    }
  }

  private fun online(action: suspend () -> Boolean) {
    viewModelScope.launch {
      if (!action()) {
        _syncMessage.value = "No internet. This change is saved only on this phone."
      }
    }
  }

  private val screenBackStack = mutableListOf<Screen>(Screen.Home)
  private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
  val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

  private val _userRole = MutableStateFlow(UserRole.EMPLOYEE)
  val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  val departments: StateFlow<List<Department>> = repository.departments
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val latestUpdates: StateFlow<List<ContentItemWithDepartment>> = repository.getLatestPublishedContent(10)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allDrafts: StateFlow<List<ContentItemWithDepartment>> = repository.getAllDrafts()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allPublished: StateFlow<List<ContentItemWithDepartment>> = repository.getAllPublished()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allRequests: StateFlow<List<UserRequestEntity>> = repository.allRequests
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allUsers: StateFlow<List<AppUserEntity>> = repository.allUsers
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun observeUser(mobile: String) = repository.observeUser(mobile)

  fun registerLogin(name: String, mobile: String, onResult: (blocked: Boolean) -> Unit) {
    viewModelScope.launch {
      val cloudBlocked = cloud.registerUser(name, mobile)
      onResult(repository.registerLogin(name, mobile, cloudBlocked))
    }
  }

  fun setUserBlocked(mobile: String, blocked: Boolean) {
    viewModelScope.launch { repository.setUserBlocked(mobile, blocked) }
    val pw = adminPassword
    online { cloud.adminSetBlocked(pw, mobile, blocked) }
  }

  fun deleteUser(mobile: String) {
    viewModelScope.launch { repository.deleteUser(mobile) }
    val pw = adminPassword
    online { cloud.adminDeleteUser(pw, mobile) }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  val searchResults: StateFlow<List<ContentItemWithDepartment>> = _searchQuery
    .flatMapLatest { query ->
      if (query.isBlank()) {
        flowOf(emptyList())
      } else {
        repository.searchPublishedContent(query.trim())
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  init {
    viewModelScope.launch {
      repository.ensureInitialData()
    }
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

  fun setUserRole(role: UserRole) {
    _userRole.value = role
    if (role == UserRole.EMPLOYEE) adminPassword = ""
    if (role == UserRole.EMPLOYEE && (_currentScreen.value is Screen.AdminDashboard || _currentScreen.value is Screen.AdminEditContent || _currentScreen.value is Screen.AdminManageDepartments || _currentScreen.value is Screen.AdminUsers)) {
      navigateTo(Screen.Home)
    }
  }

  fun verifyAndLoginAdmin(password: String): Boolean {
    // The password itself is not stored in the app - only a one-way hash of it.
    if (hashPassword(password.trim()) == ADMIN_PASSWORD_HASH) {
      adminPassword = password.trim()
      setUserRole(UserRole.ADMIN)
      val pw = adminPassword
      viewModelScope.launch {
        cloud.pullAdminData(pw)
        cloud.pushUnsynced(pw)
      }
      return true
    }
    return false
  }

  private fun hashPassword(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256")
      .digest(("SolarHub|" + value).toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  private companion object {
    const val ADMIN_PASSWORD_HASH = "721ac2ecaa478601656240f046b48f8b6be87358467ea0e4b4dee292e645b0b8"
  }

  fun getDepartment(id: String) = repository.getDepartmentById(id)

  fun getPublishedContentByDepartment(deptId: String) =
    repository.getPublishedContentByDepartment(deptId)

  fun getAllContentByDepartment(deptId: String) =
    repository.getAllContentByDepartment(deptId)

  fun getContentById(id: String) = repository.getContentById(id)

  fun saveContent(
    id: String?,
    departmentId: String,
    category: String,
    subcategory: String,
    title: String,
    description: String,
    contentBody: String,
    contentType: ContentType,
    attachmentName: String?,
    attachmentSizeBytes: Long,
    isPublished: Boolean,
    attachmentUrlOrPath: String? = null,
    attachmentMimeType: String? = null
  ) {
    viewModelScope.launch {
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
        attachmentName = attachmentName?.ifBlank { null },
        attachmentUrlOrPath = attachmentUrlOrPath,
        attachmentSizeBytes = attachmentSizeBytes,
        attachmentMimeType = attachmentMimeType,
        isPublished = isPublished,
        publishedAt = now,
        updatedAt = now,
        adminAuthor = "Admin"
      )
      repository.saveContent(item)
      pushToCloud(item)
    }
  }

  private suspend fun pushToCloud(item: ContentItem) {
    val pw = adminPassword
    if (pw.isEmpty()) return
    if (!item.isPublished && !cloud.isKnown(item.id)) return
    if (item.isPublished) _syncMessage.value = "Uploading for all users... please wait"
    val ok = cloud.pushContent(item, pw)
    _syncMessage.value = if (ok) {
      if (item.isPublished) "Published for all users" else "Updated online"
    } else {
      "Upload failed (check internet). It will retry automatically."
    }
  }

  fun deleteContent(id: String) {
    viewModelScope.launch {
      repository.deleteContent(id)
    }
    val pw = adminPassword
    if (pw.isNotEmpty()) online { cloud.deleteContent(id, pw) }
  }

  fun setContentPublishStatus(id: String, isPublished: Boolean) {
    viewModelScope.launch {
      repository.setContentPublishStatus(id, isPublished)
      repository.getContentById(id).first()?.let { pushToCloud(it) }
    }
  }

  fun submitRequest(name: String, mobile: String, message: String) {
    val id = UUID.randomUUID().toString()
    viewModelScope.launch {
      repository.submitRequest(name, mobile, message, id)
      if (!cloud.submitRequest(id, name.trim(), mobile.trim(), message.trim())) {
        _syncMessage.value = "No internet. Your request will be sent automatically later."
      }
    }
  }

  fun updateRequest(id: String, status: String, reply: String) {
    viewModelScope.launch {
      repository.updateRequest(id, status, reply)
    }
    val pw = adminPassword
    online { cloud.adminUpdateRequest(pw, id, status, reply.trim()) }
  }

  fun deleteRequest(id: String) {
    viewModelScope.launch {
      repository.deleteRequest(id)
    }
    val pw = adminPassword
    online { cloud.adminDeleteRequest(pw, id) }
  }

  fun addDepartment(name: String, iconKey: String) {
    viewModelScope.launch {
      repository.addDepartment(name, iconKey)
    }
  }

  fun updateDepartment(department: Department) {
    viewModelScope.launch {
      repository.updateDepartment(department)
    }
  }

  fun deleteDepartment(id: String) {
    viewModelScope.launch {
      repository.deleteDepartment(id)
    }
  }
}

class SolarHubViewModelFactory(
  private val repository: SolarHubRepository,
  private val cloud: CloudSync
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(SolarHubViewModel::class.java)) {
      return SolarHubViewModel(repository, cloud) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
