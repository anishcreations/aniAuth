package com.aniauth.authenticator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniauth.authenticator.ui.theme.DarkCard
import com.aniauth.authenticator.ui.theme.TextPrimary
import com.aniauth.authenticator.ui.theme.TextSecondary
import com.aniauth.authenticator.ui.theme.PurpleAccent
import com.aniauth.authenticator.ui.theme.BorderColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onDismiss: () -> Unit,
    onAdd: (label: String, secret: String, issuer: String?, username: String?) -> Unit
) {
    val contentHeight = (LocalConfiguration.current.screenHeightDp.dp - 200.dp)
        .coerceIn(96.dp, 420.dp)
    var secret by remember { mutableStateOf("") }
    var issuer by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    
    var errorText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Account", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = contentHeight).verticalScroll(rememberScrollState())
            ) {
                if (errorText.isNotEmpty()) {
                    Text(errorText, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                
                OutlinedTextField(
                    value = issuer,
                    onValueChange = { issuer = it },
                    label = { Text("Issuer (e.g. Google)", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Name (e.g. user@email.com)", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                )
                
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("Key (Base32)", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (issuer.isBlank() || secret.isBlank()) {
                        errorText = "Issuer and Key cannot be empty"
                        return@Button
                    }
                    val cleanSecret = secret.replace(" ", "")
                    // Minimal Base32 check
                    val base32Regex = "^[A-Z2-7]+=*$".toRegex(RegexOption.IGNORE_CASE)
                    if (!base32Regex.matches(cleanSecret)) {
                        errorText = "Invalid characters in Base32 Key"
                        return@Button
                    }
                    onAdd(
                        issuer.trim(),
                        cleanSecret,
                        null, // old issuer field, now we use label as issuer
                        username.trim().takeIf { it.isNotEmpty() }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
            ) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextPrimary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
