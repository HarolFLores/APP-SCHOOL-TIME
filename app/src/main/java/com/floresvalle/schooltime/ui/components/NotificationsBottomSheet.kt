package com.floresvalle.schooltime.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.floresvalle.schooltime.data.entity.NotificationEntity
import com.floresvalle.schooltime.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsBottomSheet(
    viewModel: SessionViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val notifications by viewModel.liveNotifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }

    var selectedFilter by remember { mutableStateOf("Todas") }
    var filterThisWeekOnly by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Notificaciones",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (unreadCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (unreadCount > 0) "$unreadCount nuevas" else "Sin nuevas",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (unreadCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (unreadCount > 0) {
                        Text(
                            text = "Marcar todas",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.markAllNotificationsAsRead() }
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)

            // Category Filter Pills + "Esta semana" Toggle
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("Todas", "Clases", "Tareas", "Exámenes")
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { selectedFilter = filter }
                        ) {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilterChip(
                    selected = filterThisWeekOnly,
                    onClick = { filterThisWeekOnly = !filterThisWeekOnly },
                    label = { Text("Esta semana", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter List
            val nowLocalDate = LocalDate.now()
            val monday = nowLocalDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val sunday = nowLocalDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

            val filteredList = notifications.filter { notif ->
                val categoryMatches = when (selectedFilter) {
                    "Clases" -> notif.category == "Clases"
                    "Tareas" -> notif.category == "Tareas"
                    "Exámenes" -> notif.category == "Exámenes"
                    else -> true
                }

                val weekMatches = if (filterThisWeekOnly) {
                    if (notif.eventDate != null) {
                        try {
                            val eventLocalDate = LocalDate.parse(notif.eventDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            !eventLocalDate.isBefore(monday) && !eventLocalDate.isAfter(sunday)
                        } catch (e: Exception) { true }
                    } else true
                } else true

                categoryMatches && weekMatches
            }

            if (filteredList.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No tienes notificaciones en esta categoría", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { notif ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                                    viewModel.dismissNotification(notif.id)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.EndToStart, SwipeToDismissBoxValue.StartToEnd -> Color(0xFFD32F2F)
                                        else -> Color.Transparent
                                    },
                                    label = "dismissColor"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White)
                                }
                            }
                        ) {
                            NotificationCardItem(
                                notification = notif,
                                onOpenUrl = { url ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCardItem(
    notification: NotificationEntity,
    onOpenUrl: (String) -> Unit
) {
    val (iconBgColor, iconColor, iconVector) = when (notification.category) {
        "Clases" -> Triple(Color(0xFFE0F2F1), Color(0xFF00695C), Icons.Default.Videocam)
        "Tareas" -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), Icons.Default.AssignmentLate)
        "Exámenes" -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), Icons.Default.NotificationsActive)
        else -> Triple(Color(0xFFE1F5FE), Color(0xFF0288D1), Icons.Default.Notifications)
    }

    val timeStr = remember(notification.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(notification.timestamp))
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = if (!notification.isRead) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(if (!notification.isRead) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconVector, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(notification.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    if (!notification.isRead) {
                        Box(modifier = Modifier.padding(top = 6.dp, start = 8.dp).size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(notification.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Classes never show video call buttons (links were removed)
                if (notification.category != "Clases" && !notification.virtualUrl.isNullOrBlank() &&
                    (notification.virtualUrl.startsWith("http://") || notification.virtualUrl.startsWith("https://"))
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onOpenUrl(notification.virtualUrl) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver Recurso", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}
