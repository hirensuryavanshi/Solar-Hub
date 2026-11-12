package com.example.solarhub.model

enum class UserRole {
  ADMIN,
  EMPLOYEE
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

data class Department(
  val id: String,
  val name: String,
  val iconKey: String,
  val orderIndex: Int = 0,
  val createdAt: Long = System.currentTimeMillis()
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
  val attachmentName: String? = null,
  val attachmentUrlOrPath: String? = null,
  val attachmentSizeBytes: Long = 0L,
  val attachmentMimeType: String? = null,
  val isPublished: Boolean = true,
  val publishedAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val adminAuthor: String = "Admin"
)

data class ContentItemWithDepartment(
  val content: ContentItem,
  val departmentName: String,
  val departmentIconKey: String
)
