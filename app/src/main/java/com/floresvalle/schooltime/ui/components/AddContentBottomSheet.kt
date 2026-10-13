package com.floresvalle.schooltime.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.floresvalle.schooltime.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContentBottomSheet(
    viewModel: SessionViewModel,
    allowedTypes: List<Int> = listOf(0, 1, 2),
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableIntStateOf(allowedTypes.firstOrNull() ?: 0) }
    val sessions by viewModel.sessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val courseNames = remember(sessions) { sessions.map { it.courseName }.distinct() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Agregar a tu Horario",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (allowedTypes.size > 1) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val tabs = listOf(
                        Triple(0, "Clase", Icons.Default.School),
                        Triple(1, "Tarea", Icons.Default.Assignment),
                        Triple(2, "Examen", Icons.Default.EventNote)
                    ).filter { allowedTypes.contains(it.first) }

                    tabs.forEachIndexed { index, (typeId, label, icon) ->
                        SegmentedButton(
                            selected = selectedType == typeId,
                            onClick = { selectedType = typeId },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                            icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            when (selectedType) {
                0 -> ClassForm(
                    onSave = { name, modality, location, docente, start, end, date, room, url ->
                        viewModel.addSession(
                            courseName = name,
                            startTime = start,
                            endTime = end,
                            modality = modality,
                            sessionDate = date,
                            locationRoom = room,
                            virtualUrl = url,
                            docente = docente
                        )
                        onDismiss()
                    },
                    onCancel = onDismiss
                )
                1 -> TaskForm(
                    courses = courseNames,
                    onSave = { title, course, dueDate, dueTime, desc, urgency, notes ->
                        viewModel.addTask(
                            courseName = course,
                            title = title,
                            dueDate = dueDate,
                            dueTime = dueTime,
                            urgency = urgency
                        )
                        onDismiss()
                    },
                    onCancel = onDismiss
                )
                2 -> ExamForm(
                    courses = courseNames,
                    onSave = { title, course, examDate, startTime, location, desc, urgency ->
                        viewModel.addExam(
                            courseName = course,
                            type = title,
                            examDate = examDate,
                            examTime = startTime,
                            location = location.ifBlank { null }
                        )
                        onDismiss()
                    },
                    onCancel = onDismiss
                )
            }
        }
    }
}

@Composable
fun ClassForm(
    onSave: (name: String, modality: String, location: String, docente: String?, start: String, end: String, sessionDate: String, room: String?, url: String?) -> Unit,
    onCancel: () -> Unit
) {
    var courseName by remember { mutableStateOf("") }
    var modality by remember { mutableStateOf("Presencial") }
    var location by remember { mutableStateOf("") }
    var docente by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("10:00") }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var sessionDate by remember { mutableStateOf(todayStr) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = courseName,
            onValueChange = { courseName = it },
            label = { Text("Nombre del Curso *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = sessionDate,
            onValueChange = { sessionDate = it },
            label = { Text("Fecha (YYYY-MM-DD) *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = startTime,
                onValueChange = { startTime = it },
                label = { Text("Inicio (HH:mm)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = endTime,
                onValueChange = { endTime = it },
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

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Aula o Enlace Virtual") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (courseName.isNotBlank()) {
                        val isVirtual = location.startsWith("http")
                        val mod = if (isVirtual) "Virtual" else modality
                        val room = if (!isVirtual) location.ifBlank { null } else null
                        val url = if (isVirtual) location.ifBlank { null } else null
                        onSave(courseName, mod, location, docente.ifBlank { null }, startTime, endTime, sessionDate, room, url)
                    }
                },
                enabled = courseName.isNotBlank()
            ) {
                Text("Guardar Clase")
            }
        }
    }
}

@Composable
fun TaskForm(
    courses: List<String>,
    onSave: (title: String, course: String, dueDate: String, dueTime: String, desc: String, urgency: String, notes: String?) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var course by remember { mutableStateOf(courses.firstOrNull() ?: "") }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var dueDate by remember { mutableStateOf(todayStr) }
    var dueTime by remember { mutableStateOf("23:59") }
    var desc by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("Media") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Título de la Tarea / Trabajo *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = course,
            onValueChange = { course = it },
            label = { Text("Curso / Asignatura *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = dueDate,
                onValueChange = { dueDate = it },
                label = { Text("Fecha Límite (YYYY-MM-DD)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = dueTime,
                onValueChange = { dueTime = it },
                label = { Text("Hora (HH:mm)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Instrucciones o Detalles") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (title.isNotBlank() && course.isNotBlank()) {
                        onSave(title, course, dueDate, dueTime, desc, urgency, null)
                    }
                },
                enabled = title.isNotBlank() && course.isNotBlank()
            ) {
                Text("Guardar Tarea")
            }
        }
    }
}

@Composable
fun ExamForm(
    courses: List<String>,
    onSave: (title: String, course: String, examDate: String, startTime: String, location: String, desc: String, urgency: String) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var course by remember { mutableStateOf(courses.firstOrNull() ?: "") }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var examDate by remember { mutableStateOf(todayStr) }
    var startTime by remember { mutableStateOf("09:00") }
    var location by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Título del Examen (ej: Parcial, Final) *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = course,
            onValueChange = { course = it },
            label = { Text("Curso / Asignatura *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = examDate,
                onValueChange = { examDate = it },
                label = { Text("Fecha (YYYY-MM-DD)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = startTime,
                onValueChange = { startTime = it },
                label = { Text("Hora (HH:mm)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Aula o Lugar") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (title.isNotBlank() && course.isNotBlank()) {
                        onSave(title, course, examDate, startTime, location, desc, "Alta")
                    }
                },
                enabled = title.isNotBlank() && course.isNotBlank()
            ) {
                Text("Guardar Examen")
            }
        }
    }
}
