package com.example.solarhub.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solarhub.model.ContentItem
import com.example.solarhub.model.UserRole
import com.example.solarhub.ui.components.IconHelper
import com.example.solarhub.viewmodel.Screen
import com.example.solarhub.viewmodel.SolarHubViewModel
import com.example.ui.theme.BackgroundLightGrey
import com.example.ui.theme.BorderLight
import com.example.ui.theme.SolarBlue
import com.example.ui.theme.SolarBlueLight
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentScreen(
  departmentId: String,
  viewModel: SolarHubViewModel,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.navigateBack()
  }

  val department by viewModel.getDepartment(departmentId).collectAsState(initial = null)
  val userRole by viewModel.userRole.collectAsState()

  // In Admin mode, show both published and drafts; for employee show only published
  val contentList by if (userRole == UserRole.ADMIN) {
    viewModel.getAllContentByDepartment(departmentId).collectAsState(initial = emptyList())
  } else {
    viewModel.getPublishedContentByDepartment(departmentId).collectAsState(initial = emptyList())
  }

  var selectedCategory by remember { mutableStateOf<String?>(null) }

  // Extract unique categories created by Admin for this department
  val categories = remember(contentList) {
    contentList.map { it.category.trim() }.filter { it.isNotBlank() }.distinct()
  }

  val filteredContent = remember(contentList, selectedCategory) {
    if (selectedCategory == null) {
      contentList
    } else {
      contentList.filter { it.category.trim().equals(selectedCategory, ignoreCase = true) }
    }
  }

  val currentDept = department

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          if (currentDept != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(SolarBlueLight),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = IconHelper.getDepartmentIcon(currentDept.iconKey),
                  contentDescription = null,
                  tint = SolarBlue,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = currentDept.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
              )
            }
          } else {
            Text("Department", color = Color.White)
          }
        },
        navigationIcon = {
          IconButton(
            onClick = { viewModel.navigateBack() },
            modifier = Modifier.testTag("dept_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White
            )
          }
        },
        actions = {
          if (userRole == UserRole.ADMIN && currentDept != null) {
            Button(
              onClick = {
                viewModel.navigateTo(Screen.AdminEditContent(initialDepartmentId = currentDept.id))
              },
              colors = ButtonDefaults.buttonColors(containerColor = SolarBlue),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.padding(end = 8.dp).testTag("dept_add_content_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add", fontSize = 12.sp)
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = SolarNavyDark
        )
      )
    },
    containerColor = BackgroundLightGrey
  ) { innerPadding ->
    if (currentDept == null) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        Text("Department not found", color = TextSecondary)
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Department Header Info
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(CircleShape)
                  .background(SolarBlueLight),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = IconHelper.getDepartmentIcon(currentDept.iconKey),
                  contentDescription = null,
                  tint = SolarBlue,
                  modifier = Modifier.size(30.dp)
                )
              }
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(
                  text = currentDept.name,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "Official knowledge repository & technical resources",
                  fontSize = 12.sp,
                  color = TextSecondary
                )
              }
            }
          }
        }

        // Category Filter Chips (if any created by Admin)
        if (categories.isNotEmpty()) {
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All (${contentList.size})") },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SolarNavyDark,
                  selectedLabelColor = Color.White
                )
              )
              categories.forEach { cat ->
                val count = contentList.count { it.category.trim().equals(cat, ignoreCase = true) }
                FilterChip(
                  selected = selectedCategory == cat,
                  onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                  label = { Text("$cat ($count)") },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SolarNavyDark,
                    selectedLabelColor = Color.White
                  )
                )
              }
            }
          }
        }

        // Empty state requirement: "If the Admin has not uploaded anything, show: 'No information available yet.'"
        if (filteredContent.isEmpty()) {
          item {
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .testTag("empty_department_content_card"),
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = SurfaceCard),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Box(
                  modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.FolderOff,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(32.dp)
                  )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                  text = "No information available yet.",
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                  text = "Technical SOPs, guidelines, and documents will appear here once published by the department administrator.",
                  fontSize = 13.sp,
                  color = TextSecondary,
                  textAlign = TextAlign.Center,
                  lineHeight = 18.sp
                )

                if (userRole == UserRole.ADMIN) {
                  Spacer(modifier = Modifier.height(20.dp))
                  Button(
                    onClick = {
                      viewModel.navigateTo(Screen.AdminEditContent(initialDepartmentId = currentDept.id))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("button_publish_first_content")
                  ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Publish First Information")
                  }
                }
              }
            }
          }
        } else {
          // List of published information uploaded by Admin
          items(filteredContent, key = { it.id }) { item ->
            DepartmentContentItemCard(
              item = item,
              userRole = userRole,
              onClick = { viewModel.navigateTo(Screen.ContentDetail(item.id)) },
              onEdit = { viewModel.navigateTo(Screen.AdminEditContent(contentId = item.id)) },
              onTogglePublish = { viewModel.setContentPublishStatus(item.id, !item.isPublished) },
              onDelete = { viewModel.deleteContent(item.id) }
            )
          }
        }
      }
    }
  }
}

@Composable
fun DepartmentContentItemCard(
  item: ContentItem,
  userRole: UserRole,
  onClick: () -> Unit,
  onEdit: () -> Unit,
  onTogglePublish: () -> Unit,
  onDelete: () -> Unit
) {
  val typeColor = IconHelper.getContentTypeColor(item.contentType)
  val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
  var menuExpanded by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("dept_content_card_${item.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(typeColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = IconHelper.getContentTypeIcon(item.contentType),
              contentDescription = null,
              tint = typeColor,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (!item.isPublished) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = Color(0xFFFEF3C7),
                  modifier = Modifier.padding(end = 6.dp)
                ) {
                  Text(
                    text = "DRAFT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                  )
                }
              }

              if (item.category.isNotBlank()) {
                Text(
                  text = item.category.uppercase(),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = SolarBlue
                )
              }
            }

            Text(
              text = item.title,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        if (userRole == UserRole.ADMIN) {
          Box {
            IconButton(
              onClick = { menuExpanded = true },
              modifier = Modifier.size(32.dp).testTag("menu_button_${item.id}")
            ) {
              Icon(Icons.Default.MoreVert, contentDescription = "Actions", tint = TextSecondary)
            }
            DropdownMenu(
              expanded = menuExpanded,
              onDismissRequest = { menuExpanded = false }
            ) {
              DropdownMenuItem(
                text = { Text("Edit Content") },
                onClick = {
                  menuExpanded = false
                  onEdit()
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
              )
              DropdownMenuItem(
                text = { Text(if (item.isPublished) "Unpublish (Make Draft)" else "Publish") },
                onClick = {
                  menuExpanded = false
                  onTogglePublish()
                },
                leadingIcon = {
                  Icon(
                    if (item.isPublished) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null
                  )
                }
              )
              DropdownMenuItem(
                text = { Text("Delete", color = Color.Red) },
                onClick = {
                  menuExpanded = false
                  onDelete()
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
              )
            }
          }
        }
      }

      if (item.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = item.description,
          fontSize = 13.sp,
          color = TextSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 18.sp
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = item.contentType.displayName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = typeColor
          )
          Text(
            text = " • ",
            fontSize = 11.sp,
            color = TextMuted
          )
          Text(
            text = dateFormat.format(Date(item.publishedAt)),
            fontSize = 11.sp,
            color = TextMuted
          )
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Color(0xFFF1F5F9)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Open",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = SolarNavyDark
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              tint = SolarNavyDark,
              modifier = Modifier.size(11.dp)
            )
          }
        }
      }
    }
  }
}
