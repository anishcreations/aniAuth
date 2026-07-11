package com.aniauth.authenticator.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniauth.authenticator.crypto.OtpAuthParser
import com.aniauth.authenticator.crypto.KeyStoreHelper
import com.aniauth.authenticator.crypto.TotpGenerator
import com.aniauth.authenticator.model.Account
import com.aniauth.authenticator.model.AccountRepository
import com.aniauth.authenticator.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    repository: AccountRepository,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onToggleLock: (Boolean) -> Unit,
    isLocked: Boolean
) {
    val context = LocalContext.current
    var accounts by remember { mutableStateOf(repository.getAccounts()) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var selectedAccountForDetails by remember { mutableStateOf<Account?>(null) }
    
    // Timer states
    var timeRemaining by remember { mutableLongStateOf(30L - (System.currentTimeMillis() / 1000 % 30)) }
    var progress by remember { mutableFloatStateOf(timeRemaining / 30f) }
    
    // Refresh loop for TOTP codes
    LaunchedEffect(Unit) {
        while (true) {
            val currentSecond = System.currentTimeMillis() / 1000
            timeRemaining = 30L - (currentSecond % 30)
            progress = timeRemaining / 30f
            delay(200L) // Refresh frequently for responsive progress bar
        }
    }
    
    val filteredAccounts = accounts.filter {
        it.label.contains(searchQuery, ignoreCase = true) ||
        (it.issuer != null && it.issuer.contains(searchQuery, ignoreCase = true))
    }
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "aniAuth",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 32.sp
                    )
                },
                actions = {
                    IconButton(onClick = onImportBackup) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import Backup", tint = PurpleAccent)
                    }
                    IconButton(onClick = onExportBackup) {
                        Icon(Icons.Default.DownloadForOffline, contentDescription = "Export Backup", tint = PurpleAccent)
                    }
                    IconButton(onClick = { onToggleLock(!isLocked) }) {
                        Icon(
                            if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Toggle Lock",
                            tint = if (isLocked) EmeraldGreen else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = DarkBg
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { showScanner = true },
                    containerColor = PurpleAccent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR Code", modifier = Modifier.size(24.dp))
                }
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = PurpleAccent,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Account", modifier = Modifier.size(28.dp))
                }
            }
        },
        containerColor = DarkBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search accounts...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Timer
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        color = if (timeRemaining <= 5) Color.Red else EmeraldGreen,
                        strokeWidth = 4.dp,
                        trackColor = BorderColor,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = timeRemaining.toString(),
                        color = if (timeRemaining <= 5) Color.Red else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        "Time-based OTP",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Codes refresh automatically every 30s",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Accounts List
            if (filteredAccounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No accounts found.\nScan a QR code or tap + to add one.",
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                items(filteredAccounts, key = { it.id }) { account ->
                    AccountCard(
                        account = account,
                        timeRemaining = timeRemaining,
                        onLongClick = {
                            selectedAccountForDetails = account
                        }
                    )
                }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddAccountScreen(
            onDismiss = { showAddDialog = false },
            onAdd = { label, secret, issuer, username ->
                repository.addAccount(label, secret, issuer, username)
                accounts = repository.getAccounts()
                showAddDialog = false
            }
        )
    }

    if (showScanner) {
        ScannerScreen(
            onQrCodeDetected = { qrData ->
                val parsed = OtpAuthParser.parse(qrData)
                if (parsed != null) {
                    repository.addAccount(parsed.label, parsed.encryptedSecret, parsed.issuer, parsed.username)
                    accounts = repository.getAccounts()
                    showScanner = false
                    Toast.makeText(context, "Account added!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Invalid QR Code", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showScanner = false }
        )
    }

    selectedAccountForDetails?.let { account ->
        AccountDetailsScreen(
            account = account,
            onDismiss = { selectedAccountForDetails = null },
            onUpdate = { label, username ->
                repository.updateAccount(account.id, label, username)
                accounts = repository.getAccounts()
                selectedAccountForDetails = null
                Toast.makeText(context, "Account updated!", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                repository.deleteAccount(account.id)
                accounts = repository.getAccounts()
                selectedAccountForDetails = null
                Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountCard(
    account: Account,
    timeRemaining: Long,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Decrypt the secret to calculate the TOTP code
    val decryptedSecret = remember(account.encryptedSecret) {
        KeyStoreHelper.decrypt(account.encryptedSecret) ?: ""
    }
    
    // Calculate the TOTP code
    val code = remember(decryptedSecret, timeRemaining) {
        if (decryptedSecret.isNotEmpty()) {
            TotpGenerator.generateTOTP(decryptedSecret) ?: "000000"
        } else {
            "000000"
        }
    }
    
    // Format code as "123 456"
    val formattedCode = remember(code) {
        if (code.length >= 6) {
            "${code.substring(0, 3)} ${code.substring(3, 6)}"
        } else {
            code
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("2FA Code", code)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.label,
                    color = PurpleAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = 0.05.sp
                )
                
                if (!account.username.isNullOrEmpty()) {
                    Text(
                        text = account.username,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = formattedCode,
                    color = if (timeRemaining <= 5) Color.Red else EmeraldGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }
            
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy Code",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
