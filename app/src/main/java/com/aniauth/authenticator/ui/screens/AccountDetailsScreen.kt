package com.aniauth.authenticator.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
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
    onDelete: () -> Unit
) {
    var label by remember { mutableStateOf(account.label) }
    var username by remember { mutableStateOf(account.username ?: "") }
    var isSecretVisible by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    val decryptedSecret = remember(account.encryptedSecret) {
        KeyStoreHelper.decrypt(account.encryptedSecret) ?: "Error decrypting"
    }
    
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Account Details", fontSize = 20.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                // Editable Issuer/Label
                TextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Issuer", color = TextSecondary) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedIndicatorColor = PurpleAccent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                     ),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                )

                // Editable Username
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Name / ID", color = TextSecondary) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedIndicatorColor = PurpleAccent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                )

                // Read-only Secret Key with Visibility Toggle
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
                            IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                                Icon(
                                    imageVector = if (isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = { 
                                clipboardManager.setText(AnnotatedString(decryptedSecret))
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
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account", color = Color.Red, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdate(label.trim(), username.trim().takeIf { it.isNotEmpty() }) },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
            ) {
                Text("Save Changes", color = TextPrimary)
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
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
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
