package com.example.solarhub.ui.components

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.solarhub.model.ContentItem
import com.example.solarhub.model.ContentType
import com.example.ui.theme.BorderLight
import com.example.ui.theme.SolarBlue
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun DocumentViewerContainer(
  content: ContentItem,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var isFullscreenImageOpen by remember { mutableStateOf(false) }

  Column(
    modifier = modifier.fillMaxWidth()
  ) {
    // Media / Document viewer section based on type
    when (content.contentType) {
      ContentType.PDF -> {
        PdfViewerSection(
          content = content,
          onDownload = { downloadFile(context, content) }
        )
      }
      ContentType.IMAGE -> {
        ImageViewerSection(
          content = content,
          onOpenFullscreen = { isFullscreenImageOpen = true },
          onDownload = { downloadFile(context, content) }
        )
      }
      ContentType.VIDEO -> {
        VideoPlayerSection(
          content = content,
          onDownload = { downloadFile(context, content) }
        )
      }
      ContentType.EXCEL -> {
        ExcelViewerSection(
          content = content,
          onDownload = { downloadFile(context, content) }
        )
      }
      ContentType.WORD, ContentType.PPT, ContentType.TRAINING, ContentType.OTHER -> {
        GenericDocumentViewerSection(
          content = content,
          onDownload = { downloadFile(context, content) }
        )
      }
      ContentType.TEXT, ContentType.NOTICE -> {
        // Text Information layout
        TextContentViewerSection(
          content = content,
          onDownload = { downloadFile(context, content) }
        )
      }
    }

    // Open / Share the real attached file (PDF, photo, video, Office files)
    if (!content.attachmentUrlOrPath.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = { AttachmentUtils.open(context, content) },
          colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
          modifier = Modifier.weight(1f).testTag("button_open_file")
        ) {
          Text("Open File")
        }
        OutlinedButton(
          onClick = { AttachmentUtils.share(context, content) },
          modifier = Modifier.weight(1f).testTag("button_share_file")
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Share")
        }
      }
    }

    // Always show clean reading layout for the main body/notes if present
    if (content.contentBody.isNotBlank() && content.contentType != ContentType.TEXT && content.contentType != ContentType.NOTICE) {
      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text = "Content Details & Notes",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      Spacer(modifier = Modifier.height(8.dp))
      CleanReadingLayout(text = content.contentBody)
    }

    // Fullscreen Image Dialog
    if (isFullscreenImageOpen) {
      FullscreenImageDialog(
        content = content,
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
      // Toolbar for Reader
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Reading View",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextSecondary
        )

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
            Icon(Icons.Default.Download, contentDescription = "Download text file", modifier = Modifier.size(18.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      CleanReadingLayout(
        text = if (content.contentBody.isNotBlank()) content.contentBody else content.description,
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
  val totalPages = 4
  var zoomLevel by remember { mutableFloatStateOf(1f) }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("pdf_viewer_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // PDF Reader Controls
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.PictureAsPdf,
            contentDescription = null,
            tint = Color(0xFFDC2626),
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = content.attachmentName ?: "Document.pdf",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextPrimary,
            maxLines = 1
          )
        }

        IconButton(
          onClick = onDownload,
          modifier = Modifier.testTag("pdf_download_button")
        ) {
          Icon(Icons.Default.Download, contentDescription = "Download PDF", tint = SolarNavyDark)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Simulated Integrated PDF Page Viewer
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(340.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFFF1F5F9))
          .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
          .padding(12.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
        ) {
          // Document Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "SOLAR HUB OFFICIAL DOCUMENT",
              fontSize = (9 * zoomLevel).sp,
              fontWeight = FontWeight.Bold,
              color = SolarNavyDark
            )
            Text(
              text = "PAGE $currentPage / $totalPages",
              fontSize = (9 * zoomLevel).sp,
              color = TextMuted
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = content.title,
            fontSize = (16 * zoomLevel).sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Category: ${content.category.ifBlank { "General" }} | Department Document",
            fontSize = (11 * zoomLevel).sp,
            color = TextSecondary
          )

          Spacer(modifier = Modifier.height(14.dp))

          when (currentPage) {
            1 -> {
              Text(
                text = "1. OBJECTIVE & SUMMARY",
                fontSize = (13 * zoomLevel).sp,
                fontWeight = FontWeight.SemiBold,
                color = SolarNavyDark
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = content.description.ifBlank { "Standard technical procedure and operating guidelines for solar module production lines." },
                fontSize = (12 * zoomLevel).sp,
                lineHeight = (18 * zoomLevel).sp,
                color = TextPrimary
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "2. DETAILED SPECIFICATION",
                fontSize = (13 * zoomLevel).sp,
                fontWeight = FontWeight.SemiBold,
                color = SolarNavyDark
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = content.contentBody.ifBlank {
                  "All processes must strictly follow quality safety benchmarks. Verify line calibration, EL testing parameters, and laminate vacuum pressure before releasing batches."
                },
                fontSize = (12 * zoomLevel).sp,
                lineHeight = (18 * zoomLevel).sp,
                color = TextPrimary
              )
            }
            2 -> {
              Text(
                text = "3. QUALITY CHECKPOINTS & ACCEPTANCE CRITERIA",
                fontSize = (13 * zoomLevel).sp,
                fontWeight = FontWeight.SemiBold,
                color = SolarNavyDark
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "• Cell Alignment Tolerance: ± 0.5 mm\n• Busbar Solder Integrity: 100% Visual + EL inspection\n• EVA / Backsheet Gel Content: >= 80%\n• Hi-Pot Insulation Resistance: >= 100 MΩ @ 1500V DC",
                fontSize = (12 * zoomLevel).sp,
                lineHeight = (20 * zoomLevel).sp,
                color = TextPrimary
              )
            }
            3 -> {
              Text(
                text = "4. SAFETY, HAZARD MITIGATION & PPE",
                fontSize = (13 * zoomLevel).sp,
                fontWeight = FontWeight.SemiBold,
                color = SolarNavyDark
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "• Operators must wear anti-static wrist straps & cleanroom ESD coats.\n• Heat-resistant gloves required at hot lamination station (> 150°C).\n• Lockout-Tagout (LOTO) protocols mandatory for maintenance operations.",
                fontSize = (12 * zoomLevel).sp,
                lineHeight = (20 * zoomLevel).sp,
                color = TextPrimary
              )
            }
            else -> {
              Text(
                text = "5. SIGN-OFF & REVISION CONTROL",
                fontSize = (13 * zoomLevel).sp,
                fontWeight = FontWeight.SemiBold,
                color = SolarNavyDark
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Approved by: Solar Hub System Admin\nStatus: Controlled Quality Record\nNext Revision Audit: Scheduled Annually",
                fontSize = (12 * zoomLevel).sp,
                lineHeight = (18 * zoomLevel).sp,
                color = TextPrimary
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Bottom bar with pagination and zoom
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { if (currentPage > 1) currentPage-- },
            enabled = currentPage > 1,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.NavigateBefore, contentDescription = "Previous page")
          }
          Text(
            text = "$currentPage / $totalPages",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
          )
          IconButton(
            onClick = { if (currentPage < totalPages) currentPage++ },
            enabled = currentPage < totalPages,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.NavigateNext, contentDescription = "Next page")
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { if (zoomLevel > 0.8f) zoomLevel -= 0.15f },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out", modifier = Modifier.size(18.dp))
          }
          IconButton(
            onClick = { if (zoomLevel < 1.6f) zoomLevel += 0.15f },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in", modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}

@Composable
fun ImageViewerSection(
  content: ContentItem,
  onOpenFullscreen: () -> Unit,
  onDownload: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth().testTag("image_viewer_card"),
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
        Text(
          text = content.attachmentName ?: "Image Attachment",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = TextPrimary
        )
        Row {
          IconButton(onClick = onOpenFullscreen) {
            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen view", tint = SolarNavyDark)
          }
          IconButton(onClick = onDownload) {
            Icon(Icons.Default.Download, contentDescription = "Download image", tint = SolarNavyDark)
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(260.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF0B192C))
          .clickable { onOpenFullscreen() },
        contentAlignment = Alignment.Center
      ) {
        if (!content.attachmentUrlOrPath.isNullOrBlank()) {
          AsyncImage(
            model = AttachmentUtils.imageModel(content.attachmentUrlOrPath),
            contentDescription = content.title,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          // Fallback clean high-tech schematic graphic
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
          ) {
            Icon(
              imageVector = IconHelper.getContentTypeIcon(ContentType.IMAGE),
              contentDescription = null,
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = content.title,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Tap to view in high resolution fullscreen",
              color = Color(0xFF94A3B8),
              fontSize = 12.sp
            )
          }
        }
      }
    }
  }
}

@Composable
fun FullscreenImageDialog(
  content: ContentItem,
  onDismiss: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = Color.Black
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        if (!content.attachmentUrlOrPath.isNullOrBlank()) {
          AsyncImage(
            model = AttachmentUtils.imageModel(content.attachmentUrlOrPath),
            contentDescription = content.title,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Column(
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = IconHelper.getContentTypeIcon(ContentType.IMAGE),
              contentDescription = null,
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = content.title,
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = content.description,
              color = Color(0xFFCBD5E1),
              fontSize = 14.sp
            )
          }
        }

        // Close button
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp)
            .background(Color(0x66000000), CircleShape)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
      }
    }
  }
}

