package com.example.solarhub.model

enum class UserRole {
  ADMIN,
  EMPLOYEE
}

enum class RequestStatus(val label: String) {
  PENDING("Pending"),
  ACCEPTED("Accepted"),
  REJECTED("Rejected"),
  COMPLETED("Completed")
}

enum class SyncConnectionState(val label: String) {
  DISCONNECTED("Disconnected"),
  CONNECTING("Connecting..."),
  SYNCING("Syncing..."),
  LIVE("Live Sync"),
  OFFLINE("Offline (Cached)"),
  ERROR("Sync Error")
}

enum class ContentType(
  val displayName: String,
  val extensionHint: String
) {
  TEXT("Text Information", "txt"),
  PDF("PDF Document", "pdf"),
  WORD("Word Document", "docx"),
  EXCEL("Excel Spreadsheet", "xlsx"),
  PPT("PowerPoint Presentation", "pptx"),
  IMAGE("Image / Photograph", "png"),
  VIDEO("Video Recording", "mp4"),
  TRAINING("Training Material", "pdf"),
  NOTICE("Notice & Announcement", "notice"),
  OTHER("Technical Document", "doc")
}

enum class SolarCalculatorMode {
  MW_TO_QUANTITY,
  QUANTITY_TO_MW
}

data class UserSession(
  val userId: String,
  val name: String,
  val mobile: String,
  val role: UserRole,
  val token: String,
  val isOfflineSession: Boolean = false
)

data class Department(
  val id: String,
  val name: String,
  val iconKey: String,
  val orderIndex: Int = 0,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

data class ContentItem(
  val id: String,
  val departmentId: String,
  val category: String = "",
  val subcategory: String = "",
  val title: String,
  val description: String,
  val contentBody: String = "",
  val contentType: ContentType = ContentType.TEXT,
  val attachmentFileId: String? = null,
  val attachmentName: String? = null,
  val attachmentUrlOrPath: String? = null,
  val attachmentSizeBytes: Long = 0L,
  val attachmentMimeType: String? = null,
  val isPublished: Boolean = true,
  val isAnnouncement: Boolean = false,
  val publishedAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val adminAuthor: String = "Admin"
)

data class ContentItemWithDepartment(
  val content: ContentItem,
  val departmentName: String,
  val departmentIconKey: String
)

data class UserRequest(
  val id: String,
  val clientRequestId: String = "",
  val userId: String,
  val userName: String,
  val userMobile: String,
  val departmentId: String = "",
  val departmentName: String = "",
  val title: String,
  val details: String,
  val status: RequestStatus = RequestStatus.PENDING,
  val adminReply: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val repliedAt: Long? = null,
  val isPendingSync: Boolean = false
)
