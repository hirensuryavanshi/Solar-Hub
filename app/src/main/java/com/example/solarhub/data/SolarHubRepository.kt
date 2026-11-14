package com.example.solarhub.data

import com.example.solarhub.model.ContentItem
import com.example.solarhub.model.ContentItemWithDepartment
import com.example.solarhub.model.Department
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class SolarHubRepository(private val dao: SolarHubDao) {

  val departments: Flow<List<Department>> = dao.getAllDepartments().map { list ->
    list.map { it.toModel() }
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

  fun getDepartmentById(id: String): Flow<Department?> {
    return dao.getDepartmentById(id).map { it?.toModel() }
  }

  suspend fun addDepartment(name: String, iconKey: String): String {
    val id = "dept_" + UUID.randomUUID().toString().take(8)
    val existing = departments.first()
    val order = (existing.maxOfOrNull { it.orderIndex } ?: 0) + 1
    dao.insertDepartment(
      DepartmentEntity(
        id = id,
        name = name.trim(),
        iconKey = iconKey,
        orderIndex = order,
        createdAt = System.currentTimeMillis()
      )
    )
    return id
  }

  suspend fun updateDepartment(dept: Department) {
    dao.updateDepartment(DepartmentEntity.fromModel(dept))
  }

  suspend fun deleteDepartment(id: String) {
    dao.deleteContentByDepartment(id)
    dao.deleteDepartment(id)
  }

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

  fun getLatestPublishedContent(limit: Int = 10): Flow<List<ContentItemWithDepartment>> {
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

  suspend fun saveContent(item: ContentItem) {
    dao.insertContent(ContentItemEntity.fromModel(item))
  }

  suspend fun updateContent(item: ContentItem) {
    dao.updateContent(ContentItemEntity.fromModel(item))
  }

  suspend fun deleteContent(id: String) {
    dao.deleteContent(id)
  }

  suspend fun setContentPublishStatus(id: String, isPublished: Boolean) {
    val existing = getContentById(id).first() ?: return
    val updated = existing.copy(
      isPublished = isPublished,
      publishedAt = if (isPublished) System.currentTimeMillis() else existing.publishedAt,
      updatedAt = System.currentTimeMillis()
    )
    dao.updateContent(ContentItemEntity.fromModel(updated))
  }

  val allRequests: Flow<List<UserRequestEntity>> = dao.getAllRequests()

  suspend fun submitRequest(name: String, mobile: String, message: String) {
    val now = System.currentTimeMillis()
    dao.insertRequest(
      UserRequestEntity(
        id = UUID.randomUUID().toString(),
        userName = name.trim(),
        userMobile = mobile.trim(),
        message = message.trim(),
        status = "PENDING",
        adminReply = "",
        createdAt = now,
        updatedAt = now
      )
    )
  }

  suspend fun updateRequest(id: String, status: String, reply: String) {
    dao.updateRequest(id, status, reply.trim(), System.currentTimeMillis())
  }

  suspend fun deleteRequest(id: String) {
    dao.deleteRequest(id)
  }
}
