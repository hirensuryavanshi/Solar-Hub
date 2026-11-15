package com.example.solarhub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solarhub.model.ContentItemWithDepartment
import com.example.solarhub.model.Department
import com.example.solarhub.model.RequestStatus
import com.example.solarhub.model.SyncConnectionState
import com.example.solarhub.model.UserRequest
import com.example.solarhub.model.UserRole
import com.example.solarhub.ui.components.IconHelper
import com.example.solarhub.ui.components.SolarCalculatorCard
import com.example.solarhub.viewmodel.Screen
import com.example.solarhub.viewmodel.SolarHubViewModel
import com.example.ui.theme.BackgroundLightGrey
import com.example.ui.theme.SolarBlue
import com.example.ui.theme.SolarBlueLight
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.SolarNavyMid
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
  viewModel: SolarHubViewModel,
  onOpenAuthDialog: () -> Unit,
  modifier: Modifier = Modifier
) {
  val departments by viewModel.departments.collectAsState()
  val latestUpdates by viewModel.latestUpdates.collectAsState()
  val announcements by viewModel.announcements.collectAsState()
  val myRequests by viewModel.myRequests.collectAsState()
  val userRole by viewModel.userRole.collectAsState()
  val syncState by viewModel.syncState.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val searchResults by viewModel.searchResults.collectAsState()
  val isBusy by viewModel.isBusy.collectAsState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(BackgroundLightGrey)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding()
        .testTag("home_screen_scroll"),
      contentPadding = PaddingValues(bottom = 36.dp)
    ) {
      // 1. TOP HEADER & DARK NAVY BLUE WELCOME SECTION
      item {
        DarkNavyWelcomeSection(
          userRole = userRole,
          syncState = syncState,
          searchQuery = searchQuery,
          onSearchQueryChange = { viewModel.setSearchQuery(it) },
          onSyncClick = { viewModel.triggerSync() },
          onOpenAuthDialog = onOpenAuthDialog,
          onOpenAdminDashboard = { viewModel.navigateTo(Screen.AdminDashboard) }
        )
      }

      // 2. SEARCH RESULTS
      if (searchQuery.isNotBlank()) {
        item {
          SearchResultsSection(
            query = searchQuery,
            results = searchResults,
            onContentClick = { viewModel.navigateTo(Screen.ContentDetail(it.content.id)) },
            onClearSearch = { viewModel.setSearchQuery("") }
          )
        }
      }

      // 3. IMPORTANT ADMIN ANNOUNCEMENTS (Latest First, Visible to Everyone)
      if (announcements.isNotEmpty()) {
        item {
          Spacer(modifier = Modifier.height(20.dp))
          Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(22.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Important Announcements",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            announcements.forEach { item ->
              AnnouncementBannerCard(
                item = item,
                onClick = { viewModel.navigateTo(Screen.ContentDetail(item.content.id)) }
              )
              Spacer(modifier = Modifier.height(8.dp))
            }
          }
        }
      }

      // 4. EXPLORE DEPARTMENTS SECTION
      item {
        Spacer(modifier = Modifier.height(22.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Explore Departments",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "Department knowledge bases (${departments.size} departments)",
                fontSize = 13.sp,
                color = TextSecondary
              )
            }

            if (userRole == UserRole.ADMIN) {
              Button(
                onClick = { viewModel.navigateTo(Screen.AdminManageDepartments) },
                colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("button_manage_departments")
              ) {
                Text("Manage", fontSize = 12.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          DepartmentCardsGrid(
            departments = departments,
            onDepartmentClick = { dept -> viewModel.navigateTo(Screen.DepartmentDetail(dept.id)) }
          )
        }
      }

      // 5. QUICK ACCESS – SOLAR CALCULATOR SECTION
      item {
        Spacer(modifier = Modifier.height(26.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          Text(
            text = "Quick Access",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Instant engineering tools for manufacturing capacity sizing",
            fontSize = 13.sp,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(12.dp))
          SolarCalculatorCard()
        }
      }

      // 6. LATEST UPDATES SECTION
      item {
        Spacer(modifier = Modifier.height(26.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Latest Updates",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "Recent publications uploaded by department administrators",
                fontSize = 13.sp,
                color = TextSecondary
              )
            }

            if (userRole == UserRole.ADMIN) {
              Button(
                onClick = { viewModel.navigateTo(Screen.AdminEditContent()) },
                colors = ButtonDefaults.buttonColors(containerColor = SolarBlue),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("button_publish_content_shortcut")
              ) {
                Text("+ Publish", fontSize = 12.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          if (latestUpdates.isEmpty()) {
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("empty_updates_card"),
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = SurfaceCard),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = null,
                  tint = TextMuted,
                  modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = "No information available yet.",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "All department knowledge items, SOPs, and notices will appear here once published by Admin.",
                  fontSize = 12.sp,
                  color = TextSecondary,
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        }
      }

      if (latestUpdates.isNotEmpty()) {
        items(latestUpdates, key = { it.content.id }) { itemWithDept ->
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            LatestUpdateCard(
              item = itemWithDept,
              onClick = { viewModel.navigateTo(Screen.ContentDetail(itemWithDept.content.id)) }
            )
          }
        }
      }

      // 7. SUBMIT REQUEST & PRIVATE MY REQUEST HISTORY
      item {
        Spacer(modifier = Modifier.height(26.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          Text(
            text = "Submit Request & History",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Private to your account — Admin responds in real time",
            fontSize = 13.sp,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(12.dp))

          SubmitRequestCard(
            departments = departments,
            isBusy = isBusy,
            onSubmit = { deptId, deptName, title, details, done ->
              viewModel.submitRequest(deptId, deptName, title, details, done)
            }
          )
        }
      }

      if (myRequests.isNotEmpty()) {
        items(myRequests, key = { it.id }) { req ->
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
            MyRequestStatusCard(request = req)
          }
        }
      }
    }
  }
}

@Composable
private fun DarkNavyWelcomeSection(
  userRole: UserRole,
  syncState: SyncConnectionState,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  onSyncClick: () -> Unit,
  onOpenAuthDialog: () -> Unit,
  onOpenAdminDashboard: () -> Unit
) {
  val syncIcon = when (syncState) {
    SyncConnectionState.LIVE -> Icons.Default.CloudDone
    SyncConnectionState.SYNCING, SyncConnectionState.CONNECTING -> Icons.Default.Sync
    else -> Icons.Default.CloudOff
  }
  val syncTint = when (syncState) {
    SyncConnectionState.LIVE -> Color(0xFF4ADE80)
    SyncConnectionState.SYNCING, SyncConnectionState.CONNECTING -> Color(0xFF38BDF8)
    else -> Color(0xFFFCA5A5)
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        brush = Brush.verticalGradient(colors = listOf(SolarNavyDark, SolarNavyMid)),
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
      )
      .statusBarsPadding()
      .padding(horizontal = 20.dp, vertical = 18.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF1E3E62))
              .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.WbSunny,
              contentDescription = "Solar Hub Logo",
              tint = SolarGold,
              modifier = Modifier.size(26.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "SOLAR HUB",
              color = Color.White,
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 1.sp
            )
            Text(
              text = "SOLAR MANUFACTURING KNOWLEDGE",
              color = Color(0xFF93C5FD),
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold,
              letterSpacing = 0.8.sp
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0x28FFFFFF),
            modifier = Modifier
              .clickable { onSyncClick() }
              .testTag("sync_status_badge")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(syncIcon, contentDescription = syncState.label, tint = syncTint, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(syncState.label, color = syncTint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }

          if (userRole == UserRole.ADMIN) {
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = Color(0xFF16A34A),
              modifier = Modifier
                .clickable { onOpenAdminDashboard() }
                .testTag("admin_dashboard_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin CMS", tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Admin", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0x26FFFFFF),
            modifier = Modifier
              .clickable { onOpenAuthDialog() }
              .testTag("role_badge_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Person, contentDescription = "Account", tint = Color.White, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (userRole == UserRole.ADMIN) "Account" else "Employee",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(22.dp))

      Text(
        text = "Welcome to Solar Hub",
        color = Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Your central platform for solar manufacturing knowledge and information sharing.",
        color = Color(0xFFCBD5E1),
        fontSize = 14.sp,
        lineHeight = 20.sp
      )

      Spacer(modifier = Modifier.height(18.dp))

      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
          Text(
            text = "Search departments, published info, documents...",
            fontSize = 13.sp,
            color = TextMuted
          )
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = SolarBlue) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChange("") }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextMuted)
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = Color.White,
          unfocusedContainerColor = Color.White,
          focusedBorderColor = SolarBlue,
          unfocusedBorderColor = Color.Transparent,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("home_search_input")
      )

      Spacer(modifier = Modifier.height(6.dp))
    }
  }
}

