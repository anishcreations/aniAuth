package com.aniauth.authenticator

/** Main-thread authentication state, including returns from the system credential activity. */
internal class AuthenticationSession {
    private var pendingAction: (() -> Unit)? = null
    private var succeeded = false
    private var resumed = false
    var wasPausedDuringAuthentication = false
        private set

    val isInProgress: Boolean
        get() = pendingAction != null

    val isPaused: Boolean
        get() = !resumed

    fun authenticate(
        action: () -> Unit,
        launchPrompt: () -> Unit,
        onLaunchError: (Exception) -> Unit
    ): Boolean {
        if (isInProgress) return false
        pendingAction = action
        succeeded = false
        wasPausedDuringAuthentication = false
        try {
            launchPrompt()
        } catch (error: Exception) {
            onError()
            onLaunchError(error)
            return false
        }
        return true
    }

    fun onPause() {
        resumed = false
        if (isInProgress) wasPausedDuringAuthentication = true
    }

    fun onResume() {
        resumed = true
        deliverSuccess()
    }

    fun onSuccess() {
        if (!isInProgress) return
        succeeded = true
        deliverSuccess()
    }

    /** Also used for cancellation and exceptions while constructing or launching a prompt. */
    fun onError() {
        pendingAction = null
        succeeded = false
        wasPausedDuringAuthentication = false
    }

    private fun deliverSuccess() {
        if (!resumed || !succeeded) return
        val action = pendingAction
        pendingAction = null
        succeeded = false
        wasPausedDuringAuthentication = false
        action?.invoke()
    }
}
