package com.aniauth.authenticator.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniauth.authenticator.crypto.KeyStoreHelper
import com.aniauth.authenticator.model.Account
import com.aniauth.authenticator.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsScreen(
    account: Account,
    onDismiss: () -> Unit,
    onUpdate: (label: String, username: String?) -> Unit,
    onDelete: () -> Unit,
    onVerifyIdentity: ((title: String, subtitle: String, onSuccess: () -> Unit) -> Unit)? = null
) {
    val contentHeight = (LocalConfiguration.current.screenHeightDp.dp - 200.dp)
        .coerceIn(96.dp, 420.dp)
    var label by remember { mutableStateOf(account.label) }
    var username by remember { mutableStateOf(account.username ?: "") }
    var isSecretVisible by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    val decryptedSecret = remember(account.encryptedSecret) {
        KeyStoreHelper.decrypt(account.encryptedSecret) ?: "Error decrypting"
    }
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Account Details", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = contentHeight)
                    .verticalScroll(rememberScrollState()).padding(top = 8.dp)
            ) {
                // Editable Issuer/Label
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Issuer", color = TextSecondary) },
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

                // Editable Username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Name / ID", color = TextSecondary) },
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

                // Read-only Secret Key with Visibility Toggle & Biometric Auth
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Secret Key", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                    Surface(
                        color = DarkCard,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isSecretVisible) decryptedSecret else "••••••••••••••••",
                                color = if (isSecretVisible) EmeraldGreen else TextSecondary,
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(onClick = { 
                                if (isSecretVisible) {
                                    isSecretVisible = false
                                } else {
                                    if (onVerifyIdentity != null) {
                                        onVerifyIdentity(
                                            "Reveal Secret Key",
                                            "Authenticate identity to view raw secret key"
                                        ) {
                                            isSecretVisible = true
                                        }
                                    } else {
                                        isSecretVisible = true
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = if (isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = { 
                                if (onVerifyIdentity != null) {
                                    onVerifyIdentity(
                                        "Copy Secret Key",
                                        "Authenticate identity to copy secret key"
                                    ) {
                                        clipboardManager.setText(AnnotatedString(decryptedSecret))
                                        Toast.makeText(context, "Secret key copied!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    clipboardManager.setText(AnnotatedString(decryptedSecret))
                                    Toast.makeText(context, "Secret key copied!", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy Secret",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Delete Action (Inside details for v1)
                TextButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.align(Alignment.Start),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdate(label.trim(), username.trim().takeIf { it.isNotEmpty() }) },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
            ) {
                Text("Save Changes", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextPrimary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirm Deletion", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this account? This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        if (onVerifyIdentity != null) {
                            onVerifyIdentity(
                                "Delete Account",
                                "Authenticate identity to permanently delete ${account.label}"
                            ) {
                                onDelete()
                            }
                        } else {
                            onDelete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            containerColor = DarkCard
        )
    }
}
