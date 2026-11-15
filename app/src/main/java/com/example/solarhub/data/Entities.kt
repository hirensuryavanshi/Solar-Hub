package com.example.solarhub.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.solarhub.model.ContentItem
import com.example.solarhub.model.ContentType
import com.example.solarhub.model.Department
import com.example.solarhub.model.RequestStatus
import com.example.solarhub.model.UserRequest

@Entity(tableName = "departments")
data class DepartmentEntity(
  @PrimaryKey val id: String,
  val name: String,
  val iconKey: String,
  val orderIndex: Int,
  val createdAt: Long,
  @ColumnInfo(defaultValue = "0") val updatedAt: Long = createdAt
) {
  fun toModel(): Department = Department(
    id = id,
    name = name,
    iconKey = iconKey,
    orderIndex = orderIndex,
    createdAt = createdAt,
    updatedAt = updatedAt
  )

  companion object {
    fun fromModel(model: Department): DepartmentEntity = DepartmentEntity(
      id = model.id,
      name = model.name,
      iconKey = model.iconKey,
      orderIndex = model.orderIndex,
      createdAt = model.createdAt,
      updatedAt = model.updatedAt
    )
  }
}

@Entity(
  tableName = "content_items",
  indices = [
    Index(value = ["departmentId"]),
    Index(value = ["isPublished"]),
    Index(value = ["publishedAt"]),
    Index(value = ["isAnnouncement"])
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
  val attachmentFileId: String? = null,
  val attachmentName: String? = null,
  val attachmentUrlOrPath: String? = null,
  val attachmentSizeBytes: Long = 0L,
  val attachmentMimeType: String? = null,
  val isPublished: Boolean = true,
  @ColumnInfo(defaultValue = "0") val isAnnouncement: Boolean = false,
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
    attachmentFileId = attachmentFileId,
    attachmentName = attachmentName,
    attachmentUrlOrPath = attachmentUrlOrPath,
    attachmentSizeBytes = attachmentSizeBytes,
    attachmentMimeType = attachmentMimeType,
    isPublished = isPublished,
    isAnnouncement = isAnnouncement,
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
      attachmentFileId = model.attachmentFileId,
      attachmentName = model.attachmentName,
      attachmentUrlOrPath = model.attachmentUrlOrPath,
      attachmentSizeBytes = model.attachmentSizeBytes,
      attachmentMimeType = model.attachmentMimeType,
      isPublished = model.isPublished,
      isAnnouncement = model.isAnnouncement,
      publishedAt = model.publishedAt,
      updatedAt = model.updatedAt,
      adminAuthor = model.adminAuthor
    )
  }
}

@Entity(
  tableName = "user_requests",
  indices = [
    Index(value = ["userId"]),
    Index(value = ["createdAt"]),
    Index(value = ["isPendingSync"])
  ]
)
data class UserRequestEntity(
  @PrimaryKey val id: String,
  val clientRequestId: String,
  val userId: String,
  val userName: String,
  val userMobile: String,
  val departmentId: String,
  val departmentName: String,
  val title: String,
  val details: String,
  val status: String,
  val adminReply: String,
  val createdAt: Long,
  val updatedAt: Long,
  val repliedAt: Long?,
  val isPendingSync: Boolean
) {
  fun toModel(): UserRequest = UserRequest(
    id = id,
    clientRequestId = clientRequestId,
    userId = userId,
    userName = userName,
    userMobile = userMobile,
    departmentId = departmentId,
    departmentName = departmentName,
    title = title,
    details = details,
    status = try { RequestStatus.valueOf(status) } catch (_: Exception) { RequestStatus.PENDING },
    adminReply = adminReply,
    createdAt = createdAt,
    updatedAt = updatedAt,
    repliedAt = repliedAt,
    isPendingSync = isPendingSync
  )

  companion object {
    fun fromModel(model: UserRequest): UserRequestEntity = UserRequestEntity(
      id = model.id,
      clientRequestId = model.clientRequestId,
      userId = model.userId,
      userName = model.userName,
      userMobile = model.userMobile,
      departmentId = model.departmentId,
      departmentName = model.departmentName,
      title = model.title,
      details = model.details,
      status = model.status.name,
      adminReply = model.adminReply,
      createdAt = model.createdAt,
      updatedAt = model.updatedAt,
      repliedAt = model.repliedAt,
      isPendingSync = model.isPendingSync
    )
  }
}
