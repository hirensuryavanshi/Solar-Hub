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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.solarhub.model.RequestStatus
import com.example.solarhub.model.UserRequest
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
  val requests by viewModel.allRequests.collectAsState()
  val departments by viewModel.departments.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) } // 0: Published, 1: Drafts, 2: Requests
  var replyTarget by remember { mutableStateOf<Pair<UserRequest, RequestStatus>?>(null) }
  var replyText by remember { mutableStateOf("") }

  Scaffold(
    modifier = modifier.navigationBarsPadding(),
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
              Text("Real-Time CMS & Requests", color = Color(0xFF94A3B8), fontSize = 11.sp)
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
      item {
        val pendingCount = requests.count { it.status == RequestStatus.PENDING }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StatCounterCard("Published", publishedItems.size, Color(0xFF16A34A), Modifier.weight(1f))
          StatCounterCard("Drafts", drafts.size, Color(0xFFD97706), Modifier.weight(1f))
          StatCounterCard("Pending Req", pendingCount, Color(0xFFDC2626), Modifier.weight(1f))
          StatCounterCard("Depts", departments.size, SolarBlue, Modifier.weight(1f))
        }
      }

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
                fontSize = 12.sp,
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
                fontSize = 12.sp,
                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
              )
            }
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            modifier = Modifier.testTag("admin_tab_requests"),
            text = {
              Text(
                text = "Requests (${requests.size})",
                fontSize = 12.sp,
                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
              )
            }
          )
        }
      }

      if (selectedTab == 2) {
        if (requests.isEmpty()) {
          item {
            EmptyAdminCard(
              icon = Icons.Default.Inbox,
              title = "No user requests yet.",
              subtitle = "Incoming employee requests will appear here immediately in real time."
            )
          }
        } else {
          items(requests, key = { it.id }) { req ->
            AdminRequestCard(
              request = req,
              onAction = { status ->
                replyTarget = req to status
                replyText = req.adminReply
              },
              onDelete = { viewModel.deleteRequestByAdmin(req.id) }
            )
          }
        }
      } else {
        val currentList = if (selectedTab == 0) publishedItems else drafts
        if (currentList.isEmpty()) {
          item {
            EmptyAdminCard(
              icon = if (selectedTab == 0) Icons.AutoMirrored.Filled.FactCheck else Icons.Default.Drafts,
              title = if (selectedTab == 0) "No published content yet." else "No drafts saved.",
              subtitle = "Tap the '+' button below to publish new information or save a draft."
            )
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

  replyTarget?.let { (req, targetStatus) ->
    AlertDialog(
      onDismissRequest = { replyTarget = null },
      title = { Text("Mark Request ${targetStatus.label}", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("${req.userName} (${req.userMobile})", fontSize = 13.sp, color = TextSecondary)
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = replyText,
            onValueChange = { replyText = it },
            label = { Text("Reply message to employee (optional)") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.updateRequestByAdmin(req.id, targetStatus, replyText.trim())
            replyTarget = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark)
        ) {
          Text("Confirm")
        }
      },
      dismissButton = {
        TextButton(onClick = { replyTarget = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun EmptyAdminCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String
) {
  Card(
    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
  ) {
    Column(
      modifier = Modifier.padding(32.dp).fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
      Spacer(modifier = Modifier.height(12.dp))
      Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
    }
  }
}

@Composable
private fun AdminRequestCard(
  request: UserRequest,
  onAction: (RequestStatus) -> Unit,
  onDelete: () -> Unit
) {
  val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
  val (badgeBg, badgeFg) = when (request.status) {
    RequestStatus.PENDING -> Color(0xFFFEF3C7) to Color(0xFF92400E)
    RequestStatus.ACCEPTED -> Color(0xFFDBEAFE) to Color(0xFF1E40AF)
    RequestStatus.COMPLETED -> Color(0xFFDCFCE7) to Color(0xFF166534)
    RequestStatus.REJECTED -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("${request.userName} (${request.userMobile})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SolarNavyDark)
          Text(dateFormat.format(Date(request.createdAt)), fontSize = 11.sp, color = TextMuted)
        }
        Surface(shape = RoundedCornerShape(6.dp), color = badgeBg) {
          Text(
            text = request.status.label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = badgeFg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(request.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
      if (request.departmentName.isNotBlank()) {
        Text("Dept: ${request.departmentName}", fontSize = 11.sp, color = SolarBlue, fontWeight = FontWeight.SemiBold)
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(request.details, fontSize = 13.sp, color = TextSecondary)
      if (request.adminReply.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text("Reply: ${request.adminReply}", fontSize = 12.sp, color = SolarBlue, fontWeight = FontWeight.Medium)
      }

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(onClick = { onAction(RequestStatus.ACCEPTED) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(6.dp)) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Accept", fontSize = 11.sp)
        }
        Button(
          onClick = { onAction(RequestStatus.COMPLETED) },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(6.dp)
        ) {
          Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Done", fontSize = 11.sp)
        }
        OutlinedButton(onClick = { onAction(RequestStatus.REJECTED) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(6.dp)) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Reject", fontSize = 11.sp)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
          Icon(Icons.Default.Delete, contentDescription = "Delete request", tint = Color(0xFFDC2626))
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
      modifier = Modifier.padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(text = count.toString(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TextSecondary, maxLines = 1)
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
            Text(text = item.departmentName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SolarBlue)
            Text(text = " • ", color = TextMuted, fontSize = 11.sp)
            Text(text = item.content.contentType.displayName, fontSize = 11.sp, color = TextSecondary)
            Text(text = " • ", color = TextMuted, fontSize = 11.sp)
            Text(text = dateFormat.format(Date(item.content.updatedAt)), fontSize = 11.sp, color = TextMuted)
          }
        }
      }

      Box {
        IconButton(
          onClick = { menuExpanded = true },
          modifier = Modifier.size(36.dp).testTag("admin_row_menu_${item.content.id}")
        ) {
          Icon(Icons.Default.MoreVert, contentDescription = "Actions", tint = TextSecondary)
        }

        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
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
