package com.floresvalle.schooltime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.floresvalle.schooltime.ui.theme.ThemePreference
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun QuickProfileDialog(
    onDismiss: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    cycleName: String = "1er Ciclo"
) {
    val context = LocalContext.current
    val isDarkMode = ThemePreference.isDarkModeState.value == "DARK"
    val currentUser = Firebase.auth.currentUser
    val userName = currentUser?.displayName ?: "Estudiante"
    val userEmail = currentUser?.email ?: "correo@uni.edu.pe"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header (Profile info & Close button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(2.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF2E7D32)))
                            }
                        }

                        Column {
                            Text(userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                    Text("Estudiante Activo • $cycleName", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                // Dark Mode Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            ThemePreference.toggleDarkMode(context)
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                "Modo Oscuro",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (isDarkMode) "Activado (tema oscuro)" else "Desactivado (predeterminado)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = null // Click is handled cleanly by Row to prevent double-toggle conflicts
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ir a Mi Perfil Completo
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().clickable {
                        onDismiss()
                        onNavigateToProfile()
                    }
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Portrait, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Ir a Mi Perfil Completo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Datos académicos, historial y ajustes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Footer (Cerrar Sesión & Version)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onLogout()
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                            Text("Cerrar Sesión", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        }
                    }

                    Text("SchoolTime v2.5.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
