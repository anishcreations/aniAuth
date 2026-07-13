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
    onExport: () -> Unit,
    onImport: () -> Unit,
    isLocked: Boolean,
    onToggleLock: (Boolean) -> Unit,
    onShowManual: () -> Unit,
    themeSetting: String,
    onThemeChange: (String) -> Unit
) {
    val context = LocalContext.current
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

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
                        onClick = onExport
                    )
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
                Text(
                    "aniAuth v1.0.0",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
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
