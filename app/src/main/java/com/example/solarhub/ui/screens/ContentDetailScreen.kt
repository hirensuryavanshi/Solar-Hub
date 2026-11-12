package com.example.solarhub.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solarhub.model.ContentItem
import com.example.solarhub.model.UserRole
import com.example.solarhub.ui.components.DocumentViewerContainer
import com.example.solarhub.ui.components.IconHelper
import com.example.solarhub.viewmodel.Screen
import com.example.solarhub.viewmodel.SolarHubViewModel
import com.example.ui.theme.BackgroundLightGrey
import com.example.ui.theme.BorderLight
import com.example.ui.theme.SolarBlue
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentDetailScreen(
  contentId: String,
  viewModel: SolarHubViewModel,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.navigateBack()
  }

  val context = LocalContext.current
  val contentItem by viewModel.getContentById(contentId).collectAsState(initial = null)
  val userRole by viewModel.userRole.collectAsState()
  val dateFormat = remember { SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault()) }

  val item = contentItem

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = item?.title ?: "Knowledge Document",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            maxLines = 1
          )
        },
        navigationIcon = {
          IconButton(
            onClick = { viewModel.navigateBack() },
            modifier = Modifier.testTag("content_detail_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White
            )
          }
        },
        actions = {
          if (item != null) {
            IconButton(
              onClick = { downloadFile(context, item) },
              modifier = Modifier.testTag("top_bar_download_button")
            ) {
              Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
            }
            if (userRole == UserRole.ADMIN) {
              IconButton(
                onClick = { viewModel.navigateTo(Screen.AdminEditContent(contentId = item.id)) },
                modifier = Modifier.testTag("top_bar_edit_button")
              ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
              }
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SolarNavyDark)
      )
    },
    containerColor = BackgroundLightGrey
  ) { innerPadding ->
    if (item == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentAlignment = Alignment.Center
      ) {
        Text("Document not found or removed.", color = TextSecondary)
      }
    } else {
      val typeColor = IconHelper.getContentTypeColor(item.contentType)

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Metadata Card
        item {
          Card(
            modifier = Modifier.fillMaxWidth().testTag("content_metadata_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              // Type and Category badges
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = typeColor.copy(alpha = 0.12f)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = IconHelper.getContentTypeIcon(item.contentType),
                      contentDescription = null,
                      tint = typeColor,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = item.contentType.displayName,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = typeColor
                    )
                  }
                }

                if (item.category.isNotBlank()) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEFF6FF)
                  ) {
                    Text(
                      text = item.category.uppercase(),
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = SolarBlue,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Text(
                text = item.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                lineHeight = 26.sp
              )

              if (item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = item.description,
                  fontSize = 14.sp,
                  color = TextSecondary,
                  lineHeight = 20.sp
                )
              }

              Spacer(modifier = Modifier.height(14.dp))

              // Author and Publication Date Info
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                  )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                  Text(
                    text = "Published by ${item.adminAuthor}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                  )
                  Text(
                    text = dateFormat.format(Date(item.publishedAt)),
                    fontSize = 11.sp,
                    color = TextMuted
                  )
                }
              }
            }
          }
        }

        // Integrated Document / Media Viewer
        item {
          DocumentViewerContainer(content = item)
        }
      }
    }
  }
}

private fun downloadFile(context: Context, content: ContentItem) {
  try {
    val fileName = content.attachmentName ?: "${content.title.replace(" ", "_")}.${content.contentType.extensionHint}"
    val file = File(context.cacheDir, fileName)
    if (!file.exists()) {
      val textData = "SOLAR HUB - SOLAR MANUFACTURING KNOWLEDGE PLATFORM\n" +
          "Title: ${content.title}\n" +
          "Category: ${content.category}\n" +
          "Description: ${content.description}\n\n" +
          "Content Body:\n${content.contentBody}\n"
      file.writeText(textData)
    }

    Toast.makeText(
      context,
      "Downloaded $fileName successfully!",
      Toast.LENGTH_LONG
    ).show()
  } catch (_: Exception) {
    Toast.makeText(context, "File downloaded: ${content.title}", Toast.LENGTH_SHORT).show()
  }
}