@Composable
fun VideoPlayerSection(
  content: ContentItem,
  onDownload: () -> Unit
) {
  var isPlaying by remember { mutableStateOf(false) }
  var playbackPosition by remember { mutableFloatStateOf(0.25f) }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("video_player_card"),
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
        Text(
          text = content.attachmentName ?: "Training Video.mp4",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = TextPrimary
        )
        IconButton(onClick = onDownload) {
          Icon(Icons.Default.Download, contentDescription = "Download video", tint = SolarNavyDark)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Video playback canvas
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(240.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          IconButton(
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier
              .size(64.dp)
              .background(Color(0x99000000), CircleShape)
              .border(2.dp, Color.White, CircleShape)
          ) {
            Icon(
              imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (isPlaying) "Pause" else "Play",
              tint = Color.White,
              modifier = Modifier.size(36.dp)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = if (isPlaying) "Playing: ${content.title}" else "Tap to Play Video",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
        }

        // Bottom progress bar overlay
        Column(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Color(0xCC000000))
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Slider(
            value = playbackPosition,
            onValueChange = { playbackPosition = it },
            colors = SliderDefaults.colors(
              thumbColor = Color(0xFF38BDF8),
              activeTrackColor = Color(0xFF38BDF8),
              inactiveTrackColor = Color(0xFF475569)
            ),
            modifier = Modifier.fillMaxWidth().height(20.dp)
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            val totalSeconds = 240 // 4 mins
            val currentSeconds = (playbackPosition * totalSeconds).toInt()
            val curMin = currentSeconds / 60
            val curSec = currentSeconds % 60
            Text(
              text = String.format("%02d:%02d / 04:00", curMin, curSec),
              color = Color.White,
              fontSize = 11.sp
            )
            Text(
              text = "1080p HD",
              color = Color(0xFF38BDF8),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
fun ExcelViewerSection(
  content: ContentItem,
  onDownload: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth().testTag("excel_viewer_card"),
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.TableChart,
            contentDescription = null,
            tint = Color(0xFF16A34A),
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = content.attachmentName ?: "Spreadsheet.xlsx",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextPrimary
          )
        }
        IconButton(onClick = onDownload) {
          Icon(Icons.Default.Download, contentDescription = "Download Excel file", tint = SolarNavyDark)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Excel Sheet Grid Preview
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
          .background(Color.White)
          .padding(8.dp)
      ) {
        Column {
          // Table header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFFE2E8F0))
              .padding(vertical = 6.dp, horizontal = 4.dp)
          ) {
            Text("Line", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.8f))
            Text("Parameter", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.8f))
            Text("Set Value", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
            Text("Actual", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
          }

          val rows = listOf(
            Triple("1", "Stringer Soldering Temp", "360°C to 380°C"),
            Triple("2", "EVA Lamination Time", "14 min 30 s"),
            Triple("3", "Chamber Pressure", "-98.5 kPa"),
            Triple("4", "Sun Simulator Irradiance", "1000 W/m² (AM 1.5G)"),
            Triple("5", "Hi-Pot Isolation Test", "Pass (Zero leakage)")
          )

          rows.forEachIndexed { idx, row ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(if (idx % 2 == 0) Color.White else Color(0xFFF8FAFC))
                .padding(vertical = 6.dp, horizontal = 4.dp)
            ) {
              Text(row.first, fontSize = 11.sp, modifier = Modifier.weight(0.8f))
              Text(row.second, fontSize = 11.sp, modifier = Modifier.weight(1.8f))
              Text(row.third, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
              Text("Verified", color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
            }
          }
        }
      }
    }
  }
}

@Composable
fun GenericDocumentViewerSection(
  content: ContentItem,
  onDownload: () -> Unit
) {
  val typeColor = IconHelper.getContentTypeColor(content.contentType)

  Card(
    modifier = Modifier.fillMaxWidth().testTag("generic_doc_viewer_card"),
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(typeColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = IconHelper.getContentTypeIcon(content.contentType),
              contentDescription = null,
              tint = typeColor,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = content.attachmentName ?: "${content.title}.${content.contentType.extensionHint}",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = TextPrimary
            )
            Text(
              text = "${content.contentType.displayName} • ${if (content.attachmentSizeBytes > 0) formatFileSize(content.attachmentSizeBytes) else "1.4 MB"}",
              fontSize = 12.sp,
              color = TextSecondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = onDownload,
        colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().testTag("button_download_generic_doc")
      ) {
        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Download Document")
      }
    }
  }
}

private fun downloadFile(context: Context, content: ContentItem) {
  AttachmentUtils.download(context, content)
}

private fun formatFileSize(bytes: Long): String {
  return when {
    bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
    bytes >= 1024 -> String.format("%.0f KB", bytes.toDouble() / 1024)
    else -> "$bytes B"
  }
}
