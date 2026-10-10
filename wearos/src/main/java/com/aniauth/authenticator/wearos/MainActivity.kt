package com.aniauth.authenticator.wearos

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults

class MainActivity : ComponentActivity() {

    private lateinit var repository: WatchRepository
    private var isUnlocked = mutableStateOf(false)
    private var cachedCorrectPin: String? = null

    private val accountsState = mutableStateOf<List<Account>>(emptyList())

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "accounts_json") {
            accountsState.value = repository.getAccounts()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = WatchRepository(this)
        accountsState.value = repository.getAccounts()
        repository.prefs.registerOnSharedPreferenceChangeListener(preferenceListener)

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
                            },
                            onDataWiped = {
                                showSetup = true
                            }
                        )
                    } else {
                        WatchDashboard(accountsState.value)
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

    override fun onDestroy() {
        super.onDestroy()
        repository.prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
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

    fun getMaxFailedAttempts(): Int {
        val prefs = getSharedPreferences("watch_lock_pref", Context.MODE_PRIVATE)
        return prefs.getInt("max_failed_attempts", 3)
    }

    fun getFailedAttempts(): Int {
        val prefs = getSharedPreferences("watch_lock_pref", Context.MODE_PRIVATE)
        return prefs.getInt("failed_attempts", 0)
    }

    fun incrementFailedAttempts() {
        val prefs = getSharedPreferences("watch_lock_pref", Context.MODE_PRIVATE)
        val current = prefs.getInt("failed_attempts", 0) + 1
        prefs.edit().putInt("failed_attempts", current).apply()

        val max = getMaxFailedAttempts()
        if (current >= max) {
            // Silent wipe of accounts database behind the scenes
            val accountsPrefs = getSharedPreferences("wear_accounts_pref", Context.MODE_PRIVATE)
            accountsPrefs.edit().clear().apply()
            WatchRepository.clearCache()
            accountsState.value = emptyList()
        }
    }

    fun resetFailedAttempts() {
        val prefs = getSharedPreferences("watch_lock_pref", Context.MODE_PRIVATE)
        prefs.edit().remove("failed_attempts").apply()
    }

    fun wipeData() {
        val lockPrefs = getSharedPreferences("watch_lock_pref", Context.MODE_PRIVATE)
        lockPrefs.edit().clear().apply()
        cachedCorrectPin = null

        val accountsPrefs = getSharedPreferences("wear_accounts_pref", Context.MODE_PRIVATE)
        accountsPrefs.edit().clear().apply()

        WatchRepository.clearCache()
        accountsState.value = emptyList()
    }

    fun verifyPin(enteredPin: String): Boolean {
        val correct = cachedCorrectPin ?: return false
        val isCorrect = correct == enteredPin
        if (isCorrect) {
            resetFailedAttempts()
        } else {
            incrementFailedAttempts()
        }
        return isCorrect
    }
}

private val WatchOutline = Color(0xFF49454F)
private val WatchSecondaryText = Color(0xFFB7B2C0)

@Composable
fun aniAuthWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = Colors(
            primary = Color(0xFFBA9CFF),
            primaryVariant = Color(0xFF493064),
            secondary = Color(0xFF273D59),
            background = Color(0xFF100D17),
            surface = Color(0xFF20192C),
            error = Color(0xFFFFB4AB),
            onPrimary = Color(0xFF2E204C),
            onSecondary = Color(0xFFDCE8FA),
            onBackground = Color(0xFFECEAF0),
            onSurface = Color(0xFFECEAF0),
            onError = Color(0xFF690005)
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
        verticalArrangement = Arrangement.Center
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
                            color = if (isFilled) MaterialTheme.colors.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isFilled) MaterialTheme.colors.primary else WatchOutline,
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
                color = MaterialTheme.colors.error,
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
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun PinLockScreen(onCorrectPin: () -> Unit, onDataWiped: () -> Unit) {
    val activity = androidx.compose.ui.platform.LocalContext.current as MainActivity
    var enteredPin by remember { mutableStateOf("") }
    var failedCount by remember { mutableStateOf(activity.getFailedAttempts()) }
    var errorMsg by remember { mutableStateOf("") }
    val maxAttempts = remember { activity.getMaxFailedAttempts() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
            color = WatchSecondaryText,
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
                            color = if (isFilled) MaterialTheme.colors.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isFilled) MaterialTheme.colors.primary else WatchOutline,
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
                color = MaterialTheme.colors.error,
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
                        failedCount = 0
                        onCorrectPin()
                    } else {
                        val failed = activity.getFailedAttempts()
                        failedCount = failed
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

        if (failedCount >= maxAttempts) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Reset PIN",
                fontSize = 9.sp,
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable {
                        activity.wipeData()
                        onDataWiped()
                    }
                    .padding(vertical = 2.dp, horizontal = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(13.dp))
        }
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
                                .background(MaterialTheme.colors.secondary, shape = RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF3D5573), shape = RoundedCornerShape(8.dp))
                                .clickable(onClick = onBackspace),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Backspace",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colors.onSecondary
                            )
                        }
                    } else if (item.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(26.dp)
                                .background(MaterialTheme.colors.secondary, shape = RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF3D5573), shape = RoundedCornerShape(8.dp))
                                .clickable { onDigit(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = item, fontSize = 13.sp, color = MaterialTheme.colors.onSecondary, fontWeight = FontWeight.Medium)
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
    val colors = MaterialTheme.colors
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
                color = WatchOutline,
                style = Stroke(width = 2.dp.toPx())
            )
            drawArc(
                color = if (timeRemaining <= 5) colors.error else colors.primary,
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
            color = colors.onBackground
        )
    }
}

@Composable
fun WatchDashboard(accounts: List<Account>) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    
    var currentEpoch by remember { mutableStateOf(0L) }
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis() / 1000
            currentEpoch = now / 30
            val nextChange = 30 - (now % 30)
            delay(nextChange * 1000)
        }
    }

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty()) {
            focusRequester.requestFocus()
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
                    color = WatchSecondaryText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap 'Sync to Watch' on your phone app.",
                    fontSize = 9.sp,
                    color = WatchSecondaryText,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .onRotaryScrollEvent { event ->
                        rotaryAccumulator += event.verticalScrollPixels
                        if (kotlin.math.abs(rotaryAccumulator) >= 36f) {
                            rotaryAccumulator = 0f
                            if (!view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                        }
                        listState.dispatchRawDelta(event.verticalScrollPixels)
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable(),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(
                    edgeScale = 0.60f,
                    edgeAlpha = 0.50f,
                    minTransitionArea = 0.22f,
                    maxTransitionArea = 0.45f
                ),
                autoCentering = null,
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    GlobalTimerHeader()
                }
                items(accounts, key = { it.id }) { account ->
                    WatchAccountCard(account, currentEpoch)
                }
                item {
                    Text(
                        text = "aniAuth v1.2.0 (watch)",
                        fontSize = 9.sp,
                        color = WatchSecondaryText,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
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
            .heightIn(min = 60.dp)
            .border(1.dp, WatchOutline, shape = RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = MaterialTheme.colors.surface,
            endBackgroundColor = MaterialTheme.colors.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = account.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (account.username != null) {
                    Text(
                        text = account.username,
                        fontSize = 9.sp,
                        color = WatchSecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = formattedCode,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}