@Composable
private fun AnnouncementBannerCard(
  item: ContentItemWithDepartment,
  onClick: () -> Unit
) {
  val dateFormat = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(Color(0xFFFEF3C7)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "ANNOUNCEMENT",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF92400E)
          )
          Text(text = " • ${item.departmentName}", fontSize = 11.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = item.content.title,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (item.content.description.isNotBlank()) {
          Text(
            text = item.content.description,
            fontSize = 12.sp,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
        Text(
          text = dateFormat.format(Date(item.content.publishedAt)),
          fontSize = 10.sp,
          color = TextMuted
        )
      }
      Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
    }
  }
}

@Composable
private fun DepartmentCardsGrid(
  departments: List<Department>,
  onDepartmentClick: (Department) -> Unit
) {
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val columns = when {
      maxWidth >= 840.dp -> 4
      maxWidth >= 600.dp -> 3
      else -> 2
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      departments.chunked(columns).forEach { rowItems ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          rowItems.forEach { dept ->
            DepartmentCard(
              department = dept,
              onClick = { onDepartmentClick(dept) },
              modifier = Modifier.weight(1f)
            )
          }
          repeat(columns - rowItems.size) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}

@Composable
private fun DepartmentCard(
  department: Department,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .height(110.dp)
      .clickable { onClick() }
      .testTag("department_card_${department.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(14.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(SolarBlueLight),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = IconHelper.getDepartmentIcon(department.iconKey),
          contentDescription = department.name,
          tint = SolarBlue,
          modifier = Modifier.size(24.dp)
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = department.name,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubmitRequestCard(
  departments: List<Department>,
  isBusy: Boolean,
  onSubmit: (deptId: String, deptName: String, title: String, details: String, onComplete: (Boolean, String?) -> Unit) -> Unit
) {
  var selectedDeptId by remember { mutableStateOf("") }
  var deptExpanded by remember { mutableStateOf(false) }
  var title by remember { mutableStateOf("") }
  var details by remember { mutableStateOf("") }

  LaunchedEffect(departments) {
    if (selectedDeptId.isBlank() && departments.isNotEmpty()) {
      selectedDeptId = departments.first().id
    }
  }

  val selectedDept = departments.find { it.id == selectedDeptId }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      if (departments.isNotEmpty()) {
        ExposedDropdownMenuBox(
          expanded = deptExpanded,
          onExpandedChange = { deptExpanded = it }
        ) {
          OutlinedTextField(
            value = selectedDept?.name ?: "Select Department",
            onValueChange = {},
            readOnly = true,
            label = { Text("Department") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
          )
          ExposedDropdownMenu(
            expanded = deptExpanded,
            onDismissRequest = { deptExpanded = false }
          ) {
            departments.forEach { d ->
              DropdownMenuItem(
                text = { Text(d.name) },
                onClick = {
                  selectedDeptId = d.id
                  deptExpanded = false
                }
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Request Subject") },
        placeholder = { Text("e.g. Need EL Calibration SOP") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("input_request_title")
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = details,
        onValueChange = { details = it },
        label = { Text("Request Details *") },
        placeholder = { Text("Describe the document or information you need...") },
        minLines = 3,
        maxLines = 5,
        modifier = Modifier.fillMaxWidth().testTag("input_request_details")
      )

      Spacer(modifier = Modifier.height(12.dp))

      Button(
        onClick = {
          onSubmit(
            selectedDept?.id ?: "",
            selectedDept?.name ?: "",
            title,
            details
          ) { ok, _ ->
            if (ok) {
              title = ""
              details = ""
            }
          }
        },
        enabled = !isBusy && details.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().testTag("button_submit_request")
      ) {
        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Submit Request", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun MyRequestStatusCard(request: UserRequest) {
  val dateFormat = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
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
        Text(
          text = request.title,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = TextPrimary,
          modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (request.isPendingSync) {
            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF1F5F9)) {
              Text(
                text = "QUEUED",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
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
      }
      if (request.departmentName.isNotBlank()) {
        Text(text = request.departmentName, fontSize = 11.sp, color = SolarBlue, fontWeight = FontWeight.SemiBold)
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = request.details, fontSize = 13.sp, color = TextSecondary)
      if (request.adminReply.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEFF6FF), modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Admin Reply: ${request.adminReply}",
            fontSize = 12.sp,
            color = SolarNavyDark,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(10.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(text = dateFormat.format(Date(request.createdAt)), fontSize = 11.sp, color = TextMuted)
    }
  }
}

@Composable
private fun LatestUpdateCard(
  item: ContentItemWithDepartment,
  onClick: () -> Unit
) {
  val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
  val typeColor = IconHelper.getContentTypeColor(item.content.contentType)

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("latest_update_card_${item.content.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(typeColor.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = IconHelper.getContentTypeIcon(item.content.contentType),
          contentDescription = item.content.contentType.displayName,
          tint = typeColor,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(shape = RoundedCornerShape(6.dp), color = SolarBlueLight) {
            Text(
              text = item.departmentName,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = SolarBlue,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = item.content.contentType.displayName, fontSize = 11.sp, color = TextMuted)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = item.content.title,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (item.content.description.isNotBlank()) {
          Text(
            text = item.content.description,
            fontSize = 12.sp,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Published ${dateFormat.format(Date(item.content.publishedAt))}",
          fontSize = 11.sp,
          color = TextMuted
        )
      }

      Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = TextMuted, modifier = Modifier.size(18.dp))
    }
  }
}

@Composable
private fun SearchResultsSection(
  query: String,
  results: List<ContentItemWithDepartment>,
  onContentClick: (ContentItemWithDepartment) -> Unit,
  onClearSearch: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Search Results for \"$query\" (${results.size})",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      Text(
        text = "Clear",
        color = SolarBlue,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clickable { onClearSearch() }
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (results.isEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
      ) {
        Text(
          text = "No matching documents or information found.",
          color = TextSecondary,
          fontSize = 13.sp,
          modifier = Modifier.padding(16.dp)
        )
      }
    } else {
      results.forEach { item ->
        LatestUpdateCard(item = item, onClick = { onContentClick(item) })
        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }
}
