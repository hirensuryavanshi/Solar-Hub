package com.example.solarhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solarhub.data.SolarHubRepository
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
}

class SolarHubViewModel(
  private val repository: SolarHubRepository
) : ViewModel() {

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
    if (role == UserRole.EMPLOYEE && (_currentScreen.value is Screen.AdminDashboard || _currentScreen.value is Screen.AdminEditContent || _currentScreen.value is Screen.AdminManageDepartments)) {
      navigateTo(Screen.Home)
    }
  }

  fun verifyAndLoginAdmin(pin: String): Boolean {
    // PIN 1234 or "admin" gives admin access
    if (pin.trim() == "1234" || pin.trim().equals("admin", ignoreCase = true) || pin.trim().equals("solar123", ignoreCase = true)) {
      setUserRole(UserRole.ADMIN)
      return true
    }
    return false
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
    isPublished: Boolean
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
        attachmentSizeBytes = attachmentSizeBytes,
        isPublished = isPublished,
        publishedAt = now,
        updatedAt = now,
        adminAuthor = "Admin"
      )
      repository.saveContent(item)
    }
  }

  fun deleteContent(id: String) {
    viewModelScope.launch {
      repository.deleteContent(id)
    }
  }

  fun setContentPublishStatus(id: String, isPublished: Boolean) {
    viewModelScope.launch {
      repository.setContentPublishStatus(id, isPublished)
    }
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
