package com.aniauth.authenticator.wearos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import com.aniauth.authenticator.wearos.crypto.KeyStoreHelper
import com.aniauth.authenticator.wearos.crypto.TotpGenerator
import com.aniauth.authenticator.wearos.model.Account
import com.aniauth.authenticator.wearos.model.WatchRepository
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var repository: WatchRepository
    private var isUnlocked = mutableStateOf(false)
    private var cachedCorrectPin: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = WatchRepository(this)

        // Pre-decrypt PIN on startup once to avoid blocking KeyStore lag during entry
        val prefs = getSharedPreferences("watch_lock_pref", Context.MODE_PRIVATE)
        val encrypted = prefs.getString("encrypted_pin", null)
        if (encrypted != null) {
            try {
                cachedCorrectPin = KeyStoreHelper.decrypt(encrypted)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            aniAuthWatchTheme {
                val hasPin = remember { checkHasPin() }
                var showSetup by remember { mutableStateOf(!hasPin) }
                val unlocked by isUnlocked

                Crossfade(targetState = Pair(unlocked, showSetup)) { (isOk, setup) ->
                    if (setup) {
                        PinSetupScreen(
                            onPinConfigured = { pin ->
                                savePin(pin)
                                showSetup = false
                                isUnlocked.value = true
                            }
                        )
                    } else if (!isOk) {
                        PinLockScreen(
                            onCorrectPin = {
                                isUnlocked.value = true
                            }
                        )
                    } else {
                        WatchDashboard(repository)
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // INSTANT LOCK TRIGGER: Lock the app immediately when backgrounded or screen dims
        isUnlocked.value = false
    }

    private fun checkHasPin(): Boolean {
        val prefs = getSharedPreferences("watch_lock_pref", Context.MODE_PRIVATE)
        return prefs.contains("encrypted_pin")
    }

    private fun savePin(pin: String) {
        val prefs = getSharedPreferences("watch_lock_pref", Context.MODE_PRIVATE)
        val encrypted = KeyStoreHelper.encrypt(pin)
        prefs.edit().putString("encrypted_pin", encrypted).apply()
        cachedCorrectPin = pin // Cache in memory immediately
    }

    fun verifyPin(enteredPin: String): Boolean {
        val correct = cachedCorrectPin ?: return false
        return correct == enteredPin
    }
}

@Composable
fun aniAuthWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = Colors(
            primary = Color(0xFFC084FC),
            primaryVariant = Color(0xFF9333EA),
            secondary = Color(0xFFE9D5FF),
            background = Color(0xFF08070A),
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onBackground = Color.White
        ),
        content = content
    )
}

@Composable
fun PinSetupScreen(onPinConfigured: (String) -> Unit) {
    var enteredPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var firstPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (step == 1) "Create PIN" else "Confirm PIN",
            fontSize = 11.sp,
            color = MaterialTheme.colors.primary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(3.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 1.dp)
        ) {
            for (i in 0 until 4) {
                val isFilled = i < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isFilled) Color(0xFFC084FC) else Color.Transparent,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isFilled) Color(0xFFC084FC) else Color(0xFF4A3E5D),
                            shape = CircleShape
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(1.dp))
        
        if (errorMsg.isNotEmpty()) {
            Text(
                text = errorMsg,
                fontSize = 8.sp,
                color = Color.Red,
                textAlign = TextAlign.Center
            )
        } else {
            Spacer(modifier = Modifier.height(10.dp))
        }
        
        WatchKeypad(
            onDigit = { digit ->
                if (enteredPin.length < 4) {
                    enteredPin += digit
                    errorMsg = ""
                }
                if (enteredPin.length == 4) {
                    if (step == 1) {
                        firstPin = enteredPin
                        enteredPin = ""
                        step = 2
                    } else {
                        if (enteredPin == firstPin) {
                            onPinConfigured(enteredPin)
                        } else {
                            errorMsg = "PINs do not match!"
                            enteredPin = ""
                            step = 1
                        }
                    }
                }
            },
            onBackspace = {
                if (enteredPin.isNotEmpty()) {
                    enteredPin = enteredPin.dropLast(1)
                }
            }
        )
    }
}

