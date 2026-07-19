package com.aniauth.authenticator.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniauth.authenticator.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    onExport: (Boolean) -> Unit,
    onImport: () -> Unit,
    isLocked: Boolean,
    onToggleLock: (Boolean) -> Unit,
    onShowManual: () -> Unit,
    themeSetting: String,
    onThemeChange: (String) -> Unit,
    isWatchConnected: Boolean = false,
    onSyncToWatch: () -> Unit = {}
) {
    val context = LocalContext.current
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var maxAttempts by remember {
        mutableStateOf(
            context.getSharedPreferences("ani_auth_prefs", android.content.Context.MODE_PRIVATE)
                .getInt("watch_max_failed_attempts", 3)
        )
    }
    var showAttemptsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PurpleAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // User Guide Section
            item {
                SettingsSection(title = "Help") {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = "User Manual",
                        subtitle = "Learn how to use aniAuth controls",
                        onClick = onShowManual
                    )
                }
            }

            // Data Section
            item {
                SettingsSection(title = "Data & Backup") {
                    SettingsItem(
                        icon = Icons.Default.UploadFile,
                        title = "Import Backup",
                        subtitle = "Import from Bitwarden or aniAuth JSON",
                        onClick = onImport
                    )
                    SettingsItem(
                        icon = Icons.Default.DownloadForOffline,
                        title = "Export Backup",
                        subtitle = "Securely save your accounts to a file",
                        onClick = { showExportDialog = true }
                    )
                }
            }

            // Wear OS Watch Section (Only shown if connected)
            if (isWatchConnected) {
                item {
                    SettingsSection(title = "Wear OS Watch") {
                        SettingsItem(
                            icon = Icons.Default.Watch,
                            title = "Sync to Watch",
                            subtitle = "Securely sync accounts to Wear OS",
                            onClick = onSyncToWatch
                        )
                        SettingsItem(
                            icon = Icons.Default.Dialpad,
                            title = "Watch Max PIN Attempts",
                            subtitle = when (maxAttempts) {
                                999 -> "Unlimited attempts"
                                else -> "$maxAttempts attempts before wipe"
                            },
                            onClick = { showAttemptsDialog = true }
                        )
                        Text(
                            text = "Watch accounts are silently wiped after max failed attempts. Re-sync required.",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
                        )
                    }
                }
            }

            // Security Section
            item {
                SettingsSection(title = "Security") {
                    SettingsToggleItem(
                        icon = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        title = "Biometric Lock",
                        subtitle = "Require fingerprint/face to view codes",
                        checked = isLocked,
                        onCheckedChange = onToggleLock
                    )
                }
            }

            // Appearance Section
            item {
                SettingsSection(title = "Appearance") {
                    SettingsItem(
                        icon = when (themeSetting) {
                            "light" -> Icons.Default.LightMode
                            "dark" -> Icons.Default.DarkMode
                            else -> Icons.Default.SettingsSuggest
                        },
                        title = "Theme",
                        subtitle = when (themeSetting) {
                            "light" -> "Light Mode"
                            "dark" -> "Dark Mode"
                            else -> "System Default"
                        },
                        onClick = { showThemeDialog = true }
                    )
                }
            }

            // Support & About
            item {
                SettingsSection(title = "About & Support") {
                    SettingsItem(
                        icon = Icons.Default.Favorite,
                        iconColor = Color.Red,
                        title = "Support Me",
                        subtitle = "Help support development",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://anisharyal09.com.np/support?from=aniAuthandroid"))
                            context.startActivity(intent)
                        }
                    )
                    SettingsItem(
                        icon = Icons.Default.VerifiedUser,
                        title = "Privacy Policy",
                        subtitle = "100% local, no data collection",
                        onClick = { showPrivacyDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.Gavel,
                        title = "Disclaimer",
                        subtitle = "Data responsibility and terms",
                        onClick = { showDisclaimerDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.Code,
                        title = "Repository",
                        subtitle = "anishcreations/aniAuth",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/anishcreations/aniAuth"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(40.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "aniAuth v1.5.0",
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/anishcreations/aniAuth/blob/main/CHANGELOG.md"))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        color = TextSecondary.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", color = TextPrimary) },
            text = { 
                Text(
                    "aniAuth is committed to your privacy. We collect absolutely no data. " +
                    "All account information, secrets, and settings are stored 1000% locally on your device's " +
                    "secure storage and encrypted via hardware-backed KeyStore. " +
                    "No internet permission is requested except for opening the support link.",
                    color = TextSecondary
                ) 
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close", color = PurpleAccent)
                }
            },
            containerColor = DarkCard
        )
    }

    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = { Text("Disclaimer", color = TextPrimary) },
            text = { 
                Text(
                    "aniAuth is designed to keep your credentials safe and entirely in your hands. " +
                    "Because it operates 100% offline with no cloud backups, we kindly ask you to keep a secure, " +
                    "personal copy of your secret keys. Please note that since the app runs fully locally, " +
                    "we cannot recover lost accounts or keys for you. This application is provided 'as is' " +
                    "without any warranties. We hope you enjoy a secure, private experience!",
                    color = TextSecondary
                ) 
            },
            confirmButton = {
                TextButton(onClick = { showDisclaimerDialog = false }) {
                    Text("I Understand", color = PurpleAccent)
                }
            },
            containerColor = DarkCard
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val options = listOf(
                        "system" to "System Default",
                        "light" to "Light Mode",
                        "dark" to "Dark Mode"
                    )
                    options.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeChange(value)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (themeSetting == value),
                                onClick = {
                                    onThemeChange(value)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PurpleAccent,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, color = TextPrimary, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel", color = PurpleAccent)
                }
            },
            containerColor = DarkCard
        )
    }

    if (showExportDialog) {
        var selectedEncrypted by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Backup", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select how you want to export your accounts:", color = TextSecondary, fontSize = 14.sp)
                    
                    // Option 1: Encrypted Export
                    Surface(
                        onClick = { selectedEncrypted = true },
                        color = if (selectedEncrypted) PurpleAccent.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (selectedEncrypted) PurpleAccent else BorderColor.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedEncrypted,
                                onClick = { selectedEncrypted = true },
                                colors = RadioButtonDefaults.colors(selectedColor = PurpleAccent, unselectedColor = TextSecondary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Encrypted Backup", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Text(
                                    "AES-256 encrypted. Safe to store almost anywhere (still, keep it secure!). Can only be restored in aniAuth.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // Option 2: Decrypted Export
                    Surface(
                        onClick = { selectedEncrypted = false },
                        color = if (!selectedEncrypted) Color(0xFFEF4444).copy(alpha = 0.1f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (!selectedEncrypted) Color(0xFFEF4444) else BorderColor.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !selectedEncrypted,
                                onClick = { selectedEncrypted = false },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFEF4444), unselectedColor = TextSecondary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Decrypted Backup", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Text(
                                    "Plaintext JSON containing all raw secret keys. Insecure! Anyone with access can read your keys.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onExport(selectedEncrypted)
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedEncrypted) PurpleAccent else Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Export", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkCard
        )
    }

    if (showAttemptsDialog) {
        AlertDialog(
            onDismissRequest = { showAttemptsDialog = false },
            title = { Text("Select Max PIN Attempts", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Watch accounts are silently wiped after max failed attempts. Re-sync required.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        val options = listOf(
                            3 to "3 Attempts (Recommended)",
                            6 to "6 Attempts",
                            9 to "9 Attempts",
                            999 to "Unlimited(999)"
                        )
                        options.forEach { (value, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        context.getSharedPreferences("ani_auth_prefs", android.content.Context.MODE_PRIVATE)
                                            .edit()
                                            .putInt("watch_max_failed_attempts", value)
                                            .apply()
                                        maxAttempts = value
                                        showAttemptsDialog = false
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (maxAttempts == value),
                                    onClick = {
                                        context.getSharedPreferences("ani_auth_prefs", android.content.Context.MODE_PRIVATE)
                                            .edit()
                                            .putInt("watch_max_failed_attempts", value)
                                            .apply()
                                        maxAttempts = value
                                        showAttemptsDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = PurpleAccent,
                                        unselectedColor = TextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, color = TextPrimary, fontSize = 16.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAttemptsDialog = false }) {
                    Text("Cancel", color = PurpleAccent)
                }
            },
            containerColor = DarkCard
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            color = PurpleAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color = PurpleAccent,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EmeraldGreen,
                checkedTrackColor = EmeraldGreen.copy(alpha = 0.5f)
            )
        )
    }
}
