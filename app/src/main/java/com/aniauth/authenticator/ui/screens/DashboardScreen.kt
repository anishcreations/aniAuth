package com.aniauth.authenticator.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.aniauth.authenticator.crypto.OtpAuthParser
import com.aniauth.authenticator.crypto.KeyStoreHelper
import com.aniauth.authenticator.crypto.TotpGenerator
import com.aniauth.authenticator.model.Account
import com.aniauth.authenticator.model.AccountRepository
import com.aniauth.authenticator.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    repository: AccountRepository,
    onExportBackup: (Boolean) -> Unit,
    onImportBackup: () -> Unit,
    onToggleLock: (Boolean) -> Unit,
    isLocked: Boolean,
    themeSetting: String,
    onThemeChange: (String) -> Unit,
    isWatchConnected: Boolean = false,
    onSyncToWatch: () -> Unit = {}
) {
    val context = LocalContext.current
    var accounts by remember { mutableStateOf(repository.getAccounts()) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    var selectedAccountForDetails by remember { mutableStateOf<Account?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var sortOrder by remember { mutableStateOf(repository.getSortOrder()) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var activeLetterY by remember { mutableFloatStateOf(0f) }
    
    val isScrolling = lazyListState.isScrollInProgress
    var isSidebarVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            isSidebarVisible = true
        } else {
            delay(1500L) // Wait 1.5s before starting to fade out
            isSidebarVisible = false
        }
    }
    
    val finalSidebarVisible = isSidebarVisible || (activeLetter != null)
    
    val sidebarAlpha by animateFloatAsState(
        targetValue = if (finalSidebarVisible) 0.4f else 0.05f,
        animationSpec = tween(durationMillis = 300),
        label = "SidebarAlpha"
    )
    
    // Timer states
    var timeRemaining by remember { mutableLongStateOf(30L - (System.currentTimeMillis() / 1000 % 30)) }
    var progress by remember { mutableFloatStateOf(timeRemaining / 30f) }
    
    // Refresh loop for TOTP codes
    LaunchedEffect(Unit) {
        while (true) {
            val currentSecond = System.currentTimeMillis() / 1000
            timeRemaining = 30L - (currentSecond % 30)
            progress = timeRemaining / 30f
            delay(1000L) 
        }
    }
    
    val filteredAccounts = remember(accounts, searchQuery, sortOrder) {
        val filtered = accounts.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
            (it.username != null && it.username.contains(searchQuery, ignoreCase = true))
        }
        when (sortOrder) {
            "alphabetical" -> filtered.sortedBy { it.label.lowercase() }
            "alphabetical_desc" -> filtered.sortedByDescending { it.label.lowercase() }
            else -> filtered
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "aniAuth",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary.copy(alpha = 0.9f),
                        fontSize = 24.sp
                    )
                },
                actions = {
                    var showSortMenu by remember { mutableStateOf(false) }
                    
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true }
                        ) {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = "Sort Accounts",
                                tint = PurpleAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(DarkCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Default (Date Added)", color = if (sortOrder == "default") PurpleAccent else TextPrimary) },
                                onClick = {
                                    sortOrder = "default"
                                    repository.saveSortOrder("default")
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOrder == "default") {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = PurpleAccent)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Alphabetical (A-Z)", color = if (sortOrder == "alphabetical") PurpleAccent else TextPrimary) },
                                onClick = {
                                    sortOrder = "alphabetical"
                                    repository.saveSortOrder("alphabetical")
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOrder == "alphabetical") {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = PurpleAccent)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Alphabetical (Z-A)", color = if (sortOrder == "alphabetical_desc") PurpleAccent else TextPrimary) },
                                onClick = {
                                    sortOrder = "alphabetical_desc"
                                    repository.saveSortOrder("alphabetical_desc")
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOrder == "alphabetical_desc") {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = PurpleAccent)
                                    }
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings, 
                            contentDescription = "Settings", 
                            tint = PurpleAccent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { showScanner = true },
                    containerColor = if (MaterialTheme.colorScheme.background == RawLightBg) PurpleAccent else PurpleAccent.copy(alpha = 0.85f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR Code", modifier = Modifier.size(24.dp))
                }
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = if (MaterialTheme.colorScheme.background == RawLightBg) PurpleAccent else PurpleAccent.copy(alpha = 0.85f),
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
        ) {
            if (accounts.isNotEmpty()) {
                // Cohesive Search + Timer Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(DarkCard)
                        .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular Timer on the Left
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(40.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            color = if (timeRemaining <= 5) Color.Red else EmeraldGreen,
                            strokeWidth = 3.dp,
                            trackColor = BorderColor,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = timeRemaining.toString(),
                            color = if (timeRemaining <= 5) Color.Red else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Middle area: switch between Info text and Search text field
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (isSearchExpanded) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (searchQuery.isEmpty()) {
                                            Text("Search accounts...", color = TextSecondary, fontSize = 15.sp)
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                        } else {
                            Column {
                                Text(
                                    "Time-based OTP",
                                    color = TextPrimary.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    "Automatically refreshes every 30s",
                                    color = TextSecondary.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Search/Close Toggle Button on the Right
                    IconButton(
                        onClick = {
                            if (isSearchExpanded) {
                                searchQuery = ""
                                isSearchExpanded = false
                            } else {
                                isSearchExpanded = true
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearchExpanded) "Close Search" else "Open Search",
                            tint = PurpleAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else {
                // Minimal Placeholder with Import action in place of Search/Timer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(DarkCard)
                        .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("No accounts added yet", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    TextButton(
                        onClick = onImportBackup,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", color = PurpleAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            
            // Accounts List
            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Scan a QR code or tap + to add an account.\nYou can also import backups using the\nbutton above or in Settings.",
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth(0.8f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        Text(
                            text = "aniAuth v1.5.0",
                            color = TextSecondary.copy(alpha = 0.3f),
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.clickable {
                                try {
                                    uriHandler.openUri("https://github.com/anishcreations/aniAuth/blob/main/CHANGELOG.md")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        )
                    }
                }
            } else if (filteredAccounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No matching accounts found for \"$searchQuery\"",
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 22.dp, top = 4.dp, bottom = 16.dp)
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

                        // Noticeable Footer at the end of scroll
                        item {
                            Surface(
                                onClick = { showManual = true },
                                color = SoftFooterColor.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp, bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = SoftFooterColor.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "View User Manual & Tips",
                                        color = SoftFooterColor.copy(alpha = 0.8f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        item {
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "aniAuth v1.5.0",
                                    color = SoftFooterColor.copy(alpha = 0.3f),
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier.clickable {
                                        try {
                                            uriHandler.openUri("https://github.com/anishcreations/aniAuth/blob/main/CHANGELOG.md")
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Alphabet Sidebar overlayed on the right edge
                    AlphabetSidebar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(y = (-70).dp)
                            .padding(end = 2.dp)
                            .height(440.dp)
                            .graphicsLayer { this.alpha = sidebarAlpha },
                        sortOrder = sortOrder,
                        onLetterSelected = { letter ->
                            val targetIndex = filteredAccounts.indexOfFirst {
                                it.label.firstOrNull()?.uppercaseChar() == letter
                            }
                            if (targetIndex != -1) {
                                coroutineScope.launch {
                                    lazyListState.scrollToItem(targetIndex)
                                }
                            } else {
                                // Find the account whose first letter has the minimum alphabetical distance
                                var bestIndex = -1
                                var minDistance = Int.MAX_VALUE
                                filteredAccounts.forEachIndexed { index, account ->
                                    val firstChar = account.label.firstOrNull()?.uppercaseChar()
                                    if (firstChar != null && firstChar in 'A'..'Z') {
                                        val dist = java.lang.Math.abs(firstChar - letter)
                                        if (dist < minDistance) {
                                            minDistance = dist
                                            bestIndex = index
                                        }
                                    }
                                }
                                if (bestIndex != -1) {
                                    coroutineScope.launch {
                                        lazyListState.scrollToItem(bestIndex)
                                    }
                                }
                            }
                        },
                        onDragStateChanged = { letter, y ->
                            activeLetter = letter
                            activeLetterY = y
                        }
                    )

                    // Floating Letter Bubble Indicator overlay
                    if (activeLetter != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(
                                    x = (-36).dp,
                                    y = with(LocalDensity.current) { activeLetterY.toDp() } - 290.dp
                                )
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PurpleAccent.copy(alpha = 0.95f))
                                .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeLetter.toString(),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        BackHandler {
            showAddDialog = false
        }
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
        BackHandler {
            showScanner = false
        }
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

    if (showSettings) {
        BackHandler {
            showSettings = false
        }
        SettingsScreen(
            onDismiss = { showSettings = false },
            onExport = onExportBackup,
            onImport = onImportBackup,
            isLocked = isLocked,
            onToggleLock = onToggleLock,
            onShowManual = { showManual = true },
            themeSetting = themeSetting,
            onThemeChange = onThemeChange,
            isWatchConnected = isWatchConnected,
            onSyncToWatch = onSyncToWatch
        )
    }

    if (showManual) {
        BackHandler {
            showManual = false
        }
        UserManualDialog(onDismiss = { showManual = false })
    }

    selectedAccountForDetails?.let { account ->
        BackHandler {
            selectedAccountForDetails = null
        }
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

@Composable
fun UserManualDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("User Manual & Tips", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GuideItem(Icons.Default.TouchApp, "Tap any card to copy its 2FA code.")
                GuideItem(Icons.Default.AdsClick, "Long-press a card to edit details or view the secret key.")
                GuideItem(Icons.Default.QrCodeScanner, "Tap the scanner button to scan 2FA QR codes via camera.")
                GuideItem(Icons.Default.Add, "Tap the + button to manually add accounts.")
                GuideItem(Icons.Default.Backup, "Export password-protected backups or import from settings.")
                GuideItem(Icons.Default.Security, "All data is encrypted and stored locally on your device.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = PurpleAccent)
            }
        },
        containerColor = DarkCard
    )
}

@Composable
fun GuideItem(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = TextPrimary, fontSize = 13.sp, lineHeight = 18.sp)
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
    
    // Calculate the TOTP code (recalculate only when the 30s epoch step actually changes)
    val timeStep = remember(timeRemaining) {
        (System.currentTimeMillis() / 1000) / 30
    }
    val code = remember(decryptedSecret, timeStep) {
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
            .height(100.dp) // Slimmer fixed height
            .clip(RoundedCornerShape(12.dp)) // Slightly tighter corners for slim look
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
                .fillMaxSize() // Use fillMaxSize to match card height
                .padding(horizontal = 16.dp, vertical = 8.dp), // Reduced vertical padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.label,
                    color = PurpleAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp, 
                    letterSpacing = 0.05.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                if (!account.username.isNullOrEmpty()) {
                    Text(
                        text = account.username,
                        color = TextPrimary.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 1.dp),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            // Code in the middle/right
            Text(
                text = formattedCode,
                color = if (timeRemaining <= 5) Color.Red else EmeraldGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun AlphabetSidebar(
    modifier: Modifier = Modifier,
    sortOrder: String = "default",
    onLetterSelected: (Char) -> Unit,
    onDragStateChanged: (Char?, Float) -> Unit
) {
    val alphabet = remember(sortOrder) {
        if (sortOrder == "alphabetical_desc") ('Z' downTo 'A').toList() else ('A'..'Z').toList()
    }
    var height by remember { mutableIntStateOf(0) }
    var touchY by remember { mutableStateOf<Float?>(null) }
    
    val haptic = LocalHapticFeedback.current
    var lastHapticLetter by remember { mutableStateOf<Char?>(null) }

    Column(
        modifier = modifier
            .width(16.dp)
            .background(Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(8.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp)
            .onGloballyPositioned { coordinates ->
                height = coordinates.size.height
            }
            .pointerInput(height) {
                if (height <= 0) return@pointerInput

                fun handleTouch(y: Float) {
                    touchY = y
                    val index = (y / height * alphabet.size).toInt()
                        .coerceIn(0, alphabet.size - 1)
                    val letter = alphabet[index]
                    onLetterSelected(letter)
                    onDragStateChanged(letter, y)
                    
                    if (letter != lastHapticLetter) {
                        lastHapticLetter = letter
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }

                detectTapGestures(
                    onPress = { offset ->
                        handleTouch(offset.y)
                        try {
                            awaitRelease()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        touchY = null
                        lastHapticLetter = null
                        onDragStateChanged(null, 0f)
                    }
                )
            }
            .pointerInput(height) {
                if (height <= 0) return@pointerInput

                fun handleTouch(y: Float) {
                    touchY = y
                    val index = (y / height * alphabet.size).toInt()
                        .coerceIn(0, alphabet.size - 1)
                    val letter = alphabet[index]
                    onLetterSelected(letter)
                    onDragStateChanged(letter, y)
                    
                    if (letter != lastHapticLetter) {
                        lastHapticLetter = letter
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }

                detectDragGestures(
                    onDragStart = { offset ->
                        handleTouch(offset.y)
                    },
                    onDrag = { change, _ ->
                        handleTouch(change.position.y)
                    },
                    onDragEnd = {
                        touchY = null
                        lastHapticLetter = null
                        onDragStateChanged(null, 0f)
                    },
                    onDragCancel = {
                        touchY = null
                        lastHapticLetter = null
                        onDragStateChanged(null, 0f)
                    }
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        alphabet.forEachIndexed { index, char ->
            val scale = {
                val currentTouchY = touchY
                if (currentTouchY != null && height > 0) {
                    val letterCenterY = (index + 0.5f) / alphabet.size * height
                    val dist = kotlin.math.abs(currentTouchY - letterCenterY)
                    val maxDist = height / 5.0f
                    if (dist < maxDist) {
                        1.0f + 0.7f * (1.0f - dist / maxDist)
                    } else {
                        1.0f
                    }
                } else {
                    1.0f
                }
            }
            Text(
                text = char.toString(),
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        val s = scale()
                        scaleX = s
                        scaleY = s
                        translationX = -((s - 1.0f) * 8.dp.toPx())
                    }
            )
        }
    }
}
