package com.example.solarhub.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.solarhub.model.ContentItem
import com.example.solarhub.model.ContentType
import com.example.solarhub.model.Department

@Entity(tableName = "departments")
data class DepartmentEntity(
  @PrimaryKey val id: String,
  val name: String,
  val iconKey: String,
  val orderIndex: Int,
  val createdAt: Long
) {
  fun toModel(): Department = Department(
    id = id,
    name = name,
    iconKey = iconKey,
    orderIndex = orderIndex,
    createdAt = createdAt
  )

  companion object {
    fun fromModel(model: Department): DepartmentEntity = DepartmentEntity(
      id = model.id,
      name = model.name,
      iconKey = model.iconKey,
      orderIndex = model.orderIndex,
      createdAt = model.createdAt
    )
  }
}

@Entity(
  tableName = "content_items",
  indices = [
    Index(value = ["departmentId"]),
    Index(value = ["isPublished"]),
    Index(value = ["publishedAt"])
  ]
)
data class ContentItemEntity(
  @PrimaryKey val id: String,
  val departmentId: String,
  val category: String,
  val subcategory: String,
  val title: String,
  val description: String,
  val contentBody: String,
  val contentType: String,
  val attachmentName: String?,
  val attachmentUrlOrPath: String?,
  val attachmentSizeBytes: Long,
  val attachmentMimeType: String?,
  val isPublished: Boolean,
  val publishedAt: Long,
  val updatedAt: Long,
  val adminAuthor: String
) {
  fun toModel(): ContentItem = ContentItem(
    id = id,
    departmentId = departmentId,
    category = category,
    subcategory = subcategory,
    title = title,
    description = description,
    contentBody = contentBody,
    contentType = try { ContentType.valueOf(contentType) } catch (_: Exception) { ContentType.TEXT },
    attachmentName = attachmentName,
    attachmentUrlOrPath = attachmentUrlOrPath,
    attachmentSizeBytes = attachmentSizeBytes,
    attachmentMimeType = attachmentMimeType,
    isPublished = isPublished,
    publishedAt = publishedAt,
    updatedAt = updatedAt,
    adminAuthor = adminAuthor
  )

  companion object {
    fun fromModel(model: ContentItem): ContentItemEntity = ContentItemEntity(
      id = model.id,
      departmentId = model.departmentId,
      category = model.category,
      subcategory = model.subcategory,
      title = model.title,
      description = model.description,
      contentBody = model.contentBody,
      contentType = model.contentType.name,
      attachmentName = model.attachmentName,
      attachmentUrlOrPath = model.attachmentUrlOrPath,
      attachmentSizeBytes = model.attachmentSizeBytes,
      attachmentMimeType = model.attachmentMimeType,
      isPublished = model.isPublished,
      publishedAt = model.publishedAt,
      updatedAt = model.updatedAt,
      adminAuthor = model.adminAuthor
    )
  }
}

@Entity(tableName = "user_requests")
data class UserRequestEntity(
  @PrimaryKey val id: String,
  val userName: String,
  val userMobile: String,
  val message: String,
  val status: String,
  val adminReply: String,
  val createdAt: Long,
  val updatedAt: Long
)

@Entity(tableName = "app_users")
data class AppUserEntity(
  @PrimaryKey val mobile: String,
  val name: String,
  val isBlocked: Boolean,
  val createdAt: Long,
  val lastLoginAt: Long
)
