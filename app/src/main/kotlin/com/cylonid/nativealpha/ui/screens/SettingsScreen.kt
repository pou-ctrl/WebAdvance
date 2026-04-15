package com.cylonid.nativealpha.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cylonid.nativealpha.crash.CrashEntry
import com.cylonid.nativealpha.crash.CrashLogStorage
import com.cylonid.nativealpha.ui.theme.*
import com.cylonid.nativealpha.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(viewModel.lastExportMessage) {
        if (viewModel.lastExportMessage.isNotEmpty()) {
            Toast.makeText(context, viewModel.lastExportMessage, Toast.LENGTH_LONG).show()
            viewModel.clearExportMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(CardSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Rounded.ArrowBack, "Back", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Settings", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Customize your WAOS experience", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                SettingsSectionCard(title = "Appearance", icon = Icons.Rounded.Palette) {
                    var themeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = themeExpanded, onExpandedChange = { themeExpanded = it }) {
                        OutlinedTextField(
                            value = viewModel.appTheme,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("App Theme", color = TextSecondary, fontSize = 12.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(themeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(14.dp),
                            colors = settingsTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false },
                            modifier = Modifier.background(CardSurface)
                        ) {
                            listOf("System", "Dark", "Light").forEach { theme ->
                                DropdownMenuItem(
                                    text = { Text(theme, color = TextPrimary) },
                                    onClick = {
                                        viewModel.updateAppTheme(theme)
                                        themeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                SettingsSectionCard(title = "Dashboard", icon = Icons.Rounded.Dashboard) {
                    SettingsInfoRow("Grid Columns", "2 columns (auto-adaptive)")
                    Spacer(Modifier.height(8.dp))
                    SettingsToggleRow("Show Category Chips", Icons.Rounded.Category, viewModel.showCategoryChips) {
                        viewModel.updateShowCategoryChips(it)
                    }
                    SettingsToggleRow("Show Status Indicators", Icons.Rounded.Circle, viewModel.showStatusIndicators) {
                        viewModel.updateShowStatusIndicators(it)
                    }
                    SettingsToggleRow("Animated Cards", Icons.Rounded.AutoAwesome, viewModel.animatedCards) {
                        viewModel.updateAnimatedCards(it)
                    }
                }

                SettingsSectionCard(title = "Notifications", icon = Icons.Rounded.Notifications) {
                    SettingsToggleRow(
                        "Global Notifications",
                        Icons.Rounded.NotificationsActive,
                        viewModel.globalNotificationsEnabled
                    ) {
                        viewModel.updateGlobalNotificationsEnabled(it)
                    }
                    SettingsToggleRow(
                        "Show Badge Count",
                        Icons.Rounded.Label,
                        viewModel.showBadgeCount && viewModel.globalNotificationsEnabled
                    ) {
                        if (viewModel.globalNotificationsEnabled) viewModel.updateShowBadgeCount(it)
                    }
                }

                SettingsSectionCard(title = "Floating Windows", icon = Icons.Rounded.OpenInNew) {
                    SettingsToggleRow(
                        "Enable Floating Windows",
                        Icons.Rounded.OpenWith,
                        viewModel.floatingWindowsEnabled
                    ) {
                        viewModel.updateFloatingWindowsEnabled(it)
                    }
                    AnimatedVisibility(visible = viewModel.floatingWindowsEnabled) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            SettingsInfoRow(
                                "Max Windows",
                                "${viewModel.maxFloatingWindows} simultaneous windows"
                            )
                        }
                    }
                }

                SettingsSectionCard(title = "Privacy & Security", icon = Icons.Rounded.Security) {
                    SettingsToggleRow(
                        "Global Credential Vault",
                        Icons.Rounded.Lock,
                        viewModel.globalVaultEnabled
                    ) {
                        viewModel.updateGlobalVaultEnabled(it)
                    }
                    SettingsToggleRow(
                        "Clipboard Manager",
                        Icons.Rounded.ContentPaste,
                        viewModel.globalClipboardEnabled
                    ) {
                        viewModel.updateGlobalClipboardEnabled(it)
                    }
                    SettingsInfoRow("Auto-lock Timeout", "5 minutes")
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { navController.navigate("settings/permissions") },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary.copy(0.12f),
                            contentColor = CyanPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Security, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("App Permissions", fontWeight = FontWeight.SemiBold)
                    }
                }

                SettingsSectionCard(title = "WebView Browsing", icon = Icons.Rounded.Speed) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Speed,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Auto-Scroll Speed", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Box(
                                modifier = Modifier
                                    .background(CyanPrimary.copy(0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "${viewModel.autoScrollSpeed}",
                                    color = CyanPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Slider(
                            value = viewModel.autoScrollSpeed.toFloat(),
                            onValueChange = { viewModel.updateAutoScrollSpeed(it.toInt()) },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                thumbColor = CyanPrimary,
                                activeTrackColor = CyanPrimary,
                                inactiveTrackColor = CardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Slow", color = TextMuted, fontSize = 10.sp)
                            Text("Fast", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }

                SettingsSectionCard(title = "Developer", icon = Icons.Rounded.Code) {
                    SettingsToggleRow(
                        "Developer Mode",
                        Icons.Rounded.BugReport,
                        viewModel.developerModeEnabled
                    ) {
                        viewModel.updateDeveloperModeEnabled(it)
                    }
                    SettingsInfoRow(
                        "JS Console",
                        if (viewModel.developerModeEnabled) "Enabled in WebView" else "Disabled"
                    )
                }

                SettingsSectionCard(title = "Backup & Restore", icon = Icons.Rounded.Backup) {
                    val exportFolderLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocumentTree(),
                        onResult = { uri: Uri? ->
                            uri?.let {
                                context.contentResolver.takePersistableUriPermission(
                                    it,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                )
                                viewModel.exportDataToFolder(it)
                            }
                        }
                    )

                    val importFileLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument(),
                        onResult = { uri: Uri? ->
                            uri?.let { viewModel.importDataFromUri(it) }
                        }
                    )

                    Button(
                        onClick = { exportFolderLauncher.launch(null) },
                        enabled = !viewModel.isExporting,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary.copy(0.12f),
                            contentColor = CyanPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (viewModel.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = CyanPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Exporting...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Rounded.CloudUpload, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export to Folder", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { importFileLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Import from .waos", fontWeight = FontWeight.SemiBold)
                    }
                    if (viewModel.lastExportMessage.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            viewModel.lastExportMessage,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                CrashLogSection(context)

                SettingsSectionCard(title = "About", icon = Icons.Rounded.Info) {
                    SettingsInfoRow("App", "WAOS - Web App Operating System")
                    SettingsInfoRow("Version", "1.5.2")
                    SettingsInfoRow("Engine", "Android System WebView")
                    SettingsInfoRow("Architecture", "Multi-process Sandboxed WebView")
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CrashLogSection(context: Context) {
    val crashes = remember { CrashLogStorage.getCrashLogs(context) }
    var expanded by remember { mutableStateOf(false) }
    var selectedCrash by remember { mutableStateOf<CrashEntry?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(
                1.dp,
                if (crashes.isNotEmpty()) ErrorRed.copy(0.3f) else CardBorder,
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (crashes.isNotEmpty()) ErrorRed.copy(0.15f) else VioletSecondary.copy(0.12f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.BugReport,
                    null,
                    tint = if (crashes.isNotEmpty()) ErrorRed else VioletSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Crash Logs", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (crashes.isEmpty()) "No crashes recorded" else "${crashes.size} crash(es) saved",
                    color = if (crashes.isNotEmpty()) ErrorRed else TextMuted,
                    fontSize = 11.sp
                )
            }
            Icon(
                if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn()
        ) {
            Column {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(Modifier.height(12.dp))

                if (crashes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No crashes recorded yet", color = TextMuted, fontSize = 13.sp)
                    }
                } else {
                    crashes.take(10).forEach { crash ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(BgMedium)
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                .clickable { selectedCrash = if (selectedCrash == crash) null else crash }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(ErrorRed, androidx.compose.foundation.shape.CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    crash.timestamp,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("v${crash.appVersion}", color = TextMuted, fontSize = 10.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                crash.message,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                maxLines = if (selectedCrash == crash) Int.MAX_VALUE else 2
                            )

                            AnimatedVisibility(
                                visible = selectedCrash == crash,
                                enter = expandVertically() + fadeIn()
                            ) {
                                Column {
                                    Spacer(Modifier.height(8.dp))
                                    Text("Thread: ${crash.threadName}", color = TextMuted, fontSize = 11.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0A0F1A))
                                            .padding(8.dp)
                                            .horizontalScroll(rememberScrollState())
                                    ) {
                                        Text(
                                            crash.stackTrace.take(800) + if (crash.stackTrace.length > 800) "\n..." else "",
                                            color = Color(0xFFFF7777),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 14.sp
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    val fullReport = buildString {
                                        appendLine("=== WAOS Crash Report ===")
                                        appendLine("Time: ${crash.timestamp}")
                                        appendLine("Version: ${crash.appVersion}")
                                        appendLine("Thread: ${crash.threadName}")
                                        appendLine("Error: ${crash.message}")
                                        appendLine()
                                        appendLine("Stack Trace:")
                                        append(crash.stackTrace)
                                    }
                                    Button(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("WAOS Crash", fullReport))
                                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ErrorRed.copy(0.15f),
                                            contentColor = ErrorRed
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Copy Report", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(0.3f))
                    ) {
                        Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear All Crash Logs", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = CardSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Clear Crash Logs?", fontWeight = FontWeight.Bold) },
            text = { Text("All ${crashes.size} saved crash logs will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    CrashLogStorage.clearLogs(context)
                    showClearConfirm = false
                }) { Text("Clear", color = ErrorRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(VioletSecondary.copy(0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = VioletSecondary, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BgDeep,
                checkedTrackColor = CyanPrimary,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CardBorder
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun settingsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VioletSecondary.copy(0.7f),
    unfocusedBorderColor = CardBorder,
    focusedContainerColor = BgMedium,
    unfocusedContainerColor = BgMedium,
    cursorColor = VioletSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
