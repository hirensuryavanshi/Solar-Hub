package com.example.solarhub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.solarhub.model.UserRole
import com.example.solarhub.ui.components.IconHelper
import com.example.solarhub.ui.components.SolarCalculatorCard
import com.example.solarhub.viewmodel.Screen
import com.example.solarhub.viewmodel.SolarHubViewModel
import com.example.ui.theme.BackgroundLightGrey
import com.example.ui.theme.BorderLight
import com.example.ui.theme.SolarBlue
import com.example.ui.theme.SolarBlueLight
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.SolarNavyLight
import com.example.ui.theme.SolarNavyMid
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  viewModel: SolarHubViewModel,
  onOpenAuthDialog: () -> Unit,
  modifier: Modifier = Modifier
) {
  val departments by viewModel.departments.collectAsState()
  val latestUpdates by viewModel.latestUpdates.collectAsState()
  val userRole by viewModel.userRole.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val searchResults by viewModel.searchResults.collectAsState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(BackgroundLightGrey)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("home_screen_scroll"),
      contentPadding = PaddingValues(bottom = 36.dp)
    ) {
      // 1. TOP HEADER & DARK NAVY BLUE WELCOME SECTION
      item {
        DarkNavyWelcomeSection(
          userRole = userRole,
          searchQuery = searchQuery,
          onSearchQueryChange = { viewModel.setSearchQuery(it) },
          onOpenAuthDialog = onOpenAuthDialog,
          onOpenAdminDashboard = { viewModel.navigateTo(Screen.AdminDashboard) }
        )
      }

      // 2. SEARCH RESULTS (if searching)
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

      // 3. EXPLORE DEPARTMENTS SECTION
      item {
        Spacer(modifier = Modifier.height(24.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
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

          Spacer(modifier = Modifier.height(16.dp))

          // Department Cards Grid
          DepartmentCardsGrid(
            departments = departments,
            onDepartmentClick = { dept ->
              viewModel.navigateTo(Screen.DepartmentDetail(dept.id))
            }
          )
        }
      }

      // 3b. REQUESTS ENTRY
      item {
        Spacer(modifier = Modifier.height(24.dp))
        val requests by viewModel.allRequests.collectAsState()
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          RequestsEntryCard(
            isAdmin = userRole == UserRole.ADMIN,
            pendingCount = requests.count { it.status == "PENDING" },
            onClick = { viewModel.navigateTo(Screen.UserRequests) }
          )
        }
      }

      // 4. QUICK ACCESS – SOLAR CALCULATOR SECTION
      item {
        Spacer(modifier = Modifier.height(28.dp))
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

      // 5. LATEST UPDATES SECTION
      item {
        Spacer(modifier = Modifier.height(28.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
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
            // Empty State specified by user
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("empty_latest_updates_card"),
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
                Box(
                  modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(28.dp)
                  )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "No latest updates available.",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Published documents and technical notices will appear here once added by the Admin.",
                  fontSize = 12.sp,
                  color = TextSecondary,
                  textAlign = TextAlign.Center
                )
              }
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              latestUpdates.forEach { item ->
                LatestUpdateItemCard(
                  item = item,
                  onClick = { viewModel.navigateTo(Screen.ContentDetail(item.content.id)) }
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun DarkNavyWelcomeSection(
  userRole: UserRole,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  onOpenAuthDialog: () -> Unit,
  onOpenAdminDashboard: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(
            SolarNavyDark,
            SolarNavyMid
          )
        )
      )
      .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 28.dp)
  ) {
    Column {
      // Top row: Logo, Brand, and User Role Switcher
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
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
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 1.sp,
              color = Color.White
            )
            Text(
              text = "SOLAR MANUFACTURING KNOWLEDGE",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp,
              color = Color(0xFF93C5FD)
            )
          }
        }

        // Role Badge / Auth Switcher
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (userRole == UserRole.ADMIN) Color(0xFF166534) else Color(0x33FFFFFF),
            modifier = Modifier
              .clickable { onOpenAuthDialog() }
              .testTag("role_badge_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (userRole == UserRole.ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (userRole == UserRole.ADMIN) "ADMIN" else "EMPLOYEE",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
              )
            }
          }

          if (userRole == UserRole.ADMIN) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
              onClick = onOpenAdminDashboard,
              modifier = Modifier
                .size(36.dp)
                .background(Color(0x33FFFFFF), CircleShape)
                .testTag("admin_dashboard_icon_button")
            ) {
              Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = "Admin Dashboard",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Welcome heading & description
      Text(
        text = "Welcome to Solar Hub",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Your central platform for solar manufacturing knowledge and information sharing.",
        fontSize = 13.sp,
        color = Color(0xFFCBD5E1),
        lineHeight = 19.sp
      )

      Spacer(modifier = Modifier.height(20.dp))

      // Global Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search departments, published info, documents...", color = Color(0xFF94A3B8), fontSize = 13.sp) },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = "Search", tint = SolarNavyDark)
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChange("") }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextMuted)
            }
          }
        },
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = Color.White,
          unfocusedContainerColor = Color.White,
          focusedBorderColor = Color(0xFF38BDF8),
          unfocusedBorderColor = Color.Transparent,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("global_search_input")
      )
    }
  }
}

