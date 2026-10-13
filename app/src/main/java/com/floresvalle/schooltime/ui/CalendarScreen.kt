package com.floresvalle.schooltime.ui

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.floresvalle.schooltime.data.entity.ClassSessionEntity
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.TaskEntity
import com.floresvalle.schooltime.ui.components.AddSessionBottomSheet
import com.floresvalle.schooltime.ui.components.ClassDetailEditDialog
import com.floresvalle.schooltime.ui.components.ExamDetailEditDialog
import com.floresvalle.schooltime.ui.components.TaskDetailEditDialog
import com.floresvalle.schooltime.viewmodel.SessionViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class CalendarViewMode {
    STRIP,      // Vista tira horizontal deslizante continua
    FULL_MONTH  // Vista cuadrícula completa del mes
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: SessionViewModel,
    onOpenNotifications: () -> Unit = {},
    onOpenProfileDialog: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val allSessions: List<ClassSessionEntity> by viewModel.sessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val allTasks: List<TaskEntity> by viewModel.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val allExams: List<ExamEntity> by viewModel.exams.collectAsStateWithLifecycle(initialValue = emptyList())

    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.STRIP) }

    // Multi-month continuous day list: 3 months back to 12 months ahead
    // Allows seamless continuous scrolling across month boundaries
    val continuousDays = remember {
        val startMonth = YearMonth.from(today).minusMonths(3)
        val endMonth = YearMonth.from(today).plusMonths(12)
        val list = mutableListOf<LocalDate>()
        var curr = startMonth.atDay(1)
        val end = endMonth.atEndOfMonth()
        while (!curr.isAfter(end)) {
            list.add(curr)
            curr = curr.plusDays(1)
        }
        list
    }

    val listState = rememberLazyListState()

    // Smooth centering of selected date in horizontal LazyRow
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val itemWidth = 64.dp
        val centerOffsetPx = with(density) {
            ((screenWidth - itemWidth) / 2).roundToPx()
        }

        // Animate scroll to center when selectedDate changes
        LaunchedEffect(selectedDate) {
            val index = continuousDays.indexOf(selectedDate)
            if (index >= 0) {
                listState.animateScrollToItem(index, scrollOffset = -centerOffsetPx)
            }
        }

        // Dynamic month header update as user scrolls horizontally past month end
        val centerItemIndex by remember {
            derivedStateOf {
                val first = listState.firstVisibleItemIndex
                val totalVisible = listState.layoutInfo.visibleItemsInfo.size
                first + (totalVisible / 2).coerceAtLeast(0)
            }
        }

        LaunchedEffect(centerItemIndex) {
            continuousDays.getOrNull(centerItemIndex)?.let { dateInCenter ->
                val month = YearMonth.from(dateInCenter)
                if (month != currentYearMonth) {
                    currentYearMonth = month
                }
            }
        }

        val selectedDateStr = remember(selectedDate) {
            selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }

        val dateSessions = remember(allSessions, selectedDateStr) {
            allSessions.filter { it.sessionDate == selectedDateStr }
        }
        val dateTasks = remember(allTasks, selectedDateStr) {
            allTasks.filter { it.dueDate == selectedDateStr }
        }
        val dateExams = remember(allExams, selectedDateStr) {
            allExams.filter { it.examDate == selectedDateStr }
        }

        val uniqueCourses = remember(allSessions) {
            allSessions.groupBy { it.courseName }
        }

        var selectedSessionForEdit by remember { mutableStateOf<ClassSessionEntity?>(null) }
        var selectedTaskForView by remember { mutableStateOf<TaskEntity?>(null) }
        var selectedExamForView by remember { mutableStateOf<ExamEntity?>(null) }
        var showAddSessionBottomSheet by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Calendario Escolar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentYearMonth.format(
                                    DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "PE"))
                                ).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                        }
                    },
                    actions = {
                        // Google Calendar Direct Sync Icon
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_INSERT)
                                        .setData(CalendarContract.Events.CONTENT_URI)
                                        .putExtra(CalendarContract.Events.TITLE, "Clases SchoolTime")
                                        .putExtra(CalendarContract.Events.DESCRIPTION, "Sincronización con Google Calendar")
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Abriendo Google Calendar...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = "Google Calendar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onOpenNotifications) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddSessionBottomSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Sesión")
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Month Navigation and View Mode Toggle
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Month Selector Controls & "Hoy" button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val prevMonth = currentYearMonth.minusMonths(1)
                                        currentYearMonth = prevMonth
                                        selectedDate = prevMonth.atDay(1)
                                    }
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mes Anterior")
                                }
                                Text(
                                    text = currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "PE"))
                                        .replaceFirstChar { it.uppercase() } + " " + currentYearMonth.year,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = {
                                        val nextMonth = currentYearMonth.plusMonths(1)
                                        currentYearMonth = nextMonth
                                        selectedDate = nextMonth.atDay(1)
                                    }
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Siguiente Mes")
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    selectedDate = today
                                    currentYearMonth = YearMonth.from(today)
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Hoy", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Toggle between Strip view and Full Month Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = viewMode == CalendarViewMode.STRIP,
                                onClick = { viewMode = CalendarViewMode.STRIP },
                                label = { Text("Tira Deslizable") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.ViewWeek,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = viewMode == CalendarViewMode.FULL_MONTH,
                                onClick = { viewMode = CalendarViewMode.FULL_MONTH },
                                label = { Text("Mes Completo") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // CALENDAR DISPLAY AREA (STRIP or FULL MONTH GRID)
                item {
                    if (viewMode == CalendarViewMode.STRIP) {
                        // Continuous Horizontal Day Selector
                        // STRICTLY UNIFORM SIZE: width 64.dp, height 82.dp for every single card
                        // Center aligned on tap, smooth month transition on swipe
                        LazyRow(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(continuousDays, key = { _, date -> date.toString() }) { index, date ->
                                val isSelected = date == selectedDate
                                val isDateToday = date == today
                                val hasSessions = allSessions.any { it.sessionDate == date.toString() }
                                val dayOfWeekShort = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "PE"))
                                    .replaceFirstChar { it.uppercase() }

                                Card(
                                    modifier = Modifier
                                        .width(64.dp)
                                        .height(82.dp)
                                        .clickable {
                                            selectedDate = date
                                            currentYearMonth = YearMonth.from(date)
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(index, scrollOffset = -centerOffsetPx)
                                            }
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isDateToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        }
                                    ),
                                    elevation = CardDefaults.cardElevation(if (isSelected) 3.dp else 0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = dayOfWeekShort,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${date.dayOfMonth}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        // Fixed-size dot container so card vertical height never shifts
                                        Box(
                                            modifier = Modifier.size(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasSessions) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                            else MaterialTheme.colorScheme.primary
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // FULL MONTH GRID VIEW (Eliminates the empty blank space!)
                        // Renders a complete 7-column calendar matrix for currentYearMonth
                        FullMonthGridView(
                            yearMonth = currentYearMonth,
                            selectedDate = selectedDate,
                            today = today,
                            allSessions = allSessions,
                            onDateSelected = { date ->
                                selectedDate = date
                            }
                        )
                    }
                }

                // Google Calendar Action Banner
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Vincular con Google Calendar",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Exporta o abre tu calendario de clases fácilmente",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_INSERT)
                                            .setData(CalendarContract.Events.CONTENT_URI)
                                            .putExtra(CalendarContract.Events.TITLE, "Semestre Académico - SchoolTime")
                                            .putExtra(CalendarContract.Events.DESCRIPTION, "Horario de clases")
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Abriendo Google Calendar...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Abrir", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Selected Date Activities Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDate.format(
                                DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "PE"))
                            ).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${dateSessions.size} clase(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // SESSIONS FOR SELECTED DATE (COMPLETE DETAILED CARDS)
                // In Calendar: COURSES CAN BE EDITED!
                if (dateSessions.isNotEmpty()) {
                    items(dateSessions, key = { it.id }) { session ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(44.dp)
                                                .background(
                                                    if (session.modality.equals("Virtual", ignoreCase = true)) Color(0xFF3B82F6)
                                                    else MaterialTheme.colorScheme.primary,
                                                    RoundedCornerShape(2.dp)
                                                )
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = session.courseName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "⏰ ${session.startTime} - ${session.endTime}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (session.modality.equals("Virtual", ignoreCase = true)) Color(0xFFDBEAFE) else MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = session.modality,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (session.modality.equals("Virtual", ignoreCase = true)) Color(0xFF1E40AF) else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "📍 ${session.locationRoom ?: session.virtualUrl ?: "Aula por confirmar"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!session.docente.isNullOrBlank()) {
                                        Text(
                                            text = "👨‍🏫 ${session.docente}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // Quick Action Buttons: EDIT (Allowed here in Calendar!) and Google Calendar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            addToGoogleCalendar(
                                                context = context,
                                                title = session.courseName,
                                                description = "Profesor: ${session.docente ?: "Sin asignar"} • Modalidad: ${session.modality}",
                                                location = session.locationRoom ?: session.virtualUrl ?: "Campus",
                                                dateStr = session.sessionDate,
                                                startTimeStr = session.startTime,
                                                endTimeStr = session.endTime
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Google Calendar", style = MaterialTheme.typography.labelMedium)
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Button(
                                        onClick = { selectedSessionForEdit = session },
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Editar Curso", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // Tasks on this day (Read-only in Calendar)
                if (dateTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Tareas para este día",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                    items(dateTasks, key = { "t_${it.id}" }) { task ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clickable { selectedTaskForView = task },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Assignment, contentDescription = null, tint = Color(0xFFEAB308))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("${task.courseName} • Hora: ${task.dueTime}", style = MaterialTheme.typography.bodySmall)
                                }
                                Icon(Icons.Default.Visibility, contentDescription = "Ver", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Exams on this day (Read-only in Calendar)
                if (dateExams.isNotEmpty()) {
                    item {
                        Text(
                            text = "Exámenes para este día",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                    items(dateExams, key = { "e_${it.id}" }) { exam ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clickable { selectedExamForView = exam },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.EventNote, contentDescription = null, tint = Color(0xFFEF4444))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${exam.type}: ${exam.courseName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("Hora: ${exam.examTime}", style = MaterialTheme.typography.bodySmall)
                                }
                                Icon(Icons.Default.Visibility, contentDescription = "Ver", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Empty state for selected date - clean helpful card
                if (dateSessions.isEmpty() && dateTasks.isEmpty() && dateExams.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Sin actividades para este día",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "No tienes clases ni evaluaciones registradas para el ${selectedDate.dayOfMonth} de ${selectedDate.month.getDisplayName(TextStyle.FULL, Locale("es", "PE"))}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                OutlinedButton(
                                    onClick = { showAddSessionBottomSheet = true },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Agregar Clase en esta fecha")
                                }
                            }
                        }
                    }
                }

                // FULL CYCLE COURSES OVERVIEW SECTION
                // Displays full course cards so there is never an awkward empty void
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Todos tus Cursos y Horarios",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uniqueCourses.keys.size} curso(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (uniqueCourses.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
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
                                Text(
                                    text = "Aún no tienes cursos registrados",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Button(onClick = { showAddSessionBottomSheet = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Agregar Mi Primer Curso")
                                }
                            }
                        }
                    }
                } else {
                    items(uniqueCourses.entries.toList(), key = { it.key }) { entry ->
                        val courseName = entry.key
                        val sessions = entry.value
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = courseName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${sessions.size} sesión(es)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    sessions.forEach { s ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    // In Calendar: Clicking allows editing the session!
                                                    selectedSessionForEdit = s
                                                }
                                                .padding(vertical = 4.dp, horizontal = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "📅 ${s.sessionDate} • ${s.startTime} - ${s.endTime}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "${s.locationRoom ?: s.modality}${if (!s.docente.isNullOrBlank()) " • Prof: ${s.docente}" else ""}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        addToGoogleCalendar(
                                                            context = context,
                                                            title = s.courseName,
                                                            description = "Profesor: ${s.docente ?: "Sin asignar"} • Modalidad: ${s.modality}",
                                                            location = s.locationRoom ?: s.virtualUrl ?: "Campus",
                                                            dateStr = s.sessionDate,
                                                            startTimeStr = s.startTime,
                                                            endTimeStr = s.endTime
                                                        )
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Event,
                                                        contentDescription = "Google Calendar",
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                }

                                                Spacer(Modifier.width(4.dp))

                                                IconButton(
                                                    onClick = { selectedSessionForEdit = s },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        contentDescription = "Editar",
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        // In Calendar: Course EDIT Dialog (canEdit = true)
        selectedSessionForEdit?.let { session ->
            ClassDetailEditDialog(
                session = session,
                canEdit = true, // EDITABLE in Calendar!
                onDismiss = { selectedSessionForEdit = null },
                onSave = { newDate, newStart, newEnd, newDocente ->
                    viewModel.updateSessionDetails(session.id, newStart, newEnd, newDate, newDocente.ifBlank { null })
                    selectedSessionForEdit = null
                },
                onDelete = {
                    viewModel.deleteSession(session.id)
                    selectedSessionForEdit = null
                }
            )
        }

        // In Calendar: Tasks & Exams are VIEW ONLY
        selectedTaskForView?.let { task ->
            TaskDetailEditDialog(
                task = task,
                canEdit = false,
                onDismiss = { selectedTaskForView = null },
                onSave = { _, _ -> }
            )
        }

        selectedExamForView?.let { exam ->
            ExamDetailEditDialog(
                exam = exam,
                canEdit = false,
                onDismiss = { selectedExamForView = null },
                onSave = { _, _ -> }
            )
        }

        if (showAddSessionBottomSheet) {
            AddSessionBottomSheet(
                onDismiss = { showAddSessionBottomSheet = false },
                onAddSession = { name, date, start, end, modality, room, url ->
                    viewModel.addSession(
                        courseName = name,
                        startTime = start,
                        endTime = end,
                        modality = modality,
                        sessionDate = date,
                        locationRoom = room,
                        virtualUrl = url,
                        docente = null
                    )
                    showAddSessionBottomSheet = false
                }
            )
        }
    }
}

/**
 * Full Month Grid View:
 * Renders all 28-31 days in a structured 7-column calendar matrix.
 * Shows day numbers, current selection, today indicator, and class count dots.
 * Fills the width gracefully to prevent awkward empty voids.
 */
@Composable
fun FullMonthGridView(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    allSessions: List<ClassSessionEntity>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysOfWeek = remember {
        listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
    }

    val firstDayOfMonth = remember(yearMonth) { yearMonth.atDay(1) }
    val daysInMonth = remember(yearMonth) { yearMonth.lengthOfMonth() }

    // Day of week offset (Monday = 0, Sunday = 6)
    val startDayOffset = remember(firstDayOfMonth) {
        (firstDayOfMonth.dayOfWeek.value - 1) % 7
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Days of Week Header: Lun, Mar, Mié, Jue, Vie, Sáb, Dom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEach { dow ->
                    val label = dow.getDisplayName(TextStyle.SHORT, Locale("es", "PE"))
                        .take(3)
                        .replaceFirstChar { it.uppercase() }
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Calendar Days Matrix
            val totalCells = startDayOffset + daysInMonth
            val totalRows = (totalCells + 6) / 7

            for (rowIndex in 0 until totalRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (colIndex in 0 until 7) {
                        val cellIndex = rowIndex * 7 + colIndex
                        val dayNumber = cellIndex - startDayOffset + 1

                        if (dayNumber in 1..daysInMonth) {
                            val cellDate = yearMonth.atDay(dayNumber)
                            val isSelected = cellDate == selectedDate
                            val isDateToday = cellDate == today
                            val sessionCount = allSessions.count { it.sessionDate == cellDate.toString() }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isDateToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onDateSelected(cellDate) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$dayNumber",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected || isDateToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isDateToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )

                                    if (sessionCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(4.dp))
                                    }
                                }
                            }
                        } else {
                            // Blank spacer for pre-month and post-month empty cells
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper to launch Google Calendar intent with specific class session details
 */
fun addToGoogleCalendar(
    context: Context,
    title: String,
    description: String,
    location: String,
    dateStr: String,
    startTimeStr: String,
    endTimeStr: String
) {
    try {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.Events.DESCRIPTION, description)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, location)

        try {
            val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            val start = LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm"))
            val end = LocalTime.parse(endTimeStr, DateTimeFormatter.ofPattern("HH:mm"))
            val startMillis = LocalDateTime.of(date, start)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = LocalDateTime.of(date, end)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        } catch (_: Exception) {
            // Ignore time parsing exception and fall back to plain insert
        }

        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo abrir la aplicación de calendario", Toast.LENGTH_SHORT).show()
    }
}
