package com.aniauth.authenticator

import android.graphics.Bitmap
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.aniauth.authenticator.model.AccountRepository
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Uses disposable sample accounts on an emulator only; never runs against a user's phone. */
class UiSmokeTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun accountAndSettingsFlows() {
        assumeTrue("Disposable emulator required", Build.MODEL.startsWith("sdk_gphone"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("ani_auth_prefs", 0)
        prefs.edit().clear().putBoolean("screen_security_enabled", false)
            .putString("theme_setting", "light").commit()
        val repository = AccountRepository(context)
        repository.addAccount("GitHub", "JBSWY3DPEHPK3PXP", null, "alex@example.com")
        repository.addAccount("Google", "JBSWY3DPEHPK3PXP", null, "personal@example.com")
        repository.addAccount("Cloudflare", "JBSWY3DPEHPK3PXP", null, "work@example.com")
        repository.addAccount("A service with a very long account name", "JBSWY3DPEHPK3PXP", null,
            "a.long.account.name@example.com")

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("GitHub").assertIsDisplayed()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            compose.runOnIdle {
                assertEquals(true, repository.isTapToCopyEnabled())
                clipboard.setPrimaryClip(ClipData.newPlainText("test", "unchanged"))
            }
            compose.onNodeWithText("GitHub").performClick()
            compose.runOnIdle {
                org.junit.Assert.assertTrue(
                    clipboard.primaryClip!!.getItemAt(0).text.toString().matches(Regex("[0-9]{6}"))
                )
            }
            capture("dashboard")
            compose.onNodeWithContentDescription("Open Search").performClick()
            compose.onNode(hasSetTextAction()).performTextInput("Google")
            compose.onNode(hasText("Google") and !hasSetTextAction()).assertIsDisplayed()
            compose.onNodeWithText("GitHub").assertDoesNotExist()
            compose.onNodeWithContentDescription("Close Search").performClick()

            compose.onNodeWithText("GitHub").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("GitHub").performTouchInput { longClick() }
            compose.waitUntil(5000) {
                compose.onAllNodesWithText("Account Details").fetchSemanticsNodes().isNotEmpty()
            }
            capture("account-details")
            compose.onNodeWithText("Account Details").assertIsDisplayed()
            compose.onNodeWithText("Cancel").performClick()

            compose.onNodeWithContentDescription("Add Account").performClick()
            compose.onNodeWithText("Add Account").assertIsDisplayed()
            capture("add-account")
            compose.onNodeWithText("Cancel").performClick()

            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onAllNodes(hasScrollToIndexAction()).onLast()
                .performScrollToNode(hasText("Biometric Lock"))
            compose.onNodeWithText("Biometric Lock").assertIsDisplayed()
            capture("settings")
            compose.onAllNodes(hasScrollToIndexAction()).onLast()
                .performScrollToNode(hasText("Tap to Copy Codes"))
            compose.onNodeWithContentDescription("Tap to Copy Codes")
                .performClick()
            compose.runOnIdle {
                assertEquals(false, repository.isTapToCopyEnabled())
            }
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithText("GitHub").performScrollTo()
            compose.runOnIdle { clipboard.setPrimaryClip(ClipData.newPlainText("test", "unchanged")) }
            compose.onNodeWithText("GitHub").performClick()
            compose.runOnIdle {
                assertEquals("unchanged", clipboard.primaryClip!!.getItemAt(0).text.toString())
            }
            compose.onNodeWithText("GitHub").performTouchInput { longClick() }
            compose.onNodeWithText("Account Details").assertIsDisplayed()
            compose.onNodeWithText("Cancel").performClick()
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onAllNodes(hasScrollToIndexAction()).onLast().performScrollToNode(hasText("Theme"))
            compose.onNodeWithText("Theme").performClick()
            compose.onNodeWithText("Dark Mode").performClick()
            capture("settings-dark")
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithText("Settings").assertDoesNotExist()
            compose.onNodeWithText("GitHub").assertIsDisplayed()
            capture("dashboard-dark")
            compose.onAllNodes(hasScrollToIndexAction()).onLast()
                .performScrollToNode(hasText("aniAuth v1.7.0"))
            capture("footer-dark")
            assertEquals(4, repository.getAccounts().size)
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            assertEquals(false, repository.isTapToCopyEnabled())
            compose.onNodeWithText("GitHub").performClick()
            compose.runOnIdle {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                assertEquals("unchanged", clipboard.primaryClip!!.getItemAt(0).text.toString())
            }
        }
        prefs.edit().clear().putBoolean("screen_security_enabled", false).commit()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("No accounts added yet").assertIsDisplayed()
            capture("empty")
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(300, 5000)
        // Let the emulator's render thread present the native frame, including dialog windows.
        SystemClock.sleep(300)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = context.getExternalFilesDir("design-review")!!
        val suffix = InstrumentationRegistry.getArguments().getString("captureSuffix", "")
        File(dir, "$name$suffix.png").outputStream().use { stream ->
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                .compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }
}
