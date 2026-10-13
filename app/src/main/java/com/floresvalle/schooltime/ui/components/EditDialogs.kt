package com.floresvalle.schooltime.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.floresvalle.schooltime.data.entity.ClassSessionEntity
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.TaskEntity

@Composable
fun ClassDetailEditDialog(
    session: ClassSessionEntity,
    canEdit: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (date: String, start: String, end: String, docente: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var date by remember { mutableStateOf(session.sessionDate) }
    var start by remember { mutableStateOf(session.startTime) }
    var end by remember { mutableStateOf(session.endTime) }
    var docente by remember { mutableStateOf(session.docente ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (canEdit) "Editar Sesión de Clase" else "Detalle de Clase",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = session.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (canEdit) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Fecha (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = start,
                            onValueChange = { start = it },
                            label = { Text("Inicio (HH:mm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = end,
                            onValueChange = { end = it },
                            label = { Text("Fin (HH:mm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = docente,
                        onValueChange = { docente = it },
                        label = { Text("Docente / Profesor") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📅 Fecha: ${session.sessionDate}", style = MaterialTheme.typography.bodyMedium)
                            Text("⏰ Horario: ${session.startTime} - ${session.endTime}", style = MaterialTheme.typography.bodyMedium)
                            if (!session.docente.isNullOrBlank()) {
                                Text("👨‍🏫 Profesor: ${session.docente}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (!session.locationRoom.isNullOrBlank()) {
                                Text("📍 Aula / Lugar: ${session.locationRoom}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("Modalidad: ${session.modality}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canEdit && onDelete != null) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Eliminar")
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text(if (canEdit) "Cancelar" else "Cerrar")
                        }
                        if (canEdit) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onSave(date, start, end, docente)
                                }
                            ) {
                                Text("Guardar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskDetailEditDialog(
    task: TaskEntity,
    canEdit: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (date: String, time: String) -> Unit
) {
    var date by remember { mutableStateOf(task.dueDate) }
    var time by remember { mutableStateOf(task.dueTime) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (canEdit) "Editar Tarea / Trabajo" else "Detalle de Tarea",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (canEdit) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Fecha de Entrega (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Hora de Entrega (HH:mm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📅 Entrega: ${task.dueDate} a las ${task.dueTime}", style = MaterialTheme.typography.bodyMedium)
                            Text("Estado: ${task.status}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Urgencia: ${task.urgency}", style = MaterialTheme.typography.bodySmall)
                            task.grade?.let {
                                Text("Nota obtenida: $it / 20", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (canEdit) "Cancelar" else "Cerrar")
                    }
                    if (canEdit) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSave(date, time)
                            }
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExamDetailEditDialog(
    exam: ExamEntity,
    canEdit: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (date: String, time: String) -> Unit
) {
    var date by remember { mutableStateOf(exam.examDate) }
    var time by remember { mutableStateOf(exam.examTime) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (canEdit) "Editar Examen / Evaluación" else "Detalle de Examen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "${exam.type}: ${exam.courseName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (canEdit) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Fecha de Examen (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Hora de Inicio (HH:mm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📅 Fecha: ${exam.examDate} a las ${exam.examTime}", style = MaterialTheme.typography.bodyMedium)
                            Text("Estado: ${exam.status}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            exam.grade?.let {
                                Text("Nota obtenida: $it / 20", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            if (!exam.location.isNullOrBlank()) {
                                Text("📍 Aula / Sede: ${exam.location}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (canEdit) "Cancelar" else "Cerrar")
                    }
                    if (canEdit) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSave(date, time)
                            }
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GradeConfirmationDialog(
    itemTitle: String,
    onDismiss: () -> Unit,
    onConfirmed: (Double) -> Unit,
    onNotDelivered: () -> Unit
) {
    var gradeText by remember { mutableStateOf("16") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calificar: $itemTitle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ingresa la calificación obtenida (0 a 20):")
                OutlinedTextField(
                    value = gradeText,
                    onValueChange = { gradeText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val grade = gradeText.toDoubleOrNull() ?: 0.0
                    onConfirmed(grade)
                }
            ) {
                Text("Confirmar Calificación")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onNotDelivered) {
                    Text("No entregado", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}
