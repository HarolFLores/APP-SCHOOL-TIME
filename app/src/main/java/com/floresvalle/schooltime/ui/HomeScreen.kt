package com.floresvalle.schooltime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.floresvalle.schooltime.data.entity.ClassSessionEntity
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.TaskEntity
import com.floresvalle.schooltime.ui.components.AddContentBottomSheet
import com.floresvalle.schooltime.ui.components.ClassDetailEditDialog
import com.floresvalle.schooltime.ui.components.ExamDetailEditDialog
import com.floresvalle.schooltime.ui.components.TaskDetailEditDialog
import com.floresvalle.schooltime.viewmodel.SessionViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SessionViewModel,
    onNavigateToProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenQuickProfile: () -> Unit = {}
) {
    val userData by viewModel.currentUserData.collectAsStateWithLifecycle()
    val allSessions: List<ClassSessionEntity> by viewModel.sessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val allTasks: List<TaskEntity> by viewModel.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val allExams: List<ExamEntity> by viewModel.exams.collectAsStateWithLifecycle(initialValue = emptyList())
    val nextClassState by viewModel.nextClassState.collectAsStateWithLifecycle()
    val averageGrade by viewModel.averageGrade.collectAsStateWithLifecycle(initialValue = 0.0)

    val todayStr = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
    val todaySessions = remember(allSessions, todayStr) {
        allSessions.filter { it.sessionDate == todayStr }
    }

    var selectedSessionForView by remember { mutableStateOf<ClassSessionEntity?>(null) }
    var selectedTaskForView by remember { mutableStateOf<TaskEntity?>(null) }
    var selectedExamForView by remember { mutableStateOf<ExamEntity?>(null) }
    var showAddBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar contenido")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                // Top Header: User Greeting & Action Icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onOpenQuickProfile() }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Perfil",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            val greetingName = userData?.let { "${it.firstName}" } ?: "Estudiante"
                            Text(
                                text = "Hola, $greetingName 👋",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userData?.career?.ifBlank { "Ingeniería de Sistemas" } ?: "Ingeniería de Sistemas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = onOpenNotifications) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notificaciones",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Next Class / Class Highlight Card
            item {
                NextClassCard(
                    state = nextClassState,
                    onClick = { session ->
                        // ONLY view details in Home screen! No edit!
                        selectedSessionForView = session
                    }
                )
            }

            // Dashboard Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCardDashboard(
                        value = if (averageGrade > 0) String.format("%.1f", averageGrade) else "--",
                        label = "Promedio",
                        icon = Icons.Default.TrendingUp,
                        chipText = "Actual",
                        chipColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCardDashboard(
                        value = "${allTasks.size}",
                        label = "Tareas",
                        icon = Icons.Default.Assignment,
                        chipText = "Pendientes",
                        chipColor = Color(0xFFEAB308),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCardDashboard(
                        value = "${allExams.size}",
                        label = "Exámenes",
                        icon = Icons.Default.EventNote,
                        chipText = "Próximos",
                        chipColor = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Today's schedule section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Horario de Hoy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("es", "PE"))).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (todaySessions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "¡Sin clases programadas para hoy!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Aprovecha para avanzar en tus tareas o repasar para tus exámenes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(todaySessions, key = { it.id }) { session ->
                    ScheduleItemCard(
                        course = session.courseName,
                        subtitle = session.docente ?: "Docente asignado",
                        modality = session.modality,
                        time = "${session.startTime} - ${session.endTime}",
                        duration = "",
                        locationOrLink = session.locationRoom ?: session.virtualUrl ?: "Aula por confirmar",
                        isVirtual = session.modality.equals("Virtual", ignoreCase = true),
                        onClick = {
                            // ONLY VISUALIZE in Home! No edit!
                            selectedSessionForView = session
                        }
                    )
                }
            }

            // Pending Tasks & Upcoming Exams sections in Home
            if (allTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "Tareas Pendientes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(allTasks.take(3), key = { "home_task_${it.id}" }) { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // ONLY VISUALIZE in Home! No edit!
                                selectedTaskForView = task
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Assignment,
                                contentDescription = null,
                                tint = Color(0xFFEAB308),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${task.courseName} • Entrega: ${task.dueDate} ${task.dueTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "Ver detalles",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (allExams.isNotEmpty()) {
                item {
                    Text(
                        text = "Próximos Exámenes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(allExams.take(3), key = { "home_exam_${it.id}" }) { exam ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // ONLY VISUALIZE in Home! No edit!
                                selectedExamForView = exam
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.EventNote,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${exam.type}: ${exam.courseName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Fecha: ${exam.examDate} ${exam.examTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "Ver detalles",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Read-only Dialogs for Home Screen (canEdit = false)
    selectedSessionForView?.let { session ->
        ClassDetailEditDialog(
            session = session,
            canEdit = false, // Strictly VIEW ONLY in Inicio!
            onDismiss = { selectedSessionForView = null },
            onSave = { _, _, _, _ -> }
        )
    }

    selectedTaskForView?.let { task ->
        TaskDetailEditDialog(
            task = task,
            canEdit = false, // Strictly VIEW ONLY in Inicio!
            onDismiss = { selectedTaskForView = null },
            onSave = { _, _ -> }
        )
    }

    selectedExamForView?.let { exam ->
        ExamDetailEditDialog(
            exam = exam,
            canEdit = false, // Strictly VIEW ONLY in Inicio!
            onDismiss = { selectedExamForView = null },
            onSave = { _, _ -> }
        )
    }

    if (showAddBottomSheet) {
        AddContentBottomSheet(
            viewModel = viewModel,
            onDismiss = { showAddBottomSheet = false }
        )
    }
}

@Composable
fun NextClassCard(
    state: SessionViewModel.NextClassState,
    onClick: (ClassSessionEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (state) {
                is SessionViewModel.NextClassState.Live -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEF4444)
                        ) {
                            Text(
                                text = "EN VIVO AHORA",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "${state.session.startTime} - ${state.session.endTime}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = state.session.courseName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "📍 ${state.session.locationRoom ?: state.session.virtualUrl ?: state.session.modality}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = { onClick(state.session) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ver Detalle de la Clase")
                    }
                }

                is SessionViewModel.NextClassState.Upcoming -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "PRÓXIMA CLASE",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = state.timeRemainingText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = state.session.courseName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "⏰ ${state.session.startTime} - ${state.session.endTime} • ${state.session.locationRoom ?: state.session.modality}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = { onClick(state.session) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ver Detalle")
                    }
                }

                SessionViewModel.NextClassState.NoClasses -> {
                    Text(
                        text = "Sin clases pendientes hoy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Has completado tus clases o no hay clases registradas para este horario.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCardDashboard(
    value: String,
    label: String,
    icon: ImageVector,
    chipText: String,
    chipColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = chipColor,
                    modifier = Modifier.size(20.dp)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = chipColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = chipText,
                        color = chipColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(
                        if (isVirtual) Color(0xFF3B82F6) else MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "⏰ $time • $locationOrLink",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "👨‍🏫 $subtitle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isVirtual) Color(0xFFDBEAFE) else MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = modality,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isVirtual) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
