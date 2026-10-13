package com.floresvalle.schooltime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.TaskEntity
import com.floresvalle.schooltime.ui.components.AddContentBottomSheet
import com.floresvalle.schooltime.ui.components.ExamDetailEditDialog
import com.floresvalle.schooltime.ui.components.GradeConfirmationDialog
import com.floresvalle.schooltime.ui.components.TaskDetailEditDialog
import com.floresvalle.schooltime.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksExamsScreen(
    viewModel: SessionViewModel,
    onBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val allTasks: List<TaskEntity> by viewModel.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val allExams: List<ExamEntity> by viewModel.exams.collectAsStateWithLifecycle(initialValue = emptyList())
    val pendingTasks = remember(allTasks) { allTasks.filter { it.status == "Pendiente" } }
    val deliveredTasks = remember(allTasks) { allTasks.filter { it.status != "Pendiente" } }
    val upcomingExams = remember(allExams) { allExams.filter { it.status == "Pendiente" } }
    val completedExams = remember(allExams) { allExams.filter { it.status != "Pendiente" } }

    // EDIT dialog states - Tasks & Exams CAN be edited here!
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var editingExam by remember { mutableStateOf<ExamEntity?>(null) }
    var gradingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var gradingExam by remember { mutableStateOf<ExamEntity?>(null) }
    var showAddBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evaluaciones y Trabajos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar evaluación")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Tareas (${pendingTasks.size})") },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Exámenes (${upcomingExams.size})") },
                    icon = { Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                if (selectedTab == 0) {
                    // TASKS TAB
                    if (pendingTasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "Tareas Pendientes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(pendingTasks, key = { "pending_t_${it.id}" }) { task ->
                            TaskItemCard(
                                task = task,
                                onEdit = { editingTask = task },
                                onGrade = { gradingTask = task },
                                onDelete = { viewModel.deleteTask(task.id) }
                            )
                        }
                    }

                    if (deliveredTasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "Tareas Entregadas / Calificadas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(deliveredTasks, key = { "deliv_t_${it.id}" }) { task ->
                            TaskItemCard(
                                task = task,
                                onEdit = { editingTask = task },
                                onGrade = { gradingTask = task },
                                onDelete = { viewModel.deleteTask(task.id) }
                            )
                        }
                    }

                    if (pendingTasks.isEmpty() && deliveredTasks.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(8.dp))
                                    Text("¡No tienes tareas registradas!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Usa el botón + para agregar tareas y trabajos", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                } else {
                    // EXAMS TAB
                    if (upcomingExams.isNotEmpty()) {
                        item {
                            Text(
                                text = "Exámenes Próximos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(upcomingExams, key = { "upc_e_${it.id}" }) { exam ->
                            ExamItemCard(
                                exam = exam,
                                onEdit = { editingExam = exam },
                                onGrade = { gradingExam = exam },
                                onDelete = { viewModel.deleteExam(exam.id) }
                            )
                        }
                    }

                    if (completedExams.isNotEmpty()) {
                        item {
                            Text(
                                text = "Exámenes Rendidos / Calificados",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(completedExams, key = { "comp_e_${it.id}" }) { exam ->
                            ExamItemCard(
                                exam = exam,
                                onEdit = { editingExam = exam },
                                onGrade = { gradingExam = exam },
                                onDelete = { viewModel.deleteExam(exam.id) }
                            )
                        }
                    }

                    if (upcomingExams.isEmpty() && completedExams.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(8.dp))
                                    Text("¡No tienes exámenes programados!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Usa el botón + para registrar fechas de parciales o finales", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }

    // In Evaluaciones: Tasks CAN be edited!
    editingTask?.let { task ->
        TaskDetailEditDialog(
            task = task,
            canEdit = true, // EDITABLE in Tasks/Exams screen!
            onDismiss = { editingTask = null },
            onSave = { newDate, newTime ->
                viewModel.updateTaskDateTime(task.id, newDate, newTime)
                editingTask = null
            }
        )
    }

    // In Evaluaciones: Exams CAN be edited!
    editingExam?.let { exam ->
        ExamDetailEditDialog(
            exam = exam,
            canEdit = true, // EDITABLE in Tasks/Exams screen!
            onDismiss = { editingExam = null },
            onSave = { newDate, newTime ->
                viewModel.updateExamDateTime(exam.id, newDate, newTime)
                editingExam = null
            }
        )
    }

    gradingTask?.let { task ->
        GradeConfirmationDialog(
            itemTitle = task.title,
            onDismiss = { gradingTask = null },
            onConfirmed = { grade ->
                viewModel.completeTaskWithGrade(task.id, grade)
                gradingTask = null
            },
            onNotDelivered = {
                viewModel.markTaskAsNotDelivered(task.id)
                gradingTask = null
            }
        )
    }

    gradingExam?.let { exam ->
        GradeConfirmationDialog(
            itemTitle = "${exam.type}: ${exam.courseName}",
            onDismiss = { gradingExam = null },
            onConfirmed = { grade ->
                viewModel.completeExamWithGrade(exam.id, grade)
                gradingExam = null
            },
            onNotDelivered = {
                viewModel.markExamAsNotDelivered(exam.id)
                gradingExam = null
            }
        )
    }

    if (showAddBottomSheet) {
        AddContentBottomSheet(
            viewModel = viewModel,
            allowedTypes = if (selectedTab == 0) listOf(1) else listOf(2),
            onDismiss = { showAddBottomSheet = false }
        )
    }
}

@Composable
fun TaskItemCard(
    task: TaskEntity,
    onEdit: () -> Unit,
    onGrade: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
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
                    .background(Color(0xFFEAB308), RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "${task.courseName} • Entrega: ${task.dueDate} ${task.dueTime}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                task.grade?.let {
                    Text(text = "Nota: $it / 20", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            IconButton(onClick = onGrade) {
                Icon(Icons.Default.Grading, contentDescription = "Calificar", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun ExamItemCard(
    exam: ExamEntity,
    onEdit: () -> Unit,
    onGrade: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
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
                    .background(Color(0xFFEF4444), RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = exam.type, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "${exam.courseName} • Fecha: ${exam.examDate} ${exam.examTime}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                exam.grade?.let {
                    Text(text = "Nota: $it / 20", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            IconButton(onClick = onGrade) {
                Icon(Icons.Default.Grading, contentDescription = "Calificar", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
