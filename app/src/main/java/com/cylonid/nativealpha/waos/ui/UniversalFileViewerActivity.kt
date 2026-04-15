package com.cylonid.nativealpha.waos.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.waos.util.WaosConstants
import java.io.File
import java.text.DecimalFormat
import java.util.zip.ZipFile

class UniversalFileViewerActivity : AppCompatActivity() {
    private lateinit var textViewer: TextView
    private lateinit var imageViewer: ImageView
    private lateinit var videoViewer: VideoView
    private lateinit var errorState: LinearLayout
    private lateinit var errorMessage: TextView
    private lateinit var fileInfoBar: LinearLayout
    private lateinit var fileNameText: TextView
    private lateinit var fileSizeText: TextView
    private lateinit var fileActionBar: LinearLayout
    private lateinit var shareButton: Button
    private lateinit var copyPathButton: Button
    private lateinit var fullscreenButton: Button
    private lateinit var openOtherButton: Button
    private lateinit var openExternalButton: Button

    private var currentFile: File? = null
    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waos_file_viewer)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        textViewer = findViewById(R.id.file_text)
        imageViewer = findViewById(R.id.file_image)
        videoViewer = findViewById(R.id.file_video)
        errorState = findViewById(R.id.file_error_state)
        errorMessage = findViewById(R.id.file_error_message)
        fileInfoBar = findViewById(R.id.file_info_bar)
        fileNameText = findViewById(R.id.file_name_text)
        fileSizeText = findViewById(R.id.file_size_text)
        fileActionBar = findViewById(R.id.file_action_bar)
        shareButton = findViewById(R.id.button_share_file)
        copyPathButton = findViewById(R.id.button_copy_path)
        fullscreenButton = findViewById(R.id.button_fullscreen)
        openOtherButton = findViewById(R.id.button_open_other_app)
        openExternalButton = findViewById(R.id.button_open_external)

        shareButton.setOnClickListener { hapticTap(); shareCurrentFile() }
        copyPathButton.setOnClickListener { hapticTap(); copyFilePathToClipboard() }
        fullscreenButton.setOnClickListener { hapticTap(); toggleFullscreen() }
        openOtherButton.setOnClickListener { hapticTap(); openExternalCurrentFile() }
        openExternalButton.setOnClickListener { hapticTap(); openExternalCurrentFile() }

        val filePath = intent.getStringExtra(WaosConstants.EXTRA_FILE_PATH) ?: ""
        loadFile(filePath)
    }

    private fun hapticTap() {
        try {
            val v = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(30)
            }
        } catch (_: Exception) {}
    }

    private fun loadFile(filePath: String) {
        if (filePath.isBlank()) {
            showError("No file path provided")
            return
        }
        val file = File(filePath)
        if (!file.exists()) {
            showError("File not found:\n${file.name}")
            return
        }

        currentFile = file
        showFileInfo(file)

        val extension = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"

        hideAllViewers()

        when {
            mimeType.startsWith("image/") -> showImage(file)
            mimeType.startsWith("video/") -> showVideo(file)
            mimeType.startsWith("audio/") -> playAudio(file)
            extension == "pdf" -> showPdf(file)
            extension in listOf("txt", "log", "csv", "json", "xml", "md", "kt", "java", "py", "js", "ts", "html", "htm", "css") -> showText(file)
            extension == "zip" -> showZipInfo(file)
            extension == "apk" -> showApkInfo(file)
            else -> openExternal(file)
        }
    }

    private fun showFileInfo(file: File) {
        fileInfoBar.visibility = View.VISIBLE
        fileNameText.text = file.name
        fileSizeText.text = formatFileSize(file.length())
        fileActionBar.visibility = View.VISIBLE
    }

    private fun hideAllViewers() {
        textViewer.visibility = View.GONE
        imageViewer.visibility = View.GONE
        videoViewer.visibility = View.GONE
        errorState.visibility = View.GONE
    }

    private fun showImage(file: File) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap == null) {
            openExternal(file)
            return
        }
        imageViewer.visibility = View.VISIBLE
        imageViewer.setImageBitmap(bitmap)
    }

    private fun showVideo(file: File) {
        videoViewer.visibility = View.VISIBLE
        videoViewer.setVideoURI(Uri.fromFile(file))
        val mc = MediaController(this)
        mc.setAnchorView(videoViewer)
        videoViewer.setMediaController(mc)
        videoViewer.setOnPreparedListener { mp: MediaPlayer ->
            mp.isLooping = false
            videoViewer.start()
        }
        videoViewer.setOnErrorListener { _, _, _ ->
            showError("Cannot play this video file.\nTry opening with an external app.")
            true
        }
    }

    private fun playAudio(file: File) {
        videoViewer.visibility = View.VISIBLE
        videoViewer.setVideoURI(Uri.fromFile(file))
        val mc = MediaController(this)
        mc.setAnchorView(videoViewer)
        videoViewer.setMediaController(mc)
        videoViewer.setOnPreparedListener { mp: MediaPlayer ->
            mp.start()
            Toast.makeText(this, "Playing: ${file.name}", Toast.LENGTH_SHORT).show()
        }
        videoViewer.setOnErrorListener { _, _, _ ->
            showError("Cannot play this audio file.\nTry opening with an external app.")
            true
        }
    }

    private fun showPdf(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(descriptor)
                if (renderer.pageCount > 0) {
                    val page = renderer.openPage(0)
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderer.close()
                    imageViewer.visibility = View.VISIBLE
                    imageViewer.setImageBitmap(bitmap)
                    Toast.makeText(this, "PDF - Page 1 of ${renderer.pageCount} (open in PDF app for full navigation)", Toast.LENGTH_LONG).show()
                } else {
                    openExternal(file)
                }
            } catch (e: Exception) {
                openExternal(file)
            }
        } else {
            openExternal(file)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showText(file: File) {
        val text = try {
            val content = file.readText(Charsets.UTF_8)
            if (content.length > 100_000) content.take(100_000) + "\n\n[File truncated - too large to display fully]"
            else content
        } catch (e: Exception) {
            showError("Cannot read file: ${e.message}")
            return
        }
        textViewer.visibility = View.VISIBLE
        textViewer.text = text
    }

    @SuppressLint("SetTextI18n")
    private fun showZipInfo(file: File) {
        val sb = StringBuilder()
        sb.appendLine("ZIP Archive: ${file.name}")
        sb.appendLine("Size: ${formatFileSize(file.length())}")
        sb.appendLine()
        try {
            ZipFile(file).use { zip ->
                val entries = zip.entries().toList()
                sb.appendLine("Contains ${entries.size} entries:")
                sb.appendLine()
                entries.take(200).forEach { entry ->
                    val size = if (entry.size > 0) " (${formatFileSize(entry.size)})" else ""
                    val dir = if (entry.isDirectory) "/" else ""
                    sb.appendLine("  ${entry.name}$dir$size")
                }
                if (entries.size > 200) {
                    sb.appendLine("  ... and ${entries.size - 200} more entries")
                }
            }
        } catch (e: Exception) {
            sb.appendLine("Cannot read ZIP: ${e.message}")
        }
        textViewer.visibility = View.VISIBLE
        textViewer.text = sb.toString()

        AlertDialog.Builder(this)
            .setTitle("ZIP Archive")
            .setMessage("Viewing ZIP contents. To extract files, open with a file manager app.")
            .setPositiveButton("Open With") { _, _ -> openExternalCurrentFile() }
            .setNegativeButton("View Contents", null)
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun showApkInfo(file: File) {
        val sb = StringBuilder()
        sb.appendLine("APK File: ${file.name}")
        sb.appendLine("Size: ${formatFileSize(file.length())}")
        sb.appendLine()
        try {
            val pm = packageManager
            val info = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_ACTIVITIES)
            if (info != null) {
                info.applicationInfo?.sourceDir = file.absolutePath
                info.applicationInfo?.publicSourceDir = file.absolutePath
                val appName = try { pm.getApplicationLabel(info.applicationInfo!!).toString() } catch (_: Exception) { "Unknown" }
                val icon = try { pm.getApplicationIcon(info.applicationInfo!!) } catch (_: Exception) { null }
                sb.appendLine("App Name: $appName")
                sb.appendLine("Package: ${info.packageName}")
                sb.appendLine("Version: ${info.versionName} (${info.longVersionCode})")
                sb.appendLine("Min SDK: ${info.applicationInfo?.minSdkVersion ?: "Unknown"}")
                sb.appendLine()
                sb.appendLine("Activities: ${info.activities?.size ?: 0}")
                info.activities?.take(20)?.forEach { activity ->
                    sb.appendLine("  - ${activity.name}")
                }
                textViewer.visibility = View.VISIBLE
                textViewer.text = sb.toString()
                if (icon != null) {
                    imageViewer.visibility = View.VISIBLE
                    imageViewer.setImageDrawable(icon)
                }
            } else {
                sb.appendLine("Could not parse APK metadata.")
                textViewer.visibility = View.VISIBLE
                textViewer.text = sb.toString()
            }
        } catch (e: Exception) {
            sb.appendLine("Error reading APK: ${e.message}")
            textViewer.visibility = View.VISIBLE
            textViewer.text = sb.toString()
        }

        AlertDialog.Builder(this)
            .setTitle("Install APK?")
            .setMessage("Do you want to install this APK file?")
            .setPositiveButton("Install") { _, _ -> openExternal(file) }
            .setNegativeButton("View Info Only", null)
            .show()
    }

    private fun openExternal(file: File) {
        val uri: Uri = try {
            FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            showError("No app found to open this file type (${file.extension.uppercase()}).\n\nInstall a compatible app from the Play Store.")
        }
    }

    private fun openExternalCurrentFile() {
        val file = currentFile ?: return
        openExternal(file)
    }

    private fun shareCurrentFile() {
        val file = currentFile ?: return
        val uri: Uri = try {
            FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension) ?: "*/*"
        val intent = Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Share ${file.name}"))
    }

    private fun copyFilePathToClipboard() {
        val file = currentFile ?: return
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("File path", file.absolutePath)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(this, "File path copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            supportActionBar?.hide()
            fileInfoBar.visibility = View.GONE
            fileActionBar.visibility = View.GONE
            fullscreenButton.text = "Exit Full Screen"
            Toast.makeText(this, "Tap 'Exit Full Screen' button to exit", Toast.LENGTH_SHORT).show()
            fileActionBar.visibility = View.VISIBLE
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            supportActionBar?.show()
            fileInfoBar.visibility = View.VISIBLE
            fileActionBar.visibility = View.VISIBLE
            fullscreenButton.text = "Full Screen"
        }
    }

    private fun showError(message: String) {
        hideAllViewers()
        errorState.visibility = View.VISIBLE
        errorMessage.text = message
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val df = DecimalFormat("#,##0.#")
        return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