@Composable
fun PinLockScreen(onCorrectPin: () -> Unit) {
    val activity = androidx.compose.ui.platform.LocalContext.current as MainActivity
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked",
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(10.dp)
        )
        
        Text(
            text = "Enter Watch PIN",
            fontSize = 10.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(3.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 1.dp)
        ) {
            for (i in 0 until 4) {
                val isFilled = i < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isFilled) Color(0xFFC084FC) else Color.Transparent,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isFilled) Color(0xFFC084FC) else Color(0xFF4A3E5D),
                            shape = CircleShape
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(1.dp))
        
        if (errorMsg.isNotEmpty()) {
            Text(
                text = errorMsg,
                fontSize = 8.sp,
                color = Color.Red,
                textAlign = TextAlign.Center
            )
        } else {
            Spacer(modifier = Modifier.height(10.dp))
        }
        
        WatchKeypad(
            onDigit = { digit ->
                if (enteredPin.length < 4) {
                    enteredPin += digit
                    errorMsg = ""
                }
                if (enteredPin.length == 4) {
                    if (activity.verifyPin(enteredPin)) {
                        onCorrectPin()
                    } else {
                        errorMsg = "Incorrect PIN!"
                        enteredPin = ""
                    }
                }
            },
            onBackspace = {
                if (enteredPin.isNotEmpty()) {
                    enteredPin = enteredPin.dropLast(1)
                }
            }
        )
    }
}

@Composable
fun WatchKeypad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val buttons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("back", "0", "")
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (row in buttons) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (item in row) {
                    if (item == "back") {
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(26.dp)
                                .background(Color(0xFF1E1B2C), shape = RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF2C243B), shape = RoundedCornerShape(8.dp))
                                .clickable(onClick = onBackspace),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Backspace",
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                        }
                    } else if (item.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(26.dp)
                                .background(Color(0xFF1E1B2C), shape = RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF2C243B), shape = RoundedCornerShape(8.dp))
                                .clickable { onDigit(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = item, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(42.dp).height(26.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalTimerHeader() {
    var timeRemaining by remember { mutableStateOf(30L) }
    
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis() / 1000
            timeRemaining = 30 - (now % 30)
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF2C243B),
                style = Stroke(width = 2.dp.toPx())
            )
            drawArc(
                color = if (timeRemaining <= 5) Color.Red else Color(0xFFC084FC),
                startAngle = -90f,
                sweepAngle = (timeRemaining.toFloat() / 30f) * 360f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        Text(
            text = timeRemaining.toString(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun WatchDashboard(repository: WatchRepository) {
    var accounts by remember { mutableStateOf(repository.getAccounts()) }
    val listState = rememberScalingLazyListState()
    
    DisposableEffect(repository) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "accounts_json") {
                accounts = repository.getAccounts()
            }
        }
        repository.prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            repository.prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    var currentEpoch by remember { mutableStateOf(0L) }
    
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis() / 1000
            currentEpoch = now / 30
            val nextChange = 30 - (now % 30)
            delay(nextChange * 1000)
        }
    }
    
    Scaffold(
        positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
    ) {
        if (accounts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No accounts synced.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap 'Sync to Watch' on your phone app.",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                autoCentering = null
            ) {
                item {
                    GlobalTimerHeader()
                }
                items(accounts) { account ->
                    WatchAccountCard(account, currentEpoch)
                }
                item {
                    Text(
                        text = "aniAuth v1.0.0 (watch)",
                        fontSize = 9.sp,
                        color = Color.LightGray.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WatchAccountCard(account: Account, currentEpoch: Long) {
    val decryptedSecret = remember(account.encryptedSecret) {
        WatchRepository.decryptCached(account.encryptedSecret)
    }
    
    val code = remember(decryptedSecret, currentEpoch) {
        if (decryptedSecret.isNotEmpty()) {
            TotpGenerator.generateTOTP(decryptedSecret) ?: "000000"
        } else {
            "000000"
        }
    }
    
    val formattedCode = remember(code) {
        if (code.length == 6) {
            "${code.substring(0, 3)} ${code.substring(3)}"
        } else {
            code
        }
    }

    Card(
        onClick = { /* Disable click actions */ },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = Color(0xFF120E1A),
            endBackgroundColor = Color(0xFF1A1527)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = account.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                if (account.username != null) {
                    Text(
                        text = account.username,
                        fontSize = 9.sp,
                        color = Color.LightGray,
                        maxLines = 1
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = formattedCode,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}
