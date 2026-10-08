package com.floresvalle.schooltime.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.TaskEntity
import com.floresvalle.schooltime.ui.components.ExamDetailEditDialog
import com.floresvalle.schooltime.ui.components.GradeConfirmationDialog
import com.floresvalle.schooltime.ui.components.TaskDetailEditDialog
import com.floresvalle.schooltime.viewmodel.SessionViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksExamsScreen(
    viewModel: SessionViewModel,
    onOpenNotifications: () -> Unit = {},
    onOpenQuickProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableStateOf("Todos") }

    val dbTasks by viewModel.tasks.collectAsState()
    val dbExams by viewModel.exams.collectAsState()
    val liveNotifications by viewModel.liveNotifications.collectAsState()
    val unreadNotifCount = liveNotifications.count { !it.isRead }

    // Dialog States
    var taskForGrade by remember { mutableStateOf<TaskEntity?>(null) }
    var examForGrade by remember { mutableStateOf<ExamEntity?>(null) }
    var taskForEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var examForEdit by remember { mutableStateOf<ExamEntity?>(null) }

    val now = LocalDate.now()
    val nowDateTime = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    // Calculations for Progress
    val totalTasksAndExams = dbTasks.size + dbExams.size
    val completedCount = dbTasks.count { it.status == "Completado" } + dbExams.count { it.status == "Completado" }
    val pendingCount = totalTasksAndExams - completedCount
    val upcomingExamsCount by viewModel.urgentExamsCount.collectAsState()

    val progressPercent = if (totalTasksAndExams > 0) (completedCount.toFloat() / totalTasksAndExams.toFloat()) else 0f
    val progressInt = (progressPercent * 100).toInt()

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
                        Text("Evaluaciones", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Real Progress Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.elevatedCardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PROGRESO DEL CICLO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$progressInt% Completado", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$pendingCount pendientes • $upcomingExamsCount exámenes próximos (48h)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                        CircularProgressIndicator(
                            progress = { progressPercent },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                            strokeWidth = 6.dp
                        )
                        Text("$progressInt%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tabs
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Trabajos Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTabIndex = 0 }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Trabajos y\nProyectos",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTabIndex == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedTabIndex == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text("${dbTasks.size}", color = if (selectedTabIndex == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.height(3.dp).width(120.dp).clip(RoundedCornerShape(50)).background(if (selectedTabIndex == 0) MaterialTheme.colorScheme.primary else Color.Transparent))
                        }
                    }

                    // Exámenes Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTabIndex = 1 }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Exámenes",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTabIndex == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedTabIndex == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text("${dbExams.size}", color = if (selectedTabIndex == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.height(3.dp).width(100.dp).clip(RoundedCornerShape(50)).background(if (selectedTabIndex == 1) MaterialTheme.colorScheme.primary else Color.Transparent))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips with Corporate Colors
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val filters = listOf("Todos", "Urgentes", "Entregados", "No entregados")
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    val chipBg = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        filter == "Urgentes" -> MaterialTheme.colorScheme.tertiaryContainer
                        filter == "Entregados" -> MaterialTheme.colorScheme.secondaryContainer
                        filter == "No entregados" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val chipText = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        filter == "Urgentes" -> MaterialTheme.colorScheme.onTertiaryContainer
                        filter == "Entregados" -> MaterialTheme.colorScheme.onSecondaryContainer
                        filter == "No entregados" -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = chipBg,
                        onClick = { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = chipText,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filtered Items List
            if (selectedTabIndex == 0) {
                // TRABAJOS
                val filteredTasks = dbTasks.filter { task ->
                    val isCompleted = task.status == "Completado"
                    val isNotDelivered = task.status == "No entregado"

                    val isUrgent = try {
                        val limit = LocalDateTime.parse("${task.dueDate} ${task.dueTime.ifBlank { "23:59" }}", formatter)
                        limit.isAfter(nowDateTime) && ChronoUnit.HOURS.between(nowDateTime, limit) <= 48 && !isCompleted
                    } catch (e: Exception) { false }

                    val isOverdueNotDone = try {
                        val limitDate = LocalDate.parse(task.dueDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        limitDate.isBefore(now) && !isCompleted
                    } catch (e: Exception) { false }

                    when (selectedFilter) {
                        "Urgentes" -> isUrgent
                        "Entregados" -> isCompleted
                        "No entregados" -> isNotDelivered || isOverdueNotDone
                        else -> true
                    }
                }

                if (filteredTasks.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No hay trabajos en esta categoría", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredTasks, key = { it.id }) { task ->
                            TaskRowCard(
                                task = task,
                                onCheckClick = { taskForGrade = task },
                                onEditClick = { taskForEdit = task }
                            )
                        }
                    }
                }
            } else {
                // EXÁMENES
                val filteredExams = dbExams.filter { exam ->
                    val isCompleted = exam.status == "Completado"
                    val isNotDelivered = exam.status == "No entregado"

                    val isUrgent = try {
                        val limit = LocalDateTime.parse("${exam.examDate} 23:59", formatter)
                        limit.isAfter(nowDateTime) && ChronoUnit.HOURS.between(nowDateTime, limit) <= 48 && !isCompleted
                    } catch (e: Exception) { false }

                    val isOverdueNotDone = try {
                        val limitDate = LocalDate.parse(exam.examDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        limitDate.isBefore(now) && !isCompleted
                    } catch (e: Exception) { false }

                    when (selectedFilter) {
                        "Urgentes" -> isUrgent
                        "Entregados" -> isCompleted
                        "No entregados" -> isNotDelivered || isOverdueNotDone
                        else -> true
                    }
                }

                if (filteredExams.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No hay exámenes en esta categoría", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredExams, key = { it.id }) { exam ->
                            ExamRowCard(
                                exam = exam,
                                onCheckClick = { examForGrade = exam },
                                onEditClick = { examForEdit = exam }
                            )
                        }
                    }
                }
            }
        }
    }

    // Grade Completion Dialogs
    taskForGrade?.let { task ->
        GradeConfirmationDialog(
            itemTitle = task.title,
            onDismiss = { taskForGrade = null },
            onConfirmed = { grade ->
                viewModel.completeTaskWithGrade(task.id, grade)
                Toast.makeText(context, "¡Calificación de $grade guardada con éxito!", Toast.LENGTH_SHORT).show()
            },
            onNotDelivered = {
                viewModel.markTaskAsNotDelivered(task.id)
                Toast.makeText(context, "¡A darlo todo en la siguiente evaluación!", Toast.LENGTH_LONG).show()
            }
        )
    }

    examForGrade?.let { exam ->
        GradeConfirmationDialog(
            itemTitle = "${exam.type} - ${exam.courseName}",
            onDismiss = { examForGrade = null },
            onConfirmed = { grade ->
                viewModel.completeExamWithGrade(exam.id, grade)
                Toast.makeText(context, "¡Calificación de $grade guardada con éxito!", Toast.LENGTH_SHORT).show()
            },
            onNotDelivered = {
                viewModel.markExamAsNotDelivered(exam.id)
                Toast.makeText(context, "¡A darlo todo en la siguiente evaluación!", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Edit Dialogs
    taskForEdit?.let { task ->
        TaskDetailEditDialog(
            task = task,
            canEdit = true,
            onDismiss = { taskForEdit = null },
            onDelete = {
                viewModel.deleteTask(task.id)
                taskForEdit = null
            },
            onSave = { newTitle, dueDate, dueTime ->
                viewModel.updateTaskDetails(task.id, newTitle, dueDate, dueTime)
                taskForEdit = null
            }
        )
    }

    examForEdit?.let { exam ->
        ExamDetailEditDialog(
            exam = exam,
            canEdit = true,
            onDismiss = { examForEdit = null },
            onDelete = {
                viewModel.deleteExam(exam.id)
                examForEdit = null
            },
            onSave = { newType, examDate, examTime ->
                viewModel.updateExamDetails(exam.id, newType, examDate, examTime)
                examForEdit = null
            }
        )
    }
}

@Composable
fun TaskRowCard(
    task: TaskEntity,
    onCheckClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val isCompleted = task.status == "Completado"
    val isNotDelivered = task.status == "No entregado"

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onEditClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) Color(0xFF2E7D32) else if (isNotDelivered) Color(0xFFC62828) else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onCheckClick() },
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                } else if (isNotDelivered) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(task.courseName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Surface(shape = RoundedCornerShape(50), color = if (isCompleted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)) {
                        Text(
                            text = if (isCompleted && task.grade != null) "Nota: ${task.grade}" else task.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCompleted) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Vence: ${task.dueDate} ${task.dueTime}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ExamRowCard(
    exam: ExamEntity,
    onCheckClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val isCompleted = exam.status == "Completado"
    val isNotDelivered = exam.status == "No entregado"

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onEditClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) Color(0xFF2E7D32) else if (isNotDelivered) Color(0xFFC62828) else Color(0xFFE8EAF6))
                    .clickable { onCheckClick() },
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                } else if (isNotDelivered) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(exam.courseName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    val statusBg = if (isCompleted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                    val statusColor = if (isCompleted) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                    Surface(shape = RoundedCornerShape(50), color = statusBg) {
                        Text(
                            text = if (isCompleted && exam.grade != null) "Nota: ${exam.grade}" else exam.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = exam.type,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fecha: ${exam.examDate} • ${exam.examTime}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
