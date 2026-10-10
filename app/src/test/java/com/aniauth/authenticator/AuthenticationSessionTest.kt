package com.aniauth.authenticator

import org.junit.Assert.*
import org.junit.Test

class AuthenticationSessionTest {
    private val session = AuthenticationSession().apply { onResume() }
    private var actionCount = 0

    private fun start() = session.authenticate(
        action = { actionCount++ },
        launchPrompt = {},
        onLaunchError = { throw AssertionError("Unexpected launch error", it) }
    )

    @Test
    fun successExecutesActionOnlyOnce() {
        start()
        session.onSuccess()
        session.onSuccess()
        session.onResume()
        assertEquals(1, actionCount)
        assertFalse(session.isInProgress)
    }

    @Test
    fun successWithoutPendingAuthenticationDoesNothing() {
        session.onSuccess()
        assertEquals(0, actionCount)
    }

    @Test
    fun cancellationOrTerminalErrorDiscardsActionEvenIfSuccessArrivesLater() {
        start()
        session.onError()
        session.onSuccess()
        assertEquals(0, actionCount)
        assertFalse(session.isInProgress)
    }

    @Test
    fun promptExceptionDoesNotAuthorizeActionAndAllowsRetry() {
        var reportedError: Exception? = null
        val failure = IllegalArgumentException("Prompt unavailable")
        assertFalse(session.authenticate(
            action = { actionCount++ },
            launchPrompt = { throw failure },
            onLaunchError = { reportedError = it }
        ))
        session.onSuccess()
        assertSame(failure, reportedError)
        assertEquals(0, actionCount)
        assertFalse(session.isInProgress)
        assertTrue(start())
        session.onSuccess()
        assertEquals(1, actionCount)
    }

    @Test
    fun credentialActivityPauseKeepsRequestAndDefersSuccessUntilResume() {
        start()
        session.onPause()
        assertTrue(session.isInProgress)
        assertTrue(session.wasPausedDuringAuthentication)
        session.onSuccess()
        assertEquals(0, actionCount)
        session.onResume()
        assertEquals(1, actionCount)
        assertFalse(session.isInProgress)
        session.onResume()
        assertEquals(1, actionCount)
    }

    @Test
    fun resumeBeforeCredentialResultDoesNotRestartOrAuthorizeAuthentication() {
        start()
        session.onPause()
        session.onResume()
        assertTrue(session.wasPausedDuringAuthentication)
        assertFalse(start())
        assertEquals(0, actionCount)
        session.onSuccess()
        assertEquals(1, actionCount)
    }

    @Test
    fun errorWhilePausedDiscardsDeferredSuccess() {
        start()
        session.onPause()
        session.onSuccess()
        session.onError()
        session.onResume()
        assertEquals(0, actionCount)
        assertFalse(session.isInProgress)
    }

    @Test
    fun ordinaryBackgroundReturnDoesNotAuthorizeAnything() {
        session.onPause()
        session.onResume()
        session.onSuccess()
        assertEquals(0, actionCount)
        assertFalse(session.isInProgress)
    }

    @Test
    fun overlappingRequestCannotReplaceActionOrLaunchSecondPrompt() {
        start()
        var secondPromptLaunched = false
        assertFalse(session.authenticate(
            action = { actionCount += 10 },
            launchPrompt = { secondPromptLaunched = true },
            onLaunchError = { throw AssertionError(it) }
        ))
        session.onSuccess()
        assertFalse(secondPromptLaunched)
        assertEquals(1, actionCount)
    }
}
