package com.example.solarhub.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solarhub.model.ContentType
import com.example.solarhub.ui.components.IconHelper
import com.example.solarhub.viewmodel.SolarHubViewModel
import com.example.ui.theme.BackgroundLightGrey
import com.example.ui.theme.SolarBlue
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditContentScreen(
  contentId: String?,
  initialDepartmentId: String?,
  viewModel: SolarHubViewModel,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.navigateBack()
  }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val departments by viewModel.departments.collectAsState()
  val isBusy by viewModel.isBusy.collectAsState()

  var selectedDeptId by remember { mutableStateOf(initialDepartmentId ?: "") }
  var category by remember { mutableStateOf("") }
  var subcategory by remember { mutableStateOf("") }
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var contentBody by remember { mutableStateOf("") }
  var contentType by remember { mutableStateOf(ContentType.TEXT) }
  var isAnnouncement by remember { mutableStateOf(false) }

  var existingAttachmentFileId by remember { mutableStateOf<String?>(null) }
  var existingAttachmentUrl by remember { mutableStateOf<String?>(null) }
  var attachmentName by remember { mutableStateOf<String?>(null) }
  var attachmentMimeType by remember { mutableStateOf<String?>(null) }
  var attachmentSizeBytes by remember { mutableLongStateOf(0L) }
  var pickedFileBytes by remember { mutableStateOf<ByteArray?>(null) }
  var hasLoadedExisting by remember { mutableStateOf(false) }

  var deptDropdownExpanded by remember { mutableStateOf(false) }

  val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) {
      scope.launch {
        val loaded = withContext(Dispatchers.IO) { readUriBytes(context, uri) }
        if (loaded != null) {
          val (name, mime, bytes) = loaded
          pickedFileBytes = bytes
          attachmentName = name
          attachmentMimeType = mime
          attachmentSizeBytes = bytes.size.toLong()
          contentType = inferContentType(name, mime, contentType)
          Toast.makeText(context, "Attached $name", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(context, "Could not read selected file", Toast.LENGTH_LONG).show()
        }
      }
    }
  }

  if (contentId != null) {
    val existingItem by viewModel.getContentById(contentId).collectAsState(initial = null)
    LaunchedEffect(existingItem) {
      if (!hasLoadedExisting && existingItem != null) {
        val item = existingItem!!
        hasLoadedExisting = true
        selectedDeptId = item.departmentId
        category = item.category
        subcategory = item.subcategory
        title = item.title
        description = item.description
        contentBody = item.contentBody
        contentType = item.contentType
        isAnnouncement = item.isAnnouncement
        existingAttachmentFileId = item.attachmentFileId
        existingAttachmentUrl = item.attachmentUrlOrPath
        attachmentName = item.attachmentName
        attachmentMimeType = item.attachmentMimeType
        attachmentSizeBytes = item.attachmentSizeBytes
      }
    }
  } else if (selectedDeptId.isEmpty() && departments.isNotEmpty()) {
    selectedDeptId = departments.first().id
  }

  val selectedDept = departments.find { it.id == selectedDeptId }

  Scaffold(
    modifier = modifier.navigationBarsPadding(),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (contentId == null) "Publish New Information" else "Edit Information",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
        },
        navigationIcon = {
          IconButton(
            onClick = { viewModel.navigateBack() },
            modifier = Modifier.testTag("admin_edit_back_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SolarNavyDark)
      )
    },
    containerColor = BackgroundLightGrey
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("1. Target Department *", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
              expanded = deptDropdownExpanded,
              onExpandedChange = { deptDropdownExpanded = it }
            ) {
              OutlinedTextField(
                value = selectedDept?.name ?: "Select Department",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptDropdownExpanded) },
                modifier = Modifier
                  .fillMaxWidth()
                  .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                  .testTag("dropdown_department_select")
              )

              ExposedDropdownMenu(
                expanded = deptDropdownExpanded,
                onDismissRequest = { deptDropdownExpanded = false }
              ) {
                departments.forEach { dept ->
                  DropdownMenuItem(
                    text = {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                          imageVector = IconHelper.getDepartmentIcon(dept.iconKey),
                          contentDescription = null,
                          tint = SolarBlue,
                          modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(dept.name)
                      }
                    },
                    onClick = {
                      selectedDeptId = dept.id
                      deptDropdownExpanded = false
                    }
                  )
                }
              }
            }
          }
        }
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("2. Content Type & Announcement *", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              ContentType.entries.forEach { type ->
                val isSelected = contentType == type
                val typeColor = IconHelper.getContentTypeColor(type)

                FilterChip(
                  selected = isSelected,
                  onClick = {
                    contentType = type
                    if (type == ContentType.NOTICE) isAnnouncement = true
                  },
                  label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(imageVector = IconHelper.getContentTypeIcon(type), contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(type.displayName)
                    }
                  },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = typeColor,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                  ),
                  modifier = Modifier.testTag("chip_type_${type.name}")
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("Important Announcement", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                  Text("Show prominently to all registered users", fontSize = 11.sp, color = TextSecondary)
                }
              }
              Switch(checked = isAnnouncement, onCheckedChange = { isAnnouncement = it })
            }
          }
        }
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("3. Information Title, Category & Summary *", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
              value = title,
              onValueChange = { title = it },
              label = { Text("Content Title *") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("input_content_title")
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = subcategory,
                onValueChange = { subcategory = it },
                label = { Text("Subcategory") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
              value = description,
              onValueChange = { description = it },
              label = { Text("Short Description / Summary") },
              minLines = 2,
              maxLines = 4,
              modifier = Modifier.fillMaxWidth().testTag("input_content_description")
            )
          }
        }
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("4. Detailed Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.clickable { contentBody += "\n\n### SECTION TITLE\n" }
              ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.FormatSize, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Heading", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.clickable { contentBody += "\n• " }
              ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Bullet", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.clickable { contentBody += "\n1. " }
              ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Numbered", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF3C7),
                modifier = Modifier.clickable {
                  contentBody += "\n[SAFETY WARNING: Mandatory ESD wrist band and heat protection required.]\n"
                }
              ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFF92400E), modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Safety Warning", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.SemiBold)
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
              value = contentBody,
              onValueChange = { contentBody = it },
              placeholder = { Text("Write technical details, SOP steps, specifications...") },
              minLines = 6,
              maxLines = 14,
              modifier = Modifier.fillMaxWidth().testTag("input_content_body")
            )
          }
        }
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("5. Attachment (Uploaded to Persistent Server Storage)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
              onClick = { filePicker.launch(arrayOf("*/*")) },
              enabled = !isBusy,
              colors = ButtonDefaults.buttonColors(containerColor = SolarBlue),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("button_choose_file")
            ) {
              Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Select PDF / Image / Video / Office File")
            }

            if (!attachmentName.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(10.dp))
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEFF6FF),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Description, contentDescription = null, tint = SolarBlue, modifier = Modifier.size(22.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(attachmentName ?: "", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                    Text("${(attachmentSizeBytes / 1024L).coerceAtLeast(1L)} KB", fontSize = 11.sp, color = TextSecondary)
                  }
                  IconButton(
                    onClick = {
                      pickedFileBytes = null
                      existingAttachmentFileId = null
                      existingAttachmentUrl = null
                      attachmentName = null
                      attachmentMimeType = null
                      attachmentSizeBytes = 0L
                    }
                  ) {
                    Icon(Icons.Default.Clear, contentDescription = "Remove file", tint = TextPrimary)
                  }
                }
              }
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
            onClick = {
              if (title.isBlank() || selectedDeptId.isBlank()) {
                Toast.makeText(context, "Please select department and enter a title", Toast.LENGTH_SHORT).show()
                return@OutlinedButton
              }
              viewModel.saveContentWithAttachment(
                id = contentId,
                departmentId = selectedDeptId,
                category = category,
                subcategory = subcategory,
                title = title,
                description = description,
                contentBody = contentBody,
                contentType = contentType,
                isPublished = false,
                isAnnouncement = isAnnouncement,
                existingAttachmentFileId = existingAttachmentFileId,
                existingAttachmentName = attachmentName,
                existingAttachmentUrlOrPath = existingAttachmentUrl,
                existingAttachmentSizeBytes = attachmentSizeBytes,
                existingAttachmentMimeType = attachmentMimeType,
                pickedFileName = if (pickedFileBytes != null) attachmentName else null,
                pickedFileMime = attachmentMimeType,
                pickedFileBytes = pickedFileBytes
              ) { ok, err ->
                if (ok) {
                  Toast.makeText(context, "Saved as Draft", Toast.LENGTH_SHORT).show()
                  viewModel.navigateBack()
                } else {
                  Toast.makeText(context, err ?: "Failed to save draft", Toast.LENGTH_LONG).show()
                }
              }
            },
            enabled = !isBusy,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("button_save_draft")
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Draft")
          }

          Button(
            onClick = {
              if (title.isBlank() || selectedDeptId.isBlank()) {
                Toast.makeText(context, "Please select department and enter a title", Toast.LENGTH_SHORT).show()
                return@Button
              }
              viewModel.saveContentWithAttachment(
                id = contentId,
                departmentId = selectedDeptId,
                category = category,
                subcategory = subcategory,
                title = title,
                description = description,
                contentBody = contentBody,
                contentType = contentType,
                isPublished = true,
                isAnnouncement = isAnnouncement,
                existingAttachmentFileId = existingAttachmentFileId,
                existingAttachmentName = attachmentName,
                existingAttachmentUrlOrPath = existingAttachmentUrl,
                existingAttachmentSizeBytes = attachmentSizeBytes,
                existingAttachmentMimeType = attachmentMimeType,
                pickedFileName = if (pickedFileBytes != null) attachmentName else null,
                pickedFileMime = attachmentMimeType,
                pickedFileBytes = pickedFileBytes
              ) { ok, err ->
                if (ok) {
                  Toast.makeText(context, "Published Live to All Users!", Toast.LENGTH_SHORT).show()
                  viewModel.navigateBack()
                } else {
                  Toast.makeText(context, err ?: "Publish failed", Toast.LENGTH_LONG).show()
                }
              }
            },
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1.2f).testTag("button_publish_live")
          ) {
            if (isBusy) {
              CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            } else {
              Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Publish Live")
            }
          }
        }
      }
    }
  }
}

private fun readUriBytes(context: Context, uri: Uri): Triple<String, String, ByteArray>? {
  return try {
    var name = "attachment"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (cursor.moveToFirst() && nameIndex >= 0) {
        name = cursor.getString(nameIndex) ?: "attachment"
      }
    }
    val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    Triple(name, mime, bytes)
  } catch (_: Exception) {
    null
  }
}

private fun inferContentType(name: String, mime: String, fallback: ContentType): ContentType {
  val lowerName = name.lowercase()
  val lowerMime = mime.lowercase()
  return when {
    lowerMime.contains("pdf") || lowerName.endsWith(".pdf") -> ContentType.PDF
    lowerMime.startsWith("image/") || lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") -> ContentType.IMAGE
    lowerMime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") -> ContentType.VIDEO
    lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".csv") -> ContentType.EXCEL
    lowerName.endsWith(".doc") || lowerName.endsWith(".docx") -> ContentType.WORD
    lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx") -> ContentType.PPT
    else -> fallback
  }
}