@Composable
fun SearchResultsSection(
  query: String,
  results: List<ContentItemWithDepartment>,
  onContentClick: (ContentItemWithDepartment) -> Unit,
  onClearSearch: () -> Unit
) {
  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Search results for \"$query\" (${results.size})",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      Text(
        text = "Clear",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = SolarBlue,
        modifier = Modifier.clickable { onClearSearch() }
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (results.isEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("No published information matched your query.", color = TextSecondary, fontSize = 13.sp)
        }
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        results.forEach { item ->
          LatestUpdateItemCard(
            item = item,
            onClick = { onContentClick(item) }
          )
        }
      }
    }
  }
}

@Composable
fun DepartmentCardsGrid(
  departments: List<Department>,
  onDepartmentClick: (Department) -> Unit
) {
  // Use responsive Rows to display white rounded cards with blue icons
  // 2 columns layout
  val chunked = departments.chunked(2)

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    chunked.forEach { rowItems ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        rowItems.forEach { dept ->
          DepartmentCard(
            department = dept,
            modifier = Modifier.weight(1f),
            onClick = { onDepartmentClick(dept) }
          )
        }
        if (rowItems.size == 1) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
fun DepartmentCard(
  department: Department,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Card(
    modifier = modifier
      .clickable { onClick() }
      .testTag("dept_card_${department.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Blue Department Icon in soft circular container
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

      Spacer(modifier = Modifier.width(12.dp))

      Text(
        text = department.name,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun LatestUpdateItemCard(
  item: ContentItemWithDepartment,
  onClick: () -> Unit
) {
  val typeColor = IconHelper.getContentTypeColor(item.content.contentType)
  val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("latest_update_item_${item.content.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Content Type icon
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(typeColor.copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = IconHelper.getContentTypeIcon(item.content.contentType),
            contentDescription = null,
            tint = typeColor,
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Text(
            text = item.content.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
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
            Text(
              text = " • ",
              fontSize = 11.sp,
              color = TextMuted
            )
            Text(
              text = item.content.contentType.displayName,
              fontSize = 11.sp,
              color = TextSecondary
            )
            Text(
              text = " • ",
              fontSize = 11.sp,
              color = TextMuted
            )
            Text(
              text = dateFormat.format(Date(item.content.publishedAt)),
              fontSize = 11.sp,
              color = TextMuted
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // View Option
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1F5F9),
        modifier = Modifier.testTag("view_button_${item.content.id}")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "View",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = SolarNavyDark
          )
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = SolarNavyDark,
            modifier = Modifier.size(12.dp)
          )
        }
      }
    }
  }
}
