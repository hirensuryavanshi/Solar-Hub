package com.example.solarhub.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.solarhub.model.ContentItem
import com.example.solarhub.model.ContentType
import java.io.File
import java.io.OutputStream

/**
 * Helpers to attach real files (PDF, photo, video, Word, Excel, PowerPoint...) to content,
 * and to open / share / download them.
 */
object AttachmentUtils {

  data class PickedFile(
    val path: String,
    val name: String,
    val size: Long,
    val mime: String?
  )

  /** Copies the file chosen by the admin into the app's private storage. Call from a background thread. */
  fun copyToInternal(context: Context, uri: Uri): PickedFile? {
    return try {
      val resolver = context.contentResolver
      var name = "attachment"
      var size = 0L
      resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
          if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
          if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
        }
      }
      val safeName = name.replace(Regex("[^A-Za-z0-9._ -]"), "_")
      val folder = File(File(context.filesDir, "attachments"), System.currentTimeMillis().toString())
      folder.mkdirs()
      val outFile = File(folder, safeName)
      val input = resolver.openInputStream(uri) ?: return null
      input.use { source ->
        outFile.outputStream().use { target -> source.copyTo(target) }
      }
      if (size <= 0L) size = outFile.length()
      PickedFile(outFile.absolutePath, name, size, resolver.getType(uri))
    } catch (e: Exception) {
      null
    }
  }

  /** Guess the content type from the file name / mime type. */
  fun detectType(name: String, mime: String?): ContentType? {
    val ext = name.substringAfterLast('.', "").lowercase()
    val m = mime ?: ""
    return when {
      m.startsWith("image/") || ext in listOf("png", "jpg", "jpeg", "webp", "gif", "bmp") -> ContentType.IMAGE
      m.startsWith("video/") || ext in listOf("mp4", "mkv", "mov", "avi", "3gp", "webm") -> ContentType.VIDEO
      m == "application/pdf" || ext == "pdf" -> ContentType.PDF
      ext in listOf("doc", "docx") -> ContentType.WORD
      ext in listOf("xls", "xlsx", "csv") -> ContentType.EXCEL
      ext in listOf("ppt", "pptx") -> ContentType.PPT
      else -> ContentType.OTHER
    }
  }

  fun formatSize(bytes: Long): String {
    return when {
      bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
      bytes >= 1024 -> String.format("%.0f KB", bytes.toDouble() / 1024)
      else -> "$bytes B"
    }
  }

  /** Model object for Coil's AsyncImage (local file or web address). */
  fun imageModel(path: String?): Any? {
    if (path.isNullOrBlank()) return null
    return if (path.startsWith("http")) path else File(path)
  }

  private fun sourceFile(content: ContentItem): File? {
    val path = content.attachmentUrlOrPath
    if (path.isNullOrBlank() || path.startsWith("http")) return null
    val file = File(path)
    return if (file.exists()) file else null
  }

  private fun mimeFor(content: ContentItem): String {
    val saved = content.attachmentMimeType
    if (!saved.isNullOrBlank() && saved != "application/octet-stream") return saved
    val ext = (content.attachmentName ?: content.attachmentUrlOrPath ?: "")
      .substringAfterLast('.', "")
      .lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
  }

  private fun textFor(content: ContentItem): String {
    return "SOLAR HUB - SOLAR MANUFACTURING KNOWLEDGE PLATFORM\n" +
      "Title: ${content.title}\n" +
      "Category: ${content.category}\n" +
      "Description: ${content.description}\n\n" +
      "Content Body:\n${content.contentBody}\n"
  }

  private fun uriFor(context: Context, file: File): Uri {
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  }

  fun open(context: Context, content: ContentItem) {
    val file = sourceFile(content)
    if (file == null) {
      Toast.makeText(context, "No file attached to open", Toast.LENGTH_SHORT).show()
      return
    }
    try {
      val uri = uriFor(context, file)
      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeFor(content))
        clipData = ClipData.newRawUri("file", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      val chooser = Intent.createChooser(intent, "Open with").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    } catch (e: ActivityNotFoundException) {
      Toast.makeText(context, "No app found to open this file. Install a viewer app.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
      Toast.makeText(context, "Could not open the file", Toast.LENGTH_SHORT).show()
    }
  }

  fun share(context: Context, content: ContentItem) {
    try {
      val file = sourceFile(content)
      val intent = Intent(Intent.ACTION_SEND)
      if (file != null) {
        val uri = uriFor(context, file)
        intent.type = mimeFor(content)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.putExtra(Intent.EXTRA_SUBJECT, content.title)
        intent.putExtra(Intent.EXTRA_TEXT, content.title)
        intent.clipData = ClipData.newRawUri("file", uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      } else {
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, content.title)
        intent.putExtra(Intent.EXTRA_TEXT, textFor(content))
      }
      val chooser = Intent.createChooser(intent, "Share via").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    } catch (e: Exception) {
      Toast.makeText(context, "Could not share", Toast.LENGTH_SHORT).show()
    }
  }

  /** Saves the attached file (or the text content) to the phone's Downloads folder. */
  fun download(context: Context, content: ContentItem) {
    try {
      val source = sourceFile(content)
      val name = if (source != null) {
        content.attachmentName?.takeIf { it.isNotBlank() } ?: source.name
      } else {
        content.title.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".txt"
      }
      val mime = if (source != null) mimeFor(content) else "text/plain"
      val writer: (OutputStream) -> Unit = { out ->
        if (source != null) {
          source.inputStream().use { it.copyTo(out) }
        } else {
          out.write(textFor(content).toByteArray())
        }
      }

      val where: String
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
          put(MediaStore.Downloads.DISPLAY_NAME, name)
          put(MediaStore.Downloads.MIME_TYPE, mime)
          put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
          ?: throw IllegalStateException("Cannot create download")
        val stream = context.contentResolver.openOutputStream(uri)
          ?: throw IllegalStateException("Cannot open download")
        stream.use(writer)
        where = "Downloads folder"
      } else {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        dir.mkdirs()
        File(dir, name).outputStream().use(writer)
        where = "app downloads folder"
      }
      Toast.makeText(context, "Saved $name to $where", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
      Toast.makeText(context, "Could not save the file", Toast.LENGTH_SHORT).show()
    }
  }
}
