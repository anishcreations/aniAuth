package com.aniauth.authenticator.wearos.model

import java.util.UUID

data class Account(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val encryptedSecret: String,
    val issuer: String? = null,
    val username: String? = null
)
