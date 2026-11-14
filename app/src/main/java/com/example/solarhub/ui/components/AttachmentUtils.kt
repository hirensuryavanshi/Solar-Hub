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
 * Helpers for real file attachments (photos, videos, PDF, Word, Excel, PowerPoint...).
 * One post can have many files. They are stored in the existing database columns,
 * separated by "|", so no database change is needed.
 */
object AttachmentUtils {

  private const val SEP = "|"

  data class Attachment(
    val path: String,
    val name: String,
    val mime: String?
  ) {
    val file: File get() = File(path)
    val type: ContentType get() = typeOf(name, mime)
  }

  data class Encoded(
    val paths: String?,
    val names: String?,
    val mimes: String?,
    val totalSize: Long
  )

  // ---------- Encoding of many files into the existing columns ----------

  fun parse(content: ContentItem): List<Attachment> {
    val paths = content.attachmentUrlOrPath
      ?.split(SEP)
      ?.map { it.trim() }
      ?.filter { it.isNotBlank() }
      ?: return emptyList()
    val names = content.attachmentName?.split(SEP) ?: emptyList()
    val mimes = content.attachmentMimeType?.split(SEP) ?: emptyList()
    return paths.mapIndexed { index, path ->
      Attachment(
        path = path,
        name = names.getOrNull(index)?.takeIf { it.isNotBlank() } ?: path.substringAfterLast('/'),
        mime = mimes.getOrNull(index)?.takeIf { it.isNotBlank() }
      )
    }
  }

  fun encode(list: List<Attachment>): Encoded {
    if (list.isEmpty()) return Encoded(null, null, null, 0L)
    return Encoded(
      paths = list.joinToString(SEP) { it.path },
      names = list.joinToString(SEP) { it.name },
      mimes = list.joinToString(SEP) { it.mime ?: "" },
      totalSize = list.sumOf { it.file.length() }
    )
  }

  // ---------- Picking files ----------

  /** Copies a file chosen by the admin into the app's private storage. Call from a background thread. */
  fun copyToInternal(context: Context, uri: Uri): Attachment? {
    return try {
      val resolver = context.contentResolver
      var name = "attachment"
      resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
          name = cursor.getString(nameIndex) ?: name
        }
      }
      val safeName = name.replace(Regex("[^A-Za-z0-9._ -]"), "_")
      val folder = File(File(context.filesDir, "attachments"), System.nanoTime().toString())
      folder.mkdirs()
      val outFile = File(folder, safeName)
      val input = resolver.openInputStream(uri) ?: return null
      input.use { source ->
        outFile.outputStream().use { target -> source.copyTo(target) }
      }
      Attachment(outFile.absolutePath, safeName, resolver.getType(uri))
    } catch (e: Exception) {
      null
    }
  }

  fun typeOf(name: String, mime: String?): ContentType {
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

  /** If no file is attached, a "file" type makes no sense - fall back to plain text. */
  fun effectiveType(selected: ContentType, hasFiles: Boolean): ContentType {
    if (hasFiles) return selected
    return when (selected) {
      ContentType.IMAGE, ContentType.VIDEO, ContentType.PDF,
      ContentType.WORD, ContentType.EXCEL, ContentType.PPT, ContentType.OTHER -> ContentType.TEXT
      else -> selected
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

  // ---------- Internals ----------

  private fun mimeOf(att: Attachment): String {
    val saved = att.mime
    if (!saved.isNullOrBlank() && saved != "application/octet-stream") return saved
    val ext = att.name.substringAfterLast('.', "").lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
  }

  private fun existing(content: ContentItem): List<Attachment> {
    return parse(content).filter { it.file.exists() }
  }

  private fun textFor(content: ContentItem): String {
    return "SOLAR HUB - SOLAR MANUFACTURING KNOWLEDGE PLATFORM\n" +
      "Title: ${content.title}\n" +
      "Description: ${content.description}\n\n" +
      "Content Body:\n${content.contentBody}\n"
  }

  private fun uriFor(context: Context, file: File): Uri {
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  }

  private fun saveToDownloads(context: Context, name: String, mime: String, writer: (OutputStream) -> Unit) {
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
    } else {
      val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
      dir.mkdirs()
      File(dir, name).outputStream().use(writer)
    }
  }

  private fun copyWriter(att: Attachment): (OutputStream) -> Unit = { out ->
    att.file.inputStream().use { it.copyTo(out) }
  }

  // ---------- Actions ----------

  fun open(context: Context, att: Attachment) {
    if (!att.file.exists()) {
      Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
      return
    }
    try {
      val uri = uriFor(context, att.file)
      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeOf(att))
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

  fun shareOne(context: Context, att: Attachment) {
    if (!att.file.exists()) {
      Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
      return
    }
    try {
      val uri = uriFor(context, att.file)
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeOf(att)
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, att.name)
        clipData = ClipData.newRawUri("file", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(
        Intent.createChooser(intent, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    } catch (e: Exception) {
      Toast.makeText(context, "Could not share", Toast.LENGTH_SHORT).show()
    }
  }

  /** Shares the whole post: all its files, or the text if there are no files. */
  fun share(context: Context, content: ContentItem) {
    try {
      val files = existing(content)
      val intent: Intent
      if (files.isEmpty()) {
        intent = Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_SUBJECT, content.title)
          putExtra(Intent.EXTRA_TEXT, textFor(content))
        }
      } else if (files.size == 1) {
        val uri = uriFor(context, files[0].file)
        intent = Intent(Intent.ACTION_SEND).apply {
          type = mimeOf(files[0])
          putExtra(Intent.EXTRA_STREAM, uri)
          putExtra(Intent.EXTRA_SUBJECT, content.title)
          putExtra(Intent.EXTRA_TEXT, content.title)
          clipData = ClipData.newRawUri("file", uri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      } else {
        val uris = ArrayList<Uri>(files.map { uriFor(context, it.file) })
        val clip = ClipData.newRawUri("files", uris[0])
        for (i in 1 until uris.size) clip.addItem(ClipData.Item(uris[i]))
        intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
          type = "*/*"
          putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
          putExtra(Intent.EXTRA_SUBJECT, content.title)
          putExtra(Intent.EXTRA_TEXT, content.title)
          clipData = clip
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      }
      context.startActivity(
        Intent.createChooser(intent, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    } catch (e: Exception) {
      Toast.makeText(context, "Could not share", Toast.LENGTH_SHORT).show()
    }
  }

  fun downloadOne(context: Context, att: Attachment) {
    if (!att.file.exists()) {
      Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
      return
    }
    try {
      saveToDownloads(context, att.name, mimeOf(att), copyWriter(att))
      Toast.makeText(context, "Saved ${att.name} to Downloads", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
      Toast.makeText(context, "Could not save the file", Toast.LENGTH_SHORT).show()
    }
  }

  /** Downloads all files of a post (or the text if there are no files) into the Downloads folder. */
  fun download(context: Context, content: ContentItem) {
    try {
      val files = existing(content)
      if (files.isEmpty()) {
        val name = content.title.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".txt"
        saveToDownloads(context, name, "text/plain") { out ->
          out.write(textFor(content).toByteArray())
        }
        Toast.makeText(context, "Saved $name to Downloads", Toast.LENGTH_LONG).show()
      } else {
        files.forEach { saveToDownloads(context, it.name, mimeOf(it), copyWriter(it)) }
        val message = if (files.size == 1) "Saved ${files[0].name} to Downloads" else "Saved ${files.size} files to Downloads"
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
      }
    } catch (e: Exception) {
      Toast.makeText(context, "Could not save the file", Toast.LENGTH_SHORT).show()
    }
  }
}
