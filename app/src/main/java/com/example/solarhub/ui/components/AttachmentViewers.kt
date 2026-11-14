package com.example.solarhub.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.solarhub.model.ContentType
import com.example.ui.theme.SolarBlue
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Shows every file attached to a post: photos and videos play inside the app. */
@Composable
fun AttachmentList(
  attachments: List<AttachmentUtils.Attachment>,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    attachments.forEach { attachment ->
      AttachmentCard(attachment)
    }
  }
}

@Composable
fun AttachmentCard(att: AttachmentUtils.Attachment) {
  val context = LocalContext.current
  var showFullscreen by remember { mutableStateOf(false) }
  val exists = remember(att.path) { att.file.exists() }
  val type = att.type

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = IconHelper.getContentTypeIcon(type),
          contentDescription = null,
          tint = IconHelper.getContentTypeColor(type),
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = att.name,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          if (exists) {
            Text(
              text = "${type.displayName} - ${AttachmentUtils.formatSize(att.file.length())}",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      when {
        !exists -> {
          Text(
            text = "This file is not available on this phone.",
            fontSize = 13.sp,
            color = TextSecondary
          )
        }
        type == ContentType.IMAGE -> {
          AsyncImage(
            model = att.file,
            contentDescription = att.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 420.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFFF1F5F9))
              .clickable { showFullscreen = true }
          )
          Text(
            text = "Tap the picture to view full screen",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
        type == ContentType.VIDEO -> {
          InlineVideoPlayer(path = att.path)
        }
        type == ContentType.PDF -> {
          PdfPagesViewer(file = att.file)
        }
        else -> {
          Text(
            text = "Tap Open File to view this document, or Download to save it.",
            fontSize = 13.sp,
            color = TextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = { AttachmentUtils.open(context, att) },
          colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Text("Open", fontSize = 12.sp)
        }
        OutlinedButton(
          onClick = { AttachmentUtils.shareOne(context, att) },
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Share", fontSize = 12.sp)
        }
        OutlinedButton(
          onClick = { AttachmentUtils.downloadOne(context, att) },
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Download", fontSize = 12.sp)
        }
      }
    }
  }

  if (showFullscreen) {
    FullscreenPicture(file = att.file, onDismiss = { showFullscreen = false })
  }
}

@Composable
fun FullscreenPicture(file: File, onDismiss: () -> Unit) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
      Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
          model = file,
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize()
        )
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp)
            .background(Color(0x66000000), RoundedCornerShape(24.dp))
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
      }
    }
  }
}

@Composable
fun InlineVideoPlayer(path: String) {
  var videoView by remember { mutableStateOf<VideoView?>(null) }

  DisposableEffect(path) {
    onDispose {
      try {
        videoView?.stopPlayback()
      } catch (_: Exception) {
      }
    }
  }

  Column {
    AndroidView(
      factory = { ctx ->
        VideoView(ctx).apply {
          val controller = MediaController(ctx)
          controller.setAnchorView(this)
          setMediaController(controller)
          setVideoPath(path)
          // show the first frame as a preview
          seekTo(1)
          videoView = this
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .height(230.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Color.Black)
    )
    Text(
      text = "Tap the video to play, pause or move forward",
      fontSize = 11.sp,
      color = TextSecondary,
      modifier = Modifier.padding(top = 4.dp)
    )
  }
}

@Composable
fun PdfPagesViewer(file: File) {
  var pageIndex by remember { mutableIntStateOf(0) }
  var pageCount by remember { mutableIntStateOf(0) }
  var bitmap by remember { mutableStateOf<Bitmap?>(null) }
  var failed by remember { mutableStateOf(false) }

  LaunchedEffect(file.path, pageIndex) {
    val result = withContext(Dispatchers.IO) {
      var descriptor: ParcelFileDescriptor? = null
      var renderer: PdfRenderer? = null
      try {
        descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(descriptor)
        val count = renderer.pageCount
        if (count <= 0) {
          null
        } else {
          val index = pageIndex.coerceIn(0, count - 1)
          val page = renderer.openPage(index)
          try {
            val width = 1080
            val height = (width.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(android.graphics.Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            Pair(count, bmp)
          } finally {
            page.close()
          }
        }
      } catch (e: Exception) {
        null
      } finally {
        try { renderer?.close() } catch (_: Exception) { }
        try { descriptor?.close() } catch (_: Exception) { }
      }
    }
    if (result == null) {
      failed = true
    } else {
      failed = false
      pageCount = result.first
      bitmap = result.second
    }
  }

  Column {
    if (failed) {
      Text(
        text = "Preview not available. Tap Open to read this PDF.",
        fontSize = 13.sp,
        color = TextSecondary
      )
    } else {
      val current = bitmap
      if (current != null) {
        Image(
          bitmap = current.asImageBitmap(),
          contentDescription = "PDF page ${pageIndex + 1}",
          contentScale = ContentScale.FillWidth,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
        )
      } else {
        Text("Loading PDF...", fontSize = 13.sp, color = TextSecondary)
      }

      if (pageCount > 1) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { if (pageIndex > 0) pageIndex -= 1 },
            enabled = pageIndex > 0
          ) {
            Icon(Icons.Default.NavigateBefore, contentDescription = "Previous page", tint = SolarBlue)
          }
          Text(
            text = "Page ${pageIndex + 1} of $pageCount",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
          )
          IconButton(
            onClick = { if (pageIndex < pageCount - 1) pageIndex += 1 },
            enabled = pageIndex < pageCount - 1
          ) {
            Icon(Icons.Default.NavigateNext, contentDescription = "Next page", tint = SolarBlue)
          }
        }
      }
    }
  }
}
