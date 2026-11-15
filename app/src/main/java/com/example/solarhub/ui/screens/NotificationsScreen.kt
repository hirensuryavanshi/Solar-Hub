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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solarhub.ui.components.IconHelper
import com.example.solarhub.viewmodel.Screen
import com.example.solarhub.viewmodel.SolarHubViewModel
import com.example.ui.theme.BackgroundLightGrey
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Everything the admin published, newest first. New items (since last visit) are marked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  viewModel: SolarHubViewModel,
  seenAt: Long,
  onSeen: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { viewModel.navigateBack() }

  val published by viewModel.allPublished.collectAsState()
  val previousSeen = remember { seenAt }
  val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

  LaunchedEffect(Unit) { onSeen() }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text("Notifications", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SolarNavyDark)
      )
    },
    containerColor = BackgroundLightGrey
  ) { innerPadding ->
    if (published.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentAlignment = Alignment.Center
      ) {
        Text("No notifications yet.", color = TextSecondary)
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(published, key = { it.content.id }) { entry ->
          val item = entry.content
          val isNew = item.publishedAt > previousSeen
          val typeColor = IconHelper.getContentTypeColor(item.contentType)
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { viewModel.navigateTo(Screen.ContentDetail(item.id)) },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isNew) Color(0xFFEFF6FF) else SurfaceCard
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = IconHelper.getContentTypeIcon(item.contentType),
                contentDescription = null,
                tint = typeColor,
                modifier = Modifier.size(26.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "New post in ${entry.departmentName}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextSecondary
                )
                Text(
                  text = item.title,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = dateFormat.format(Date(item.publishedAt)),
                  fontSize = 11.sp,
                  color = TextMuted
                )
              }
              if (isNew) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFFDC2626), CircleShape)
                )
              }
            }
          }
        }
      }
    }
  }
}
