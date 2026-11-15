package com.example.solarhub.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solarhub.model.ContentItemWithDepartment
import com.example.solarhub.model.UserRole
import com.example.solarhub.ui.components.IconHelper
import com.example.solarhub.viewmodel.Screen
import com.example.solarhub.viewmodel.SolarHubViewModel
import com.example.ui.theme.BackgroundLightGrey
import com.example.ui.theme.SolarBlue
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
fun AdminDashboardScreen(
  viewModel: SolarHubViewModel,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.navigateBack()
  }

  val publishedItems by viewModel.allPublished.collectAsState()
  val drafts by viewModel.allDrafts.collectAsState()
  val departments by viewModel.departments.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) } // 0: Published, 1: Drafts

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AdminPanelSettings,
              contentDescription = null,
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Admin Dashboard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
              Text("Full Content & Department Control", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
          }
        },
        navigationIcon = {
          IconButton(
            onClick = { viewModel.navigateBack() },
            modifier = Modifier.testTag("admin_dashboard_back_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
        },
        actions = {
          Button(
            onClick = { viewModel.navigateTo(Screen.AdminManageDepartments) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3E62)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.padding(end = 8.dp).testTag("admin_manage_depts_button")
          ) {
            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Departments", fontSize = 12.sp)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SolarNavyDark)
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { viewModel.navigateTo(Screen.AdminEditContent()) },
        containerColor = SolarBlue,
        contentColor = Color.White,
        modifier = Modifier.testTag("admin_fab_publish_new")
      ) {
        Icon(Icons.Default.PostAdd, contentDescription = "Publish Content")
      }
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
      // 1. Overview Counters Row
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          StatCounterCard(
            title = "Published",
            count = publishedItems.size,
            color = Color(0xFF16A34A),
            modifier = Modifier.weight(1f)
          )
          StatCounterCard(
            title = "Drafts",
            count = drafts.size,
            color = Color(0xFFD97706),
            modifier = Modifier.weight(1f)
          )
          StatCounterCard(
            title = "Departments",
            count = departments.size,
            color = SolarBlue,
            modifier = Modifier.weight(1f)
          )
        }
      }

      // 1a. User data entry
      item {
        val users by viewModel.allUsers.collectAsState()
        UsersEntryCard(
          totalUsers = users.size,
          blockedUsers = users.count { it.isBlocked },
          onClick = { viewModel.navigateTo(Screen.AdminUsers) }
        )
      }

      // 1b. User requests entry
      item {
        val requests by viewModel.allRequests.collectAsState()
        RequestsEntryCard(
          isAdmin = true,
          pendingCount = requests.count { it.status == RequestStatus.PENDING },
          onClick = { viewModel.navigateTo(Screen.UserRequests) }
        )
      }

      // 2. Tabs: Published vs Drafts
      item {
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = SurfaceCard,
          contentColor = SolarNavyDark,
          modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            modifier = Modifier.testTag("admin_tab_published"),
            text = {
              Text(
                text = "Published (${publishedItems.size})",
                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
              )
            }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            modifier = Modifier.testTag("admin_tab_drafts"),
            text = {
              Text(
                text = "Drafts (${drafts.size})",
                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
              )
            }
          )
        }
      }

      // 3. Content list
      val currentList = if (selectedTab == 0) publishedItems else drafts

      if (currentList.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
          ) {
            Column(
              modifier = Modifier.padding(32.dp).fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = if (selectedTab == 0) Icons.Default.FactCheck else Icons.Default.Drafts,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(40.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = if (selectedTab == 0) "No published content yet." else "No drafts saved.",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Tap the '+' button below to publish new information or save a draft.",
                fontSize = 12.sp,
                color = TextSecondary
              )
            }
          }
        }
      } else {
        items(currentList, key = { it.content.id }) { item ->
          AdminContentRowCard(
            item = item,
            onClick = { viewModel.navigateTo(Screen.ContentDetail(item.content.id)) },
            onEdit = { viewModel.navigateTo(Screen.AdminEditContent(contentId = item.content.id)) },
            onTogglePublish = { viewModel.setContentPublishStatus(item.content.id, !item.content.isPublished) },
            onDelete = { viewModel.deleteContent(item.content.id) }
          )
        }
      }
    }
  }
}

@Composable
fun StatCounterCard(
  title: String,
  count: Int,
  color: Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = count.toString(),
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = TextSecondary
      )
    }
  }
}

@Composable
fun AdminContentRowCard(
  item: ContentItemWithDepartment,
  onClick: () -> Unit,
  onEdit: () -> Unit,
  onTogglePublish: () -> Unit,
  onDelete: () -> Unit
) {
  val typeColor = IconHelper.getContentTypeColor(item.content.contentType)
  val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
  var menuExpanded by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("admin_content_row_${item.content.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(typeColor.copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = IconHelper.getContentTypeIcon(item.content.contentType),
            contentDescription = null,
            tint = typeColor,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Text(
            text = item.content.title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = item.departmentName,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = SolarBlue
            )
            Text(text = " • ", color = TextMuted, fontSize = 11.sp)
            Text(
              text = item.content.contentType.displayName,
              fontSize = 11.sp,
              color = TextSecondary
            )
            Text(text = " • ", color = TextMuted, fontSize = 11.sp)
            Text(
              text = dateFormat.format(Date(item.content.updatedAt)),
              fontSize = 11.sp,
              color = TextMuted
            )
          }
        }
      }

      Box {
        IconButton(
          onClick = { menuExpanded = true },
          modifier = Modifier.size(32.dp).testTag("admin_row_menu_${item.content.id}")
        ) {
          Icon(Icons.Default.MoreVert, contentDescription = "Actions", tint = TextSecondary)
        }

        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false }
        ) {
          DropdownMenuItem(
            text = { Text("Edit") },
            onClick = {
              menuExpanded = false
              onEdit()
            },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
          )
          DropdownMenuItem(
            text = { Text(if (item.content.isPublished) "Unpublish (Move to Drafts)" else "Publish Live") },
            onClick = {
              menuExpanded = false
              onTogglePublish()
            },
            leadingIcon = {
              Icon(
                if (item.content.isPublished) Icons.Default.VisibilityOff else Icons.Default.Publish,
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
}
