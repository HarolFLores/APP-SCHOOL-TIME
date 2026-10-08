package com.floresvalle.schooltime.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.floresvalle.schooltime.data.entity.ClassSessionEntity
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

private fun formatReadableDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM, yyyy", Locale("es", "ES"))
        date.format(formatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    } catch (e: Exception) {
        dateStr
    }
}

/**
 * Diálogo modal para consultar y reprogramar el horario de un curso.
 * Compatible al 100% con Modo Claro y Modo Oscuro (usa tokens de MaterialTheme.colorScheme).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailEditDialog(
    session: ClassSessionEntity,
    canEdit: Boolean = true,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (courseName: String, docente: String?, startTime: String, endTime: String, sessionDate: String) -> Unit
) {
    var isEditMode by remember { mutableStateOf(false) }
    var courseName by remember { mutableStateOf(session.courseName) }
    var startTime by remember { mutableStateOf(session.startTime) }
    var endTime by remember { mutableStateOf(session.endTime) }
    var sessionDate by remember { mutableStateOf(session.sessionDate) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()
    val endTimePickerState = rememberTimePickerState()

    val defaultPrimary = MaterialTheme.colorScheme.primary
    val courseThemeColor = remember(session.color, defaultPrimary) {
        if (!session.color.isNullOrBlank()) {
            try {
                Color(android.graphics.Color.parseColor(session.color))
            } catch (e: Exception) {
                defaultPrimary
            }
        } else {
            defaultPrimary
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .heightIn(max = 640.dp)
                .wrapContentHeight()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Top Header: Badge + (Si canEdit) Botón de Lápiz
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                "ASIGNATURA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                if (canEdit) "Detalles y Modificación" else "Solo Visualización",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (canEdit) {
                        FilterChip(
                            selected = isEditMode,
                            onClick = {
                                isEditMode = !isEditMode
                                if (!isEditMode) {
                                    startTime = session.startTime
                                    endTime = session.endTime
                                    sessionDate = session.sessionDate
                                }
                            },
                            label = {
                                Text(
                                    if (isEditMode) "Editando" else "Editar",
                                    fontWeight = if (isEditMode) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Activar edición",
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                labelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Course Name & Badges
                if (isEditMode) {
                    OutlinedTextField(
                        value = courseName,
                        onValueChange = { courseName = it },
                        label = { Text("Nombre del Curso / Asignatura") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Text(
                        text = session.courseName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Badges Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isVirtual = session.modality.equals("Virtual", ignoreCase = true)
                    val modBg = if (isVirtual) Color(0xFFE0F2FE) else Color(0xFFDCFCE7)
                    val modOnBg = if (isVirtual) Color(0xFF0369A1) else Color(0xFF15803D)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = modBg,
                        border = BorderStroke(1.dp, modOnBg.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                if (isVirtual) Icons.Default.Videocam else Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = modOnBg,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                session.modality,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = modOnBg
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            Text(
                                "Activo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Banner si el modo edición está activo
                if (isEditMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(
                                "Modifica la fecha o el horario de tu clase.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Datos informativos (Docente y Aula)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Docente", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    session.docente?.ifBlank { "Docente no especificado" } ?: "Docente no especificado",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Ubicación / Aula", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    session.locationRoom ?: session.virtualUrl ?: "Aula universitaria asignada",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECCIÓN HORARIO
                Text(
                    text = "PROGRAMACIÓN DE HORARIO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Fecha
                if (!isEditMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Fecha de Cátedra", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatReadableDate(sessionDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("Fecha (Toca para cambiar)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(formatReadableDate(sessionDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Horas (Inicio / Fin)
                if (!isEditMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Horario de Clase", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$startTime - $endTime", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showStartTimePicker = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Inicio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(startTime, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showEndTimePicker = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Fin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(endTime, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botones de acción inferiores
                if (!isEditMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (canEdit && onDelete != null) {
                            OutlinedButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Eliminar", fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!canEdit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!canEdit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (canEdit) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                        ) {
                            Text("Cerrar", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isEditMode = false
                                courseName = session.courseName
                                startTime = session.startTime
                                endTime = session.endTime
                                sessionDate = session.sessionDate
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = {
                                onSave(courseName, session.docente, startTime, endTime, sessionDate)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Eliminar Asignatura") },
            text = { Text("¿Deseas eliminar este curso y sus sesiones asociadas del horario?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete?.invoke()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        sessionDate = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDatePicker = false
                }) { Text("OK", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            onCancel = { showStartTimePicker = false },
            onConfirm = {
                startTime = String.format(Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute)
                showStartTimePicker = false
            }
        ) { TimePicker(state = timePickerState) }
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onCancel = { showEndTimePicker = false },
            onConfirm = {
                endTime = String.format(Locale.getDefault(), "%02d:%02d", endTimePickerState.hour, endTimePickerState.minute)
                showEndTimePicker = false
            }
        ) { TimePicker(state = endTimePickerState) }
    }
}

/**
 * Diálogo modal para consultar y modificar la fecha y hora de una TAREA.
 * Compatible al 100% con Modo Claro y Modo Oscuro (usa tokens de MaterialTheme.colorScheme).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailEditDialog(
    task: TaskEntity,
    canEdit: Boolean = true,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (title: String, dueDate: String, dueTime: String) -> Unit
) {
    var isEditMode by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(task.title) }
    var dueDate by remember { mutableStateOf(task.dueDate) }
    var dueTime by remember { mutableStateOf(task.dueTime.ifBlank { "23:59" }) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()

    val taskAccentColor = MaterialTheme.colorScheme.error
    val taskBadgeContainer = MaterialTheme.colorScheme.errorContainer
    val taskBadgeOnContainer = MaterialTheme.colorScheme.onErrorContainer

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .heightIn(max = 640.dp)
                .wrapContentHeight()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header Bar: Badge + (Si canEdit) Lápiz
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEE2E2))
                                .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                "TRABAJO ACADÉMICO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFDC2626),
                                letterSpacing = 1.sp
                            )
                            Text(
                                if (canEdit) "Detalles y Modificación" else "Solo Visualización",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (canEdit) {
                        FilterChip(
                            selected = isEditMode,
                            onClick = {
                                isEditMode = !isEditMode
                                if (!isEditMode) {
                                    dueDate = task.dueDate
                                    dueTime = task.dueTime.ifBlank { "23:59" }
                                }
                            },
                            label = {
                                Text(
                                    if (isEditMode) "Editando" else "Editar",
                                    fontWeight = if (isEditMode) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Activar edición",
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isEditMode) Color.White else Color(0xFFDC2626)
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDC2626),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFFEE2E2),
                                labelColor = Color(0xFFDC2626)
                            ),
                            border = BorderStroke(1.dp, if (isEditMode) Color(0xFFDC2626) else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = task.courseName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFDC2626)
                )
                if (isEditMode) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título de la Tarea") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFDC2626),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Text(
                            text = "Prioridad: ${task.urgency}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "Modalidad: ${task.modality}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isEditMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEE2E2),
                        border = BorderStroke(1.dp, Color(0xFFF87171)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            Text(
                                "Modifica la fecha límite de entrega.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // FECHA LÍMITE
                Text(
                    "FECHA LÍMITE DE ENTREGA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (!isEditMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Fecha de Entrega", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatReadableDate(dueDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, Color(0xFFDC2626)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFEE2E2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("Fecha de Entrega (Toca para cambiar)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                    Text(formatReadableDate(dueDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // HORA LÍMITE
                if (!isEditMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Hora Límite", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(dueTime, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, Color(0xFFDC2626)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFEE2E2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text("Hora Límite (Toca para cambiar)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                    Text(dueTime, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botones inferiores
                if (!isEditMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (canEdit && onDelete != null) {
                            OutlinedButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Eliminar", fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!canEdit) Color(0xFFDC2626) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!canEdit) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (canEdit) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                        ) {
                            Text("Cerrar", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isEditMode = false
                                title = task.title
                                dueDate = task.dueDate
                                dueTime = task.dueTime.ifBlank { "23:59" }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = {
                                onSave(title, dueDate, dueTime)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Eliminar Tarea") },
            text = { Text("¿Estás seguro de que deseas eliminar esta tarea?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete?.invoke()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        dueDate = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDatePicker = false
                }) { Text("OK", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onCancel = { showTimePicker = false },
            onConfirm = {
                dueTime = String.format(Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute)
                showTimePicker = false
            }
        ) { TimePicker(state = timePickerState) }
    }
}

/**
 * Diálogo modal para consultar y modificar la fecha y hora de un EXAMEN.
 * Compatible al 100% con Modo Claro y Modo Oscuro (usa tokens de MaterialTheme.colorScheme).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailEditDialog(
    exam: ExamEntity,
    canEdit: Boolean = true,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (type: String, examDate: String, examTime: String) -> Unit
) {
    var isEditMode by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(exam.type) }
    var examDate by remember { mutableStateOf(exam.examDate) }
    var examTime by remember { mutableStateOf(exam.examTime.ifBlank { "08:00 - 10:00" }) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()

    val examAccentColor = MaterialTheme.colorScheme.tertiary
    val examBadgeContainer = MaterialTheme.colorScheme.tertiaryContainer
    val examBadgeOnContainer = MaterialTheme.colorScheme.onTertiaryContainer

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .heightIn(max = 640.dp)
                .wrapContentHeight()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header Bar: Badge + (Si canEdit) Lápiz
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(1.dp, Color(0xFFFCD34D), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.EventNote,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                "EVALUACIÓN ACADÉMICA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFD97706),
                                letterSpacing = 1.sp
                            )
                            Text(
                                if (canEdit) "Detalles y Modificación" else "Solo Visualización",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (canEdit) {
                        FilterChip(
                            selected = isEditMode,
                            onClick = {
                                isEditMode = !isEditMode
                                if (!isEditMode) {
                                    examDate = exam.examDate
                                    examTime = exam.examTime.ifBlank { "08:00 - 10:00" }
                                }
                            },
                            label = {
                                Text(
                                    if (isEditMode) "Editando" else "Editar",
                                    fontWeight = if (isEditMode) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Activar edición",
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isEditMode) Color.White else Color(0xFFD97706)
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFD97706),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFFEF3C7),
                                labelColor = Color(0xFFD97706)
                            ),
                            border = BorderStroke(1.dp, if (isEditMode) Color(0xFFD97706) else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = exam.courseName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFD97706)
                )
                if (isEditMode) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Tipo de Examen (ej: Parcial, Final, Práctica)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD97706),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Text(
                        text = exam.type,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFCD34D))
                    ) {
                        Text(
                            text = "Ponderación: ${exam.weight ?: "20%"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = exam.modality,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isEditMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                            Text(
                                "Modifica la fecha programada del examen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // FECHA DE EXAMEN
                Text(
                    "FECHA PROGRAMADA DEL EXAMEN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (!isEditMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Fecha de Evaluación", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatReadableDate(examDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, Color(0xFFD97706)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("Fecha de Examen (Toca para cambiar)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                    Text(formatReadableDate(examDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // RANGO DE HORAS
                if (!isEditMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Horario de Examen", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(examTime, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, Color(0xFFD97706)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text("Rango de Horas (Toca para ajustar)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                    Text(examTime, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botones inferiores
                if (!isEditMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (canEdit && onDelete != null) {
                            OutlinedButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Eliminar", fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!canEdit) Color(0xFFD97706) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!canEdit) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (canEdit) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                        ) {
                            Text("Cerrar", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isEditMode = false
                                type = exam.type
                                examDate = exam.examDate
                                examTime = exam.examTime.ifBlank { "08:00 - 10:00" }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = {
                                onSave(type, examDate, examTime)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD97706),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Eliminar Examen") },
            text = { Text("¿Estás seguro de que deseas eliminar este examen?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete?.invoke()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        examDate = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDatePicker = false
                }) { Text("OK", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onCancel = { showTimePicker = false },
            onConfirm = {
                val hour = timePickerState.hour
                val min = timePickerState.minute
                val endHour = (hour + 2) % 24
                examTime = String.format(Locale.getDefault(), "%02d:%02d - %02d:%02d", hour, min, endHour, min)
                showTimePicker = false
            }
        ) { TimePicker(state = timePickerState) }
    }
}

@Composable
fun GradeConfirmationDialog(
    itemTitle: String,
    onDismiss: () -> Unit,
    onConfirmed: (grade: Double?) -> Unit,
    onNotDelivered: () -> Unit
) {
    var isDelivered by remember { mutableStateOf<Boolean?>(null) }
    var gradeText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Cierre de Evaluación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(itemTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(16.dp))
                Text("¿Entregaste esta evaluación?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { isDelivered = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDelivered == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isDelivered == true) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sí, entregado", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onNotDelivered()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("No entregué", fontWeight = FontWeight.Bold)
                    }
                }

                if (isDelivered == true) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Ingresa tu calificación obtenida (0 - 20):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = gradeText,
                        onValueChange = {
                            gradeText = it
                            showError = false
                        },
                        placeholder = { Text("Ej. 17.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = showError
                    )
                    if (showError) {
                        Text("Ingresa una nota válida entre 0 y 20", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val gradeVal = gradeText.toDoubleOrNull()
                            if (gradeVal != null && gradeVal in 0.0..20.0) {
                                onConfirmed(gradeVal)
                                onDismiss()
                            } else {
                                showError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar Nota y Finalizar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
