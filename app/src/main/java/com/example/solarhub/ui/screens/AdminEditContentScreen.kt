package com.example.solarhub.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import com.example.solarhub.ui.components.AttachmentUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.BorderLight
import com.example.ui.theme.SolarBlue
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
  val departments by viewModel.departments.collectAsState()

  var selectedDeptId by remember { mutableStateOf(initialDepartmentId ?: "") }
  var category by remember { mutableStateOf("") }
  var subcategory by remember { mutableStateOf("") }
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var contentBody by remember { mutableStateOf("") }
  var contentType by remember { mutableStateOf(ContentType.TEXT) }
  var attachmentName by remember { mutableStateOf("") }
  var attachmentSizeBytes by remember { mutableStateOf(1024 * 1024L) } // 1 MB default
  var isPublishedState by remember { mutableStateOf(true) }

  var deptDropdownExpanded by remember { mutableStateOf(false) }

  var attachmentPath by remember { mutableStateOf<String?>(null) }
  var attachmentMime by remember { mutableStateOf<String?>(null) }
  var isCopying by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) {
      scope.launch {
        isCopying = true
        val picked = withContext(Dispatchers.IO) { AttachmentUtils.copyToInternal(context, uri) }
        isCopying = false
        if (picked != null) {
          attachmentPath = picked.path
          attachmentName = picked.name
          attachmentSizeBytes = picked.size
          attachmentMime = picked.mime
          AttachmentUtils.detectType(picked.name, picked.mime)?.let { contentType = it }
          Toast.makeText(context, "File added: ${picked.name}", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(context, "Could not add this file. Try again.", Toast.LENGTH_LONG).show()
        }
      }
    }
  }

  // If editing an existing item, load its data
  if (contentId != null) {
    val existingItem by viewModel.getContentById(contentId).collectAsState(initial = null)
    LaunchedEffect(existingItem) {
      existingItem?.let { item ->
        selectedDeptId = item.departmentId
        category = item.category
        subcategory = item.subcategory
        title = item.title
        description = item.description
        contentBody = item.contentBody
        contentType = item.contentType
        attachmentName = item.attachmentName ?: ""
        attachmentSizeBytes = item.attachmentSizeBytes
        attachmentPath = item.attachmentUrlOrPath
        attachmentMime = item.attachmentMimeType
        isPublishedState = item.isPublished
      }
    }
  } else if (selectedDeptId.isEmpty() && departments.isNotEmpty()) {
    selectedDeptId = departments.first().id
  }

  val selectedDept = departments.find { it.id == selectedDeptId }

  Scaffold(
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
      // 1. Department Selector
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceCard),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "1. Target Department *",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = TextPrimary
            )
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
                  .menuAnchor()
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

      // 2. Content Type Selector
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceCard),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "2. Content Type *",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Chips for content types
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              ContentType.values().forEach { type ->
                val isSelected = contentType == type
                val typeColor = IconHelper.getContentTypeColor(type)

                FilterChip(
                  selected = isSelected,
                  onClick = {
                    contentType = type
                  },
                  label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = IconHelper.getContentTypeIcon(type),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                      )
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
          }
        }
      }

      // 3. Category & Subcategory
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceCard),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "3. Category & Subcategory (Optional)",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                placeholder = { Text("e.g. Work Instructions") },
                modifier = Modifier.weight(1f).testTag("input_category")
              )
              OutlinedTextField(
                value = subcategory,
                onValueChange = { subcategory = it },
                label = { Text("Subcategory") },
                placeholder = { Text("e.g. Lamination") },
                modifier = Modifier.weight(1f).testTag("input_subcategory")
              )
            }
          }
        }
      }

      // 4. Content Title & Description
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceCard),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "4. Information Title & Summary *",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
              value = title,
              onValueChange = { title = it },
              label = { Text("Content Title *") },
              placeholder = { Text("e.g. Standard Operating Procedure for Solar Cell Soldering") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("input_content_title")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
              value = description,
              onValueChange = { description = it },
              label = { Text("Short Description / Summary") },
              placeholder = { Text("Brief overview of this publication...") },
              minLines = 2,
              maxLines = 4,
              modifier = Modifier.fillMaxWidth().testTag("input_content_description")
            )
          }
        }
      }

      // 5. Rich Text Information Body Editor
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceCard),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "5. Rich Text Information & Notes",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Formatting Shortcuts Toolstrip
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.clickable {
                  contentBody += "\n\n### SECTION TITLE\n"
                }
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
                modifier = Modifier.clickable {
                  contentBody += "\n• "
                }
              ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Bullet", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.clickable {
                  contentBody += "\n1. "
                }
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
              placeholder = { Text("Write technical details, specifications, operating parameters, or instructions...") },
              minLines = 6,
              maxLines = 14,
              modifier = Modifier.fillMaxWidth().testTag("input_content_body")
            )
          }
        }
      }

      // 6. Document & Media Attachment Section
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceCard),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "6. File & Media Attachment",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
              )
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFEFF6FF)
              ) {
                Text(
                  text = contentType.displayName,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = SolarBlue,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
              onClick = { filePicker.launch(arrayOf("*/*")) },
              enabled = !isCopying,
              colors = ButtonDefaults.buttonColors(containerColor = SolarBlue),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("button_choose_file")
            ) {
              Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(if (isCopying) "Adding file..." else "Choose File (PDF, Photo, Video, Document)")
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (attachmentName.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEFF6FF),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.AttachFile, contentDescription = null, tint = SolarNavyDark)
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = attachmentName,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 13.sp,
                      color = TextPrimary,
                      maxLines = 2
                    )
                    Text(
                      text = if (attachmentPath != null) AttachmentUtils.formatSize(attachmentSizeBytes) else "No file added - tap Choose File",
                      fontSize = 11.sp,
                      color = TextSecondary
                    )
                  }
                  IconButton(
                    onClick = {
                      attachmentName = ""
                      attachmentPath = null
                      attachmentMime = null
                      Toast.makeText(context, "File removed", Toast.LENGTH_SHORT).show()
                    }
                  ) {
                    Icon(Icons.Default.Clear, contentDescription = "Remove file", tint = TextPrimary)
                  }
                }
              }
            } else {
              Text(
                text = "No file selected. You can also publish text only.",
                fontSize = 12.sp,
                color = TextSecondary
              )
            }
          }
        }
      }

      // 7. Actions: Save as Draft vs Publish Information
      item {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Save Draft button
          OutlinedButton(
            onClick = {
              if (title.isBlank()) {
                Toast.makeText(context, "Please enter a content title", Toast.LENGTH_SHORT).show()
                return@OutlinedButton
              }
              if (selectedDeptId.isBlank()) {
                Toast.makeText(context, "Please select a department", Toast.LENGTH_SHORT).show()
                return@OutlinedButton
              }

              viewModel.saveContent(
                id = contentId,
                departmentId = selectedDeptId,
                category = category,
                subcategory = subcategory,
                title = title,
                description = description,
                contentBody = contentBody,
                contentType = contentType,
                attachmentName = attachmentName,
                attachmentSizeBytes = attachmentSizeBytes,
                isPublished = false,
                attachmentUrlOrPath = attachmentPath,
                attachmentMimeType = attachmentMime
              )
              Toast.makeText(context, "Saved as Draft successfully!", Toast.LENGTH_SHORT).show()
              viewModel.navigateBack()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("button_save_draft")
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Draft")
          }

          // Publish Live button
          Button(
            onClick = {
              if (title.isBlank()) {
                Toast.makeText(context, "Please enter a content title", Toast.LENGTH_SHORT).show()
                return@Button
              }
              if (selectedDeptId.isBlank()) {
                Toast.makeText(context, "Please select a department", Toast.LENGTH_SHORT).show()
                return@Button
              }

              viewModel.saveContent(
                id = contentId,
                departmentId = selectedDeptId,
                category = category,
                subcategory = subcategory,
                title = title,
                description = description,
                contentBody = contentBody,
                contentType = contentType,
                attachmentName = attachmentName,
                attachmentSizeBytes = attachmentSizeBytes,
                isPublished = true,
                attachmentUrlOrPath = attachmentPath,
                attachmentMimeType = attachmentMime
              )
              Toast.makeText(context, "Published Information Live!", Toast.LENGTH_SHORT).show()
              viewModel.navigateBack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1.2f).testTag("button_publish_live")
          ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Publish Information")
          }
        }
      }
    }
  }
}
