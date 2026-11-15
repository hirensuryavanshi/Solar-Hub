package com.example.solarhub.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.solarhub.model.ContentItem
import com.example.solarhub.model.ContentType
import com.example.ui.theme.BorderLight
import com.example.ui.theme.SolarBlue
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DocumentViewerContainer(
  content: ContentItem,
  apiBaseUrl: String = "",
  authToken: String = "",
  onDownload: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isFullscreenImageOpen by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxWidth()) {
    when (content.contentType) {
      ContentType.PDF -> PdfViewerSection(content = content, onDownload = onDownload)
      ContentType.IMAGE -> ImageViewerSection(
        content = content,
        apiBaseUrl = apiBaseUrl,
        authToken = authToken,
        onOpenFullscreen = { isFullscreenImageOpen = true },
        onDownload = onDownload
      )
      ContentType.VIDEO -> VideoPlayerSection(content = content, onDownload = onDownload)
      ContentType.EXCEL -> ExcelViewerSection(content = content, onDownload = onDownload)
      ContentType.WORD, ContentType.PPT, ContentType.TRAINING, ContentType.OTHER -> {
        GenericDocumentViewerSection(content = content, onDownload = onDownload)
      }
      ContentType.TEXT, ContentType.NOTICE -> {
        TextContentViewerSection(content = content, onDownload = onDownload)
      }
    }

    if (content.contentBody.isNotBlank() && content.contentType != ContentType.TEXT && content.contentType != ContentType.NOTICE) {
      Spacer(modifier = Modifier.height(20.dp))
      Text(text = "Content Details & Notes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
      Spacer(modifier = Modifier.height(8.dp))
      CleanReadingLayout(text = content.contentBody)
    }

    if (isFullscreenImageOpen) {
      FullscreenImageDialog(
        content = content,
        apiBaseUrl = apiBaseUrl,
        authToken = authToken,
        onDismiss = { isFullscreenImageOpen = false }
      )
    }
  }
}

@Composable
fun TextContentViewerSection(
  content: ContentItem,
  onDownload: () -> Unit
) {
  var fontSizeMultiplier by remember { mutableFloatStateOf(1f) }
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Reading View", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { if (fontSizeMultiplier > 0.8f) fontSizeMultiplier -= 0.15f },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.ZoomOut, contentDescription = "Decrease font", modifier = Modifier.size(18.dp))
          }
          IconButton(
            onClick = { if (fontSizeMultiplier < 1.6f) fontSizeMultiplier += 0.15f },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.ZoomIn, contentDescription = "Increase font", modifier = Modifier.size(18.dp))
          }
          IconButton(
            onClick = {
              val fullText = "${content.title}\n\n${content.description}\n\n${content.contentBody}"
              clipboardManager.setText(AnnotatedString(fullText))
              Toast.makeText(context, "Copied text to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.Share, contentDescription = "Copy text", modifier = Modifier.size(18.dp))
          }
          IconButton(
            onClick = onDownload,
            modifier = Modifier.size(36.dp).testTag("button_download_text")
          ) {
            Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(18.dp))
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      CleanReadingLayout(
        text = content.contentBody.ifBlank { content.description.ifBlank { content.title } },
        fontSizeMultiplier = fontSizeMultiplier
      )
    }
  }
}

@Composable
fun CleanReadingLayout(
  text: String,
  fontSizeMultiplier: Float = 1f
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(Color(0xFFF8FAFC))
      .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
      .padding(16.dp)
  ) {
    Text(
      text = text,
      fontSize = (15 * fontSizeMultiplier).sp,
      lineHeight = (24 * fontSizeMultiplier).sp,
      color = TextPrimary,
      fontFamily = FontFamily.Default
    )
  }
}

