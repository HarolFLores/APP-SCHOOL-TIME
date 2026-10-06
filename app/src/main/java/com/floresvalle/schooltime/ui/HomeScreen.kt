package com.floresvalle.schooltime.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floresvalle.schooltime.data.entity.ClassSessionEntity
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.TaskEntity
import com.floresvalle.schooltime.ui.components.ClassDetailEditDialog
import com.floresvalle.schooltime.ui.components.ExamDetailEditDialog
import com.floresvalle.schooltime.ui.components.TaskDetailEditDialog
import com.floresvalle.schooltime.viewmodel.SessionViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SessionViewModel,
    onNavigateToProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenQuickProfile: () -> Unit = {}
) {
    val currentUser = Firebase.auth.currentUser
    val userData by viewModel.currentUserData.collectAsState()

    val userName = userData?.firstName ?: currentUser?.displayName?.split(" ")?.firstOrNull() ?: "Estudiante"
    val careerName = userData?.career ?: "Carrera Profesional"
    val cycleName = userData?.currentCycle ?: "1er Ciclo"
    val semesterName = userData?.currentSemester ?: "SEMESTRE ACTUAL"

    val currentDateStr = remember {
        val formatter = SimpleDateFormat("EEEE, d 'de' MMMM yyyy", Locale.getDefault())
        formatter.format(Date()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    var currentTab by remember { mutableIntStateOf(0) }

    val sessions by viewModel.sessions.collectAsState()
    val dbExams by viewModel.exams.collectAsState()
    val dbTasks by viewModel.tasks.collectAsState()

    val nextClassState by viewModel.nextClassState.collectAsState()
    val urgentTasksCount by viewModel.urgentTasksCount.collectAsState()
    val urgentExamsCount by viewModel.urgentExamsCount.collectAsState()
    val averageGrade by viewModel.averageGrade.collectAsState()
    val liveNotifications by viewModel.liveNotifications.collectAsState()
    val unreadNotifCount = liveNotifications.count { !it.isRead }

    // Edit Dialog States
    var selectedSessionForEdit by remember { mutableStateOf<ClassSessionEntity?>(null) }
    var selectedTaskForEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var selectedExamForEdit by remember { mutableStateOf<ExamEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text("SchoolTime", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.clickable { onOpenNotifications() }) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "Notificaciones", tint = MaterialTheme.colorScheme.onSurface)
                        if (unreadNotifCount > 0) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
                        }
                    }
                    Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.clickable { onOpenQuickProfile() }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Perfil", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        }
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF2E7D32)).padding(2.dp))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Context Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(semesterName.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(currentDateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Greeting
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "¡Hola, $userName! \uD83D\uDC4B",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$careerName • $cycleName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Metrics Row
            val registeredCourses = remember(sessions) {
                sessions.groupBy { session ->
                    val day = try {
                        val d = LocalDate.parse(session.sessionDate)
                        when (d.dayOfWeek) {
                            DayOfWeek.MONDAY -> "Lunes"
                            DayOfWeek.TUESDAY -> "Martes"
                            DayOfWeek.WEDNESDAY -> "Miércoles"
                            DayOfWeek.THURSDAY -> "Jueves"
                            DayOfWeek.FRIDAY -> "Viernes"
                            DayOfWeek.SATURDAY -> "Sábado"
                            DayOfWeek.SUNDAY -> "Domingo"
                            null -> ""
                        }
                    } catch (e: Exception) {
                        ""
                    }
                    "${session.courseName}__${session.startTime}__${session.endTime}__$day"
                }.values.mapNotNull { list ->
                    val todayStr = LocalDate.now().toString()
                    list.find { it.sessionDate >= todayStr } ?: list.firstOrNull()
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val coursesCount = registeredCourses.size

                item {
                    MetricCardDashboard(
                        value = coursesCount.toString(),
                        label = if (coursesCount == 1) "Curso Registrado" else "Cursos Registrados",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        chipText = "Activo",
                        chipColor = Color(0xFF3F51B5)
                    )
                }
                item {
                    MetricCardDashboard(
                        value = urgentExamsCount.toString(),
                        label = if (urgentExamsCount == 0) "Sin exámenes próximos" else "Exámenes (48h)",
                        icon = Icons.Default.NotificationsActive,
                        chipText = if (urgentExamsCount == 0) "Al día" else "Atención",
                        chipColor = if (urgentExamsCount == 0) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
                item {
                    MetricCardDashboard(
                        value = urgentTasksCount.toString(),
                        label = if (urgentTasksCount == 0) "Sin tareas pendientes" else "Tareas (48h)",
                        icon = Icons.Default.Checklist,
                        chipText = if (urgentTasksCount == 0) "Libre" else "Urgente",
                        chipColor = if (urgentTasksCount == 0) Color(0xFF4CAF50) else Color(0xFFE91E63)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Next Class Imminent Section
            val sectionTitle = when(nextClassState) {
                is SessionViewModel.NextClassState.InProgress -> "Clase en Curso"
                is SessionViewModel.NextClassState.Imminent -> "Próxima Clase Inminente"
                is SessionViewModel.NextClassState.Relaxed -> "Descanso"
                is SessionViewModel.NextClassState.None -> "Clases"
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (nextClassState is SessionViewModel.NextClassState.Relaxed) Icons.Default.EventAvailable else Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(sectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
                if (nextClassState is SessionViewModel.NextClassState.Imminent || nextClassState is SessionViewModel.NextClassState.InProgress) {
                    Text("PRIORIDAD ALTA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            NextClassCard(nextClassState, onClick = { session ->
                selectedSessionForEdit = session
            })
            Spacer(modifier = Modifier.height(32.dp))

            // Filter Tabs for Content
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (currentTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { currentTab = 0 }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Horario",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (currentTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (currentTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { currentTab = 1 }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Trabajos",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (currentTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (currentTab == 2) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { currentTab = 2 }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Exámenes",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (currentTab == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Content List Based on Tab
            when (currentTab) {
                0 -> {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Tus Cursos y Horarios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                        if (registeredCourses.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "${registeredCourses.size} ${if (registeredCourses.size == 1) "curso" else "cursos"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (registeredCourses.isEmpty()) {
                        Text("No has registrado ningún curso todavía. Presiona el botón + para empezar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
                    } else {
                        registeredCourses.forEach { session ->
                            val dayName = try {
                                val d = LocalDate.parse(session.sessionDate)
                                when (d.dayOfWeek) {
                                    DayOfWeek.MONDAY -> "Lunes"
                                    DayOfWeek.TUESDAY -> "Martes"
                                    DayOfWeek.WEDNESDAY -> "Miércoles"
                                    DayOfWeek.THURSDAY -> "Jueves"
                                    DayOfWeek.FRIDAY -> "Viernes"
                                    DayOfWeek.SATURDAY -> "Sábado"
                                    DayOfWeek.SUNDAY -> "Domingo"
                                    null -> ""
                                }
                            } catch (e: Exception) {
                                ""
                            }

                            ScheduleItemCard(
                                course = session.courseName,
                                subtitle = if (dayName.isNotBlank()) "Todos los $dayName • ${session.modality}" else session.modality,
                                modality = session.modality,
                                time = "${session.startTime} - ${session.endTime}",
                                duration = if (dayName.isNotBlank()) dayName else "",
                                locationOrLink = session.locationRoom ?: session.virtualUrl ?: "Aula universitaria",
                                isVirtual = session.modality.equals("Virtual", ignoreCase = true),
                                onClick = { selectedSessionForEdit = session },
                                indicatorColor = if (session.color != null) {
                                    try {
                                        Color(android.graphics.Color.parseColor(session.color))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                } else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                1 -> {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Trabajos Pendientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val activeTasks = dbTasks.filter { it.status == "Pendiente" }
                    if (activeTasks.isEmpty()) {
                        Text("No tienes trabajos pendientes en este momento.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
                    } else {
                        activeTasks.forEach { task ->
                            ItemActivityCard(
                                title = task.title,
                                course = task.courseName,
                                date = "${task.dueDate} ${task.dueTime}",
                                icon = Icons.AutoMirrored.Filled.Assignment,
                                color = MaterialTheme.colorScheme.error,
                                onClick = { selectedTaskForEdit = task }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                2 -> {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Exámenes Próximos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val activeExamsList = dbExams.filter { it.status == "Pendiente" }
                    if (activeExamsList.isEmpty()) {
                        Text("No tienes exámenes programados por ahora.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
                    } else {
                        activeExamsList.forEach { exam ->
                            ItemActivityCard(
                                title = exam.type,
                                course = exam.courseName,
                                date = "${exam.examDate} ${exam.examTime}",
                                icon = Icons.Default.NotificationsActive,
                                color = MaterialTheme.colorScheme.tertiary,
                                onClick = { selectedExamForEdit = exam }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Rendimiento Promedio Real
            val avgGradeStr = if (averageGrade != null) String.format(Locale.getDefault(), "%.2f", averageGrade) else "--"
            val hasGrades = averageGrade != null

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Badge con la nota en la IZQUIERDA: jamás queda tapada por el FAB en la esquina inferior derecha
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (hasGrades) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (hasGrades) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = avgGradeStr,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = if (hasGrades) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "de 20 pts",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasGrades) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Rendimiento Promedio",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (hasGrades) "Promedio ponderado del ciclo académico" else "Sin evaluaciones registradas aún",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val progressVal = if (averageGrade != null) (averageGrade!! / 20.0).toFloat().coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progressVal },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Dialogs (Modo solo lectura en Inicio)
        selectedSessionForEdit?.let { session ->
            ClassDetailEditDialog(
                session = session,
                canEdit = false,
                onDismiss = { selectedSessionForEdit = null },
                onSave = { docente, startTime, endTime, sessionDate ->
                    viewModel.updateSessionDetails(session.id, docente, startTime, endTime, sessionDate)
                }
            )
        }

        selectedTaskForEdit?.let { task ->
            TaskDetailEditDialog(
                task = task,
                canEdit = false,
                onDismiss = { selectedTaskForEdit = null },
                onSave = { dueDate, dueTime ->
                    viewModel.updateTaskDateTime(task.id, dueDate, dueTime)
                }
            )
        }

        selectedExamForEdit?.let { exam ->
            ExamDetailEditDialog(
                exam = exam,
                canEdit = false,
                onDismiss = { selectedExamForEdit = null },
                onSave = { examDate, examTime ->
                    viewModel.updateExamDateTime(exam.id, examDate, examTime)
                }
            )
        }
    }
}

@Composable
fun NextClassCard(
    state: SessionViewModel.NextClassState,
    onClick: (ClassSessionEntity) -> Unit = {}
) {
    when (state) {
        is SessionViewModel.NextClassState.None -> {
            Text(
                text = "No tienes clases programadas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        is SessionViewModel.NextClassState.Relaxed -> {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Sin clases hoy ni mañana", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(state.futureDateText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        is SessionViewModel.NextClassState.Imminent -> {
            RenderSessionCard(
                state.session,
                state.timeUntilText,
                "Pronto",
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = { onClick(state.session) }
            )
        }
        is SessionViewModel.NextClassState.InProgress -> {
            RenderSessionCard(
                state.session,
                state.timeRemainingText,
                "EN VIVO",
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer,
                onClick = { onClick(state.session) }
            )
        }
    }
}

@Composable
fun RenderSessionCard(
    session: ClassSessionEntity,
    timeText: String,
    badgeText: String,
    badgeBg: Color,
    badgeColor: Color,
    onClick: () -> Unit = {}
) {
    val isVirtual = session.modality.equals("Virtual", ignoreCase = true)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(50), color = badgeBg) {
                        Text(badgeText, style = MaterialTheme.typography.labelSmall, color = badgeColor, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
                    }
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(timeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
                    }
                }
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(if (isVirtual) Icons.Default.Videocam else Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(session.modality, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(session.courseName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text("Docente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text(session.docente ?: "No asignado", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Horario", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text("${session.startTime} - ${session.endTime}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isVirtual) {
                Button(
                    onClick = { /* Open link */ },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unirse a Videollamada", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(session.locationRoom ?: "Ubicación por confirmar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCardDashboard(value: String, label: String, icon: ImageVector, chipText: String, chipColor: Color) {
    ElevatedCard(
        modifier = Modifier
            .width(150.dp)
            .height(130.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = chipColor)
                }
                Surface(shape = RoundedCornerShape(50), color = chipColor.copy(alpha = 0.15f)) {
                    Text(chipText, style = MaterialTheme.typography.labelSmall, color = chipColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                }
            }
            Column {
                Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun ScheduleItemCard(
    course: String,
    subtitle: String,
    modality: String,
    time: String,
    duration: String,
    locationOrLink: String,
    isVirtual: Boolean,
    onClick: () -> Unit = {},
    indicatorColor: Color = MaterialTheme.colorScheme.primary
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp)) {
            Box(modifier = Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(50)).background(indicatorColor))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(course, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val modalityBg = if (isVirtual) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    val modalityTextColor = if (isVirtual) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    Surface(shape = RoundedCornerShape(50), color = modalityBg) {
                        Text(modality, style = MaterialTheme.typography.labelSmall, color = modalityTextColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (duration.isNotEmpty()) {
                        Text("  •  $duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (isVirtual) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = locationOrLink,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))) {
                                Text("Unirse", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            }
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = locationOrLink, 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onSurface, 
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text("Ver curso →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ItemActivityCard(
    title: String,
    course: String,
    date: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(course, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
