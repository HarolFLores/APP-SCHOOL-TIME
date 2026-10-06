package com.floresvalle.schooltime.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.floresvalle.schooltime.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContentBottomSheet(
    viewModel: SessionViewModel,
    allowedTypes: List<Int> = listOf(0, 1, 2),
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedType by remember(allowedTypes) { mutableIntStateOf(allowedTypes.firstOrNull() ?: 0) }

    val allTypes = remember {
        listOf(
            Triple(0, "Clase", Icons.Default.School),
            Triple(1, "Trabajo", Icons.Default.Assignment),
            Triple(2, "Examen", Icons.Default.EventNote)
        )
    }
    val filteredTypes = remember(allowedTypes) {
        allTypes.filter { allowedTypes.contains(it.first) }
    }

    val headerTitle = when {
        allowedTypes == listOf(0) -> "Programar Horario de Cátedra"
        !allowedTypes.contains(0) -> "Nueva Evaluación Académica"
        else -> "Nuevo Registro Académico"
    }

    val headerSubtitle = when {
        allowedTypes == listOf(0) -> "Registra tu asignatura y horario semanal"
        !allowedTypes.contains(0) -> "Registra tus trabajos y exámenes"
        else -> "Sincronizado en la Nube con Firebase"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            headerTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676))
                            )
                            Text(
                                headerSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Selector Tabs (solo si hay más de 1 tipo disponible)
            if (filteredTypes.size > 1) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "TIPO DE CONTENIDO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            filteredTypes.forEach { (index, text, icon) ->
                                val isSelected = selectedType == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                        )
                                        .clickable { selectedType = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            val sessions by viewModel.sessions.collectAsState()
            val courses = sessions.map { it.courseName }.distinct()

            // Dynamic Form based on selected type
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .animateContentSize()
            ) {
                when (selectedType) {
                    0 -> ClassForm(
                        onSave = { course, date, start, end, mod, loc, url, doc, col ->
                            viewModel.addSession(course, date, start, end, mod, loc, url, doc, col)
                            onDismiss()
                        },
                        onCancel = onDismiss
                    )
                    1 -> TaskForm(
                        onSave = { course, title, date, time, mod, urg, link ->
                            viewModel.addTask(course, title, date, time, mod, urg, link)
                            onDismiss()
                        },
                        onCancel = onDismiss,
                        courses = courses
                    )
                    2 -> ExamForm(
                        onSave = { course, type, date, time, mod, loc, weight ->
                            viewModel.addExam(course, type, date, time, mod, loc, weight)
                            onDismiss()
                        },
                        onCancel = onDismiss,
                        courses = courses
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassForm(
    onSave: (String, String, String, String, String, String?, String?, String?, String?) -> Unit,
    onCancel: () -> Unit
) {
    var courseName by remember { mutableStateOf("") }
    var modality by remember { mutableStateOf("Presencial") }
    var location by remember { mutableStateOf("") }
    var docente by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableStateOf("Lunes") }
    var expandedDayDropdown by remember { mutableStateOf(false) }
    val daysOfWeek = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()
    val endTimePickerState = rememberTimePickerState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .heightIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Course Name
        Text(
            text = "Nombre de la Asignatura *",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = courseName,
            onValueChange = { courseName = it },
            placeholder = { Text("Ej. Seguridad Informática") },
            leadingIcon = {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = formFieldColors()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Docente Name
        Text(
            text = "Nombre del Docente (Opcional)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = docente,
            onValueChange = { docente = it },
            placeholder = { Text("Ej. Dra. Claudia Benítez") },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = formFieldColors()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Modality
        Text(
            text = "Modalidad de Clase",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { modality = "Presencial" },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = if (modality == "Presencial") BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (modality == "Presencial") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (modality == "Presencial") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Presencial", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { modality = "Virtual" },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = if (modality == "Virtual") BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (modality == "Virtual") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (modality == "Virtual") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Virtual (Online)", fontWeight = if (modality == "Virtual") FontWeight.Bold else FontWeight.Normal)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Si es presencial pide aula opcional, si es virtual NO pide enlace
        if (modality == "Presencial") {
            Text(
                text = "Aula / Laboratorio (Opcional)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                placeholder = { Text("Ej. Lab 201, Pabellón C") },
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = formFieldColors()
            )
        } else {
            // Banner elegante para clase virtual: no requiere enlace
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Modalidad 100% Virtual / En línea. No es necesario ingresar ningún enlace para agendar tu horario.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF006064)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Horarios
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hora Inicio *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { },
                    readOnly = true,
                    placeholder = { Text("08:00") },
                    trailingIcon = {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartTimePicker = true },
                    enabled = false,
                    shape = RoundedCornerShape(14.dp),
                    colors = formFieldColors(
                        textColor = if (startTime.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontWeight = if (startTime.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hora Fin *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { },
                    readOnly = true,
                    placeholder = { Text("10:00") },
                    trailingIcon = {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEndTimePicker = true },
                    enabled = false,
                    shape = RoundedCornerShape(14.dp),
                    colors = formFieldColors(
                        textColor = if (endTime.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontWeight = if (endTime.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Día Semanal
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Día Semanal de la Clase",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = expandedDayDropdown,
                onExpandedChange = { expandedDayDropdown = !expandedDayDropdown }
            ) {
                OutlinedTextField(
                    value = dayOfWeek,
                    onValueChange = { },
                    readOnly = true,
                    leadingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDayDropdown)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(14.dp),
                    colors = formFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expandedDayDropdown,
                    onDismissRequest = { expandedDayDropdown = false }
                ) {
                    daysOfWeek.forEach { day ->
                        DropdownMenuItem(
                            text = { Text(day) },
                            onClick = {
                                dayOfWeek = day
                                expandedDayDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Botones de Acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = {
                    val cal = Calendar.getInstance()
                    val targetDay = when (dayOfWeek) {
                        "Lunes" -> Calendar.MONDAY
                        "Martes" -> Calendar.TUESDAY
                        "Miércoles" -> Calendar.WEDNESDAY
                        "Jueves" -> Calendar.THURSDAY
                        "Viernes" -> Calendar.FRIDAY
                        "Sábado" -> Calendar.SATURDAY
                        "Domingo" -> Calendar.SUNDAY
                        else -> Calendar.MONDAY
                    }

                    val currentDay = cal.get(Calendar.DAY_OF_WEEK)
                    var daysToAdd = targetDay - currentDay
                    if (daysToAdd < 0) {
                        daysToAdd += 7
                    }
                    cal.add(Calendar.DAY_OF_YEAR, daysToAdd)

                    val realDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                    val loc = if (modality == "Presencial") location.ifBlank { "Aula por asignar" } else "Clase Virtual"

                    onSave(courseName.trim(), realDate, startTime, endTime, modality, loc, null, docente.trim(), null)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = courseName.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            onCancel = { showStartTimePicker = false },
            onConfirm = {
                startTime = String.format(Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute)
                showStartTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onCancel = { showEndTimePicker = false },
            onConfirm = {
                endTime = String.format(Locale.getDefault(), "%02d:%02d", endTimePickerState.hour, endTimePickerState.minute)
                showEndTimePicker = false
            }
        ) {
            TimePicker(state = endTimePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskForm(
    onSave: (String, String, String, String, String, String, String?) -> Unit,
    onCancel: () -> Unit,
    courses: List<String>
) {
    var courseName by remember { mutableStateOf(courses.firstOrNull() ?: "") }
    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("Baja") }
    var expandedCourseDropdown by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .heightIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Curso / Asignatura Asociada *",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (courses.isEmpty()) {
            OutlinedTextField(
                value = courseName,
                onValueChange = { courseName = it },
                placeholder = { Text("Ej. Matemática Discreta") },
                leadingIcon = {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = formFieldColors()
            )
        } else {
            ExposedDropdownMenuBox(
                expanded = expandedCourseDropdown,
                onExpandedChange = { expandedCourseDropdown = !expandedCourseDropdown }
            ) {
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { },
                    readOnly = true,
                    leadingIcon = {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCourseDropdown)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(14.dp),
                    colors = formFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expandedCourseDropdown,
                    onDismissRequest = { expandedCourseDropdown = false }
                ) {
                    courses.forEach { course ->
                        DropdownMenuItem(
                            text = { Text(course) },
                            onClick = {
                                courseName = course
                                expandedCourseDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Título de la Tarea / Trabajo *",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Ej. Informe del Laboratorio 3") },
            leadingIcon = {
                Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = formFieldColors()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Fecha de Entrega *",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = dueDate,
            onValueChange = { },
            readOnly = true,
            placeholder = { Text("Seleccionar Fecha") },
            trailingIcon = {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            enabled = false,
            shape = RoundedCornerShape(14.dp),
            colors = formFieldColors(
                textColor = if (dueDate.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            textStyle = LocalTextStyle.current.copy(
                fontWeight = if (dueDate.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Prioridad / Nivel de Urgencia",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(if (urgency == "Baja") 1.5.dp else 1.dp, if (urgency == "Baja") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
                color = if (urgency == "Baja") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable { urgency = "Baja" }
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Baja", style = MaterialTheme.typography.labelMedium, color = if (urgency == "Baja") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(if (urgency == "Media") 1.5.dp else 1.dp, if (urgency == "Media") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant),
                color = if (urgency == "Media") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable { urgency = "Media" }
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Media", style = MaterialTheme.typography.labelMedium, color = if (urgency == "Media") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(if (urgency == "Alta") 1.5.dp else 1.dp, if (urgency == "Alta") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant),
                color = if (urgency == "Alta") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable { urgency = "Alta" }
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Alta", style = MaterialTheme.typography.labelMedium, color = if (urgency == "Alta") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Botones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = { onSave(courseName.trim(), title.trim(), dueDate, "23:59", "Individual", urgency, null) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = courseName.isNotBlank() && title.isNotBlank() && dueDate.isNotBlank()
            ) {
                Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Registrar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
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
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamForm(
    onSave: (String, String, String, String, String, String?, String?) -> Unit,
    onCancel: () -> Unit,
    courses: List<String>
) {
    var courseName by remember { mutableStateOf(courses.firstOrNull() ?: "") }
    var type by remember { mutableStateOf("Examen Parcial") }
    var examDate by remember { mutableStateOf("") }
    var expandedCourseDropdown by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .heightIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Curso / Asignatura *",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (courses.isEmpty()) {
            OutlinedTextField(
                value = courseName,
                onValueChange = { courseName = it },
                placeholder = { Text("Ej. Cálculo de una Variable") },
                leadingIcon = {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = formFieldColors()
            )
        } else {
            ExposedDropdownMenuBox(
                expanded = expandedCourseDropdown,
                onExpandedChange = { expandedCourseDropdown = !expandedCourseDropdown }
            ) {
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { },
                    readOnly = true,
                    leadingIcon = {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCourseDropdown)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(14.dp),
                    colors = formFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expandedCourseDropdown,
                    onDismissRequest = { expandedCourseDropdown = false }
                ) {
                    courses.forEach { course ->
                        DropdownMenuItem(
                            text = { Text(course) },
                            onClick = {
                                courseName = course
                                expandedCourseDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tipo de Evaluación *",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        val examTypes = listOf("Examen Parcial", "Examen Final", "Práctica Calificada", "Evaluación Continua")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(examTypes[0], examTypes[1]).forEach { item ->
                    val isSelected = type == item
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { type = item }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                item,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(examTypes[2], examTypes[3]).forEach { item ->
                    val isSelected = type == item
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { type = item }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                item,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Fecha de la Prueba *",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = examDate,
            onValueChange = { },
            readOnly = true,
            placeholder = { Text("Seleccionar Fecha") },
            trailingIcon = {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            enabled = false,
            shape = RoundedCornerShape(14.dp),
            colors = formFieldColors(
                textColor = if (examDate.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            textStyle = LocalTextStyle.current.copy(
                fontWeight = if (examDate.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Botones de Acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(0.9f)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancelar", maxLines = 1)
            }

            Button(
                onClick = {
                    onSave(courseName.trim(), type, examDate, "08:00 - 10:00", "Presencial", "Aula por asignar", "20%")
                },
                modifier = Modifier
                    .weight(1.3f)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = courseName.isNotBlank() && examDate.isNotBlank(),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.EventAvailable,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Agendar Examen",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
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
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun TimePickerDialog(
    title: String = "Seleccionar Hora",
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    toggle: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    toggle()
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text("Cancelar") }
                    TextButton(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun formFieldColors(
    textColor: Color = MaterialTheme.colorScheme.onSurface
) = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
    disabledTextColor = textColor,
    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
)