@Composable
fun PdfViewerSection(
  content: ContentItem,
  onDownload: () -> Unit
) {
  var currentPage by remember { mutableIntStateOf(1) }
  var zoomLevel by remember { mutableFloatStateOf(1f) }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("pdf_viewer_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = content.attachmentName ?: "Document.pdf",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextPrimary,
            maxLines = 1
          )
        }
        Button(
          onClick = onDownload,
          colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
          modifier = Modifier.testTag("pdf_download_button")
        ) {
          Icon(Icons.Default.Download, contentDescription = "Open PDF", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Open / Save", fontSize = 12.sp)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(220.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFFF1F5F9))
          .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
          .padding(14.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
        ) {
          Text("SOLAR HUB DOCUMENT PREVIEW", fontSize = (10 * zoomLevel).sp, fontWeight = FontWeight.Bold, color = SolarNavyDark)
          Spacer(modifier = Modifier.height(8.dp))
          Text(content.title, fontSize = (15 * zoomLevel).sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = content.contentBody.ifBlank { content.description.ifBlank { "Tap Open / Save above to download and open the full PDF file." } },
            fontSize = (12 * zoomLevel).sp,
            color = TextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row {
          IconButton(onClick = { if (currentPage > 1) currentPage-- }, enabled = currentPage > 1) {
            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous")
          }
          IconButton(onClick = { currentPage++ }) {
            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next")
          }
        }
        Row {
          IconButton(onClick = { if (zoomLevel > 0.8f) zoomLevel -= 0.1f }) {
            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out")
          }
          IconButton(onClick = { if (zoomLevel < 1.5f) zoomLevel += 0.1f }) {
            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in")
          }
        }
      }
    }
  }
}

@Composable
private fun ImageViewerSection(
  content: ContentItem,
  apiBaseUrl: String,
  authToken: String,
  onOpenFullscreen: () -> Unit,
  onDownload: () -> Unit
) {
  val context = LocalContext.current
  val resolvedUrl = resolveAttachmentUrl(content, apiBaseUrl)

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      if (resolvedUrl != null) {
        val req = remember(resolvedUrl, authToken) {
          ImageRequest.Builder(context)
            .data(resolvedUrl)
            .apply {
              if (authToken.isNotBlank()) addHeader("Authorization", "Bearer $authToken")
            }
            .crossfade(true)
            .build()
        }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F5F9))
            .clickable { onOpenFullscreen() }
        ) {
          AsyncImage(
            model = req,
            contentDescription = content.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
          )
          IconButton(
            onClick = onOpenFullscreen,
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
          ) {
            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = SolarNavyDark)
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }
      Button(
        onClick = onDownload,
        colors = ButtonDefaults.buttonColors(containerColor = SolarBlue),
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Download / Open Image")
      }
    }
  }
}

@Composable
private fun FullscreenImageDialog(
  content: ContentItem,
  apiBaseUrl: String,
  authToken: String,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val resolvedUrl = resolveAttachmentUrl(content, apiBaseUrl)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {
      if (resolvedUrl != null) {
        val req = remember(resolvedUrl, authToken) {
          ImageRequest.Builder(context)
            .data(resolvedUrl)
            .apply {
              if (authToken.isNotBlank()) addHeader("Authorization", "Bearer $authToken")
            }
            .build()
        }
        AsyncImage(
          model = req,
          contentDescription = content.title,
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize().padding(16.dp)
        )
      }
      IconButton(
        onClick = onDismiss,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(16.dp)
          .clip(CircleShape)
          .background(Color(0x88000000))
      ) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
      }
    }
  }
}

@Composable
private fun VideoPlayerSection(
  content: ContentItem,
  onDownload: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = SolarNavyDark)
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(CircleShape)
          .background(SolarBlue)
          .clickable { onDownload() },
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = "Play video", tint = Color.White, modifier = Modifier.size(34.dp))
      }
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = content.attachmentName ?: content.title,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedButton(onClick = onDownload) {
        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Download & Play Video", color = Color.White)
      }
    }
  }
}

@Composable
private fun ExcelViewerSection(
  content: ContentItem,
  onDownload: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(content.attachmentName ?: "Spreadsheet.xlsx", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
          Text("${(content.attachmentSizeBytes / 1024L).coerceAtLeast(1L)} KB", fontSize = 12.sp, color = TextSecondary)
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Button(
        onClick = onDownload,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Download & Open Spreadsheet")
      }
    }
  }
}

@Composable
private fun GenericDocumentViewerSection(
  content: ContentItem,
  onDownload: () -> Unit
) {
  val typeColor = IconHelper.getContentTypeColor(content.contentType)
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(IconHelper.getContentTypeIcon(content.contentType), contentDescription = null, tint = typeColor, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(content.attachmentName ?: content.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
          Text(content.contentType.displayName, fontSize = 12.sp, color = TextSecondary)
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Button(
        onClick = onDownload,
        colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Download & Open File")
      }
    }
  }
}

private fun resolveAttachmentUrl(content: ContentItem, apiBaseUrl: String): String? {
  val raw = content.attachmentUrlOrPath ?: content.attachmentFileId?.let { "/api/files/$it" } ?: return null
  if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
  val base = apiBaseUrl.trimEnd('/')
  if (base.isBlank()) return null
  return "$base${if (raw.startsWith("/")) raw else "/$raw"}"
}
