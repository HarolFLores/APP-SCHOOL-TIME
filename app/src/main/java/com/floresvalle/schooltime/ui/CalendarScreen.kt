package com.floresvalle.schooltime.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floresvalle.schooltime.data.entity.ClassSessionEntity
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.TaskEntity
import com.floresvalle.schooltime.ui.components.ClassDetailEditDialog
import com.floresvalle.schooltime.ui.components.ExamDetailEditDialog
import com.floresvalle.schooltime.ui.components.TaskDetailEditDialog
import com.floresvalle.schooltime.ui.components.UserAvatar
import com.floresvalle.schooltime.viewmodel.SessionViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Helper to launch Google Calendar insert event intent pre-filled with course details.
 */
fun addSessionToGoogleCalendar(context: Context, session: ClassSessionEntity) {
    try {
        val startDateTime = try {
            val parts = session.startTime.split(":")
            val hour = parts[0].trim().toInt()
            val min = parts[1].trim().toInt()
            val d = LocalDate.parse(session.sessionDate)
            d.atTime(hour, min).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        val endDateTime = try {
            val parts = session.endTime.split(":")
            val hour = parts[0].trim().toInt()
            val min = parts[1].trim().toInt()
            val d = LocalDate.parse(session.sessionDate)
            d.atTime(hour, min).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            startDateTime + 2 * 60 * 60 * 1000 // default 2 hours
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Clase: ${session.courseName}")
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Docente: ${session.docente ?: "Por confirmar"}\nModalidad: ${session.modality}"
            )
            putExtra(
                CalendarContract.Events.EVENT_LOCATION,
                session.locationRoom ?: session.virtualUrl ?: "Aula universitaria"
            )
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startDateTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endDateTime)
            putExtra(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo abrir Google Calendar.", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Helper to open the Google Calendar app or web version directly.
 */
fun openGoogleCalendarApp(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://com.android.calendar/time/${System.currentTimeMillis()}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://calendar.google.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Abriendo Google Calendar...", Toast.LENGTH_SHORT).show()
        }
    }
}

data class CalendarDayItemData(
    val year: Int,
    val month: Int, // 0-based Calendar.MONTH
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val calendar: Calendar
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: SessionViewModel,
    onOpenNotifications: () -> Unit = {},
    onOpenQuickProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val todayCal = remember { Calendar.getInstance() }
    val realTodayYear = todayCal.get(Calendar.YEAR)
    val realTodayMonth = todayCal.get(Calendar.MONTH)
    val realTodayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    var currentCalendar by remember { mutableStateOf(todayCal.clone() as Calendar) }
    var selectedDate by remember { mutableIntStateOf(todayCal.get(Calendar.DAY_OF_MONTH)) }
    var isWeekView by remember { mutableStateOf(true) }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val allSessions by viewModel.sessions.collectAsState()
    val allTasks by viewModel.tasks.collectAsState()
    val allExams by viewModel.exams.collectAsState()
    val activePeriod by viewModel.activeAcademicPeriod.collectAsState()
    val liveNotifications by viewModel.liveNotifications.collectAsState()
    val unreadNotifCount = liveNotifications.count { !it.isRead }
    val userData by viewModel.currentUserData.collectAsState()
    val userName = userData?.let { "${it.firstName} ${it.lastName}".trim() }?.ifBlank { null } ?: "Estudiante"

    // Dialog States
    // In CalendarScreen: ONLY courses are editable; tasks and exams are read-only view.
    var selectedSessionForEdit by remember { mutableStateOf<ClassSessionEntity?>(null) }
    var selectedTaskForView by remember { mutableStateOf<TaskEntity?>(null) }
    var selectedExamForView by remember { mutableStateOf<ExamEntity?>(null) }
    var showGoogleCalendarDialog by remember { mutableStateOf(false) }

    // Check Cycle completion
    val nowLocalDate = LocalDate.now()
    val isCycleCompleted = remember(activePeriod) {
        val period = activePeriod
        if (period != null && period.isActive && !period.endDate.isNullOrBlank()) {
            try {
                val endDate = LocalDate.parse(period.endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                nowLocalDate.isAfter(endDate)
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    val currentMonthYearStr = remember(currentCalendar.timeInMillis) {
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        formatter.format(currentCalendar.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    val daysInMonth = remember(currentCalendar.timeInMillis) {
        currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Build continuous list of days for horizontal scrolling:
    // 5 days of previous month + all days of current month + 10 days of next month
    // Total allows seamless transition across month boundaries!
    val continuousDays = remember(currentCalendar.timeInMillis) {
        val list = mutableListOf<CalendarDayItemData>()

        val prevCal = currentCalendar.clone() as Calendar
        prevCal.add(Calendar.MONTH, -1)
        val maxPrevDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prevDaysToShow = 5
        for (d in (maxPrevDays - prevDaysToShow + 1)..maxPrevDays) {
            val c = prevCal.clone() as Calendar
            c.set(Calendar.DAY_OF_MONTH, d)
            list.add(CalendarDayItemData(c.get(Calendar.YEAR), c.get(Calendar.MONTH), d, false, c))
        }

        // Current month
        for (d in 1..daysInMonth) {
            val c = currentCalendar.clone() as Calendar
            c.set(Calendar.DAY_OF_MONTH, d)
            list.add(CalendarDayItemData(c.get(Calendar.YEAR), c.get(Calendar.MONTH), d, true, c))
        }

        // Next month (10 days)
        val nextCal = currentCalendar.clone() as Calendar
        nextCal.add(Calendar.MONTH, 1)
        for (d in 1..10) {
            val c = nextCal.clone() as Calendar
            c.set(Calendar.DAY_OF_MONTH, d)
            list.add(CalendarDayItemData(c.get(Calendar.YEAR), c.get(Calendar.MONTH), d, false, c))
        }

        list
    }

    // Centering the selected date in LazyRow:
    // The previous days count is 5, so day `selectedDate` is at index: 5 + (selectedDate - 1)
    val prevCount = 5
    val currentDayIndex = (prevCount + (selectedDate - 1)).coerceIn(0, (continuousDays.size - 1).coerceAtLeast(0))

    LaunchedEffect(selectedDate, currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.YEAR), isWeekView) {
        if (isWeekView && continuousDays.isNotEmpty()) {
            lazyListState.animateScrollToItem(currentDayIndex, 0)
        }
    }

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
                        Text("Calendario Académico", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Google Calendar integration button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { showGoogleCalendarDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Google Calendar",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Google Calendar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.clickable { onOpenNotifications() }) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "Notificaciones", tint = MaterialTheme.colorScheme.onSurface)
                        if (unreadNotifCount > 0) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
                        }
                    }
                    Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.clickable { onOpenQuickProfile() }) {
                        UserAvatar(
                            photoUri = userData?.photoUri,
                            name = userName,
                            size = 36.dp,
                            fontSize = 15.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(1.5.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF2E7D32)))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Month Header, Month Switchers, and View Toggle (Semana / Mes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(currentMonthYearStr, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = {
                            val newCal = currentCalendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            currentCalendar = newCal
                            selectedDate = minOf(selectedDate, newCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior")
                    }
                    IconButton(
                        onClick = {
                            val newCal = currentCalendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            currentCalendar = newCal
                            selectedDate = minOf(selectedDate, newCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row {
                        Box(
                            modifier = Modifier
                                .clickable { isWeekView = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isWeekView) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        ) {
                            Text(
                                "Semana",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isWeekView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isWeekView) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clickable { isWeekView = false }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (!isWeekView) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        ) {
                            Text(
                                "Mes",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (!isWeekView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (!isWeekView) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            if (isCycleCompleted) {
                Spacer(modifier = Modifier.height(8.dp))
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🎓", style = MaterialTheme.typography.headlineLarge)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("¡Ciclo Concluido con Éxito!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Has completado todas las cátedras de este periodo académico.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Target date string for filtering day sessions, tasks, and exams
            val formattedSelectedDate = remember(currentCalendar.timeInMillis, selectedDate) {
                String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    currentCalendar.get(Calendar.YEAR),
                    currentCalendar.get(Calendar.MONTH) + 1,
                    selectedDate
                )
            }
            val dailySessions = remember(allSessions, formattedSelectedDate) {
                allSessions.filter { it.sessionDate == formattedSelectedDate }
            }
            val dailyTasks = remember(allTasks, formattedSelectedDate) {
                allTasks.filter { it.dueDate == formattedSelectedDate }
            }
            val dailyExams = remember(allExams, formattedSelectedDate) {
                allExams.filter { it.examDate == formattedSelectedDate }
            }
            val totalDayEvents = dailySessions.size + dailyTasks.size + dailyExams.size

            val summaryCal = remember(currentCalendar.timeInMillis, selectedDate) {
                val c = currentCalendar.clone() as Calendar
                c.set(Calendar.DAY_OF_MONTH, selectedDate)
                c
            }
            val summaryDateStr = remember(summaryCal.timeInMillis) {
                val summaryFormatter = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault())
                summaryFormatter.format(summaryCal.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }

            if (isWeekView) {
                // WEEK / HORIZONTAL STRIP VIEW
                // Centering math: BoxWithConstraints gives exact available width
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val screenWidth = maxWidth
                    val itemWidth = 62.dp
                    val centerPadding = maxOf(16.dp, (screenWidth - itemWidth) / 2)

                    LazyRow(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentPadding = PaddingValues(horizontal = centerPadding),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(continuousDays.size, key = { idx ->
                            val item = continuousDays[idx]
                            "${item.year}-${item.month}-${item.dayOfMonth}-${item.isCurrentMonth}"
                        }) { idx ->
                            val item = continuousDays[idx]
                            val isRealToday = (item.dayOfMonth == realTodayDay &&
                                    item.month == realTodayMonth &&
                                    item.year == realTodayYear)

                            val isSelected = (item.isCurrentMonth && item.dayOfMonth == selectedDate)

                            val dateStr = String.format(
                                Locale.getDefault(),
                                "%04d-%02d-%02d",
                                item.year,
                                item.month + 1,
                                item.dayOfMonth
                            )
                            val sessionsCount = allSessions.count { it.sessionDate == dateStr }
                            val tasksCount = allTasks.count { it.dueDate == dateStr }
                            val examsCount = allExams.count { it.examDate == dateStr }
                            val totalEvents = sessionsCount + tasksCount + examsCount

                            DayItem(
                                date = item.calendar,
                                isSelected = isSelected,
                                isToday = isRealToday,
                                isOtherMonth = !item.isCurrentMonth,
                                dotsCount = minOf(totalEvents, 3),
                                onDaySelected = {
                                    if (item.isCurrentMonth) {
                                        selectedDate = item.dayOfMonth
                                    } else {
                                        // User tapped a day in adjacent month: smoothly update month!
                                        val newCal = currentCalendar.clone() as Calendar
                                        newCal.set(Calendar.YEAR, item.year)
                                        newCal.set(Calendar.MONTH, item.month)
                                        newCal.set(Calendar.DAY_OF_MONTH, item.dayOfMonth)
                                        currentCalendar = newCal
                                        selectedDate = item.dayOfMonth
                                    }
                                    coroutineScope.launch {
                                        lazyListState.animateScrollToItem(idx, 0)
                                    }
                                }
                            )
                        }
                    }
                }

                // Summary bar
                DaySummaryBar(
                    summaryDateStr = summaryDateStr,
                    sessionsCount = dailySessions.size,
                    tasksCount = dailyTasks.size,
                    examsCount = dailyExams.size,
                    onSyncDayToCalendar = {
                        if (dailySessions.isNotEmpty()) {
                            dailySessions.forEach { session ->
                                addSessionToGoogleCalendar(context, session)
                            }
                        } else {
                            Toast.makeText(context, "No hay clases programadas para este día.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Daily Activities List
                DailyEventsList(
                    dailySessions = dailySessions,
                    dailyTasks = dailyTasks,
                    dailyExams = dailyExams,
                    allCycleSessions = allSessions,
                    totalDayEvents = totalDayEvents,
                    onEditSession = { selectedSessionForEdit = it },
                    onViewTask = { selectedTaskForView = it },
                    onViewExam = { selectedExamForView = it },
                    onAddSessionToCalendar = { addSessionToGoogleCalendar(context, it) }
                )
            } else {
                // FULL MONTH GRID VIEW (No blank space!)
                val yearMonth = YearMonth.of(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH) + 1)
                val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7 // 0 = Domingo, 1 = Lunes, etc.

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Weekdays Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb").forEach { day ->
                            Text(
                                day,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Month Grid (compact and proportional)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(firstDayOfWeek) {
                            Box(modifier = Modifier.aspectRatio(1.1f))
                        }

                        items(daysInMonth) { dayIdx ->
                            val dayNum = dayIdx + 1
                            val isRealTodayInMonth = (dayNum == realTodayDay &&
                                    currentCalendar.get(Calendar.MONTH) == realTodayMonth &&
                                    currentCalendar.get(Calendar.YEAR) == realTodayYear)

                            val dateStr = String.format(
                                Locale.getDefault(),
                                "%04d-%02d-%02d",
                                currentCalendar.get(Calendar.YEAR),
                                currentCalendar.get(Calendar.MONTH) + 1,
                                dayNum
                            )

                            val sessionsCount = allSessions.count { it.sessionDate == dateStr }
                            val tasksCount = allTasks.count { it.dueDate == dateStr }
                            val examsCount = allExams.count { it.examDate == dateStr }
                            val totalEvents = sessionsCount + tasksCount + examsCount
                            val isSelected = (dayNum == selectedDate)

                            val boxBgColor = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isRealTodayInMonth -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1.1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(boxBgColor)
                                    .border(
                                        if (isRealTodayInMonth && !isSelected) 1.5.dp else 0.dp,
                                        if (isRealTodayInMonth) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedDate = dayNum
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (isRealTodayInMonth) {
                                        Text(
                                            "HOY",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (isRealTodayInMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (totalEvents > 0) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            repeat(minOf(totalEvents, 3)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(3.5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dedicated Bottom Schedule Panel for Month View (Eliminates white space entirely!)
                    DaySummaryBar(
                        summaryDateStr = summaryDateStr,
                        sessionsCount = dailySessions.size,
                        tasksCount = dailyTasks.size,
                        examsCount = dailyExams.size,
                        onSyncDayToCalendar = {
                            if (dailySessions.isNotEmpty()) {
                                dailySessions.forEach { session ->
                                    addSessionToGoogleCalendar(context, session)
                                }
                            } else {
                                Toast.makeText(context, "No hay clases en esta fecha.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DailyEventsList(
                        dailySessions = dailySessions,
                        dailyTasks = dailyTasks,
                        dailyExams = dailyExams,
                        allCycleSessions = allSessions,
                        totalDayEvents = totalDayEvents,
                        onEditSession = { selectedSessionForEdit = it },
                        onViewTask = { selectedTaskForView = it },
                        onViewExam = { selectedExamForView = it },
                        onAddSessionToCalendar = { addSessionToGoogleCalendar(context, it) }
                    )
                }
            }
        }
    }

    // Google Calendar Integration Dialog
    if (showGoogleCalendarDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleCalendarDialog = false },
            icon = {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Vincular Google Calendar", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Puedes abrir tu Google Calendar para consultar tu agenda personal o agendar directamente cada una de tus clases.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(
                                "En cada tarjeta de curso encontrarás el botón 'Agendar en Google Calendar'.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGoogleCalendarDialog = false
                        openGoogleCalendarApp(context)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Abrir Google Calendar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleCalendarDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // EDIT / VIEW DIALOGS:
    // CalendarScreen rule: ONLY courses are editable (canEdit = true).
    // Tasks and exams are view-only here (canEdit = false).
    selectedSessionForEdit?.let { session ->
        ClassDetailEditDialog(
            session = session,
            canEdit = true, // EDITABLE IN CALENDAR!
            onDismiss = { selectedSessionForEdit = null },
            onDelete = {
                viewModel.deleteSession(session.id, deleteAllRecurring = true)
                selectedSessionForEdit = null
            },
            onSave = { newCourseName, docente, startTime, endTime, sessionDate ->
                viewModel.updateSessionDetails(session.id, newCourseName, docente, startTime, endTime, sessionDate)
                selectedSessionForEdit = null
            }
        )
    }

    selectedTaskForView?.let { task ->
        TaskDetailEditDialog(
            task = task,
            canEdit = false, // SOLO VISUALIZACIÓN EN CALENDARIO
            onDismiss = { selectedTaskForView = null },
            onDelete = null,
            onSave = { _, _, _ -> }
        )
    }

    selectedExamForView?.let { exam ->
        ExamDetailEditDialog(
            exam = exam,
            canEdit = false, // SOLO VISUALIZACIÓN EN CALENDARIO
            onDismiss = { selectedExamForView = null },
            onDelete = null,
            onSave = { _, _, _ -> }
        )
    }
}

/**
 * Uniform Day Item:
 * Every single day card has the EXACT same width (62.dp) and height (88.dp).
 * The top chip slot is fixed at 18.dp to prevent any height variations between today and other days.
 */
@Composable
fun DayItem(
    date: Calendar,
    isSelected: Boolean,
    isToday: Boolean,
    isOtherMonth: Boolean = false,
    dotsCount: Int,
    onDaySelected: () -> Unit
) {
    val dayName = remember(date.timeInMillis) {
        val s = date.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: ""
        s.take(3).uppercase()
    }
    val dayOfMonth = date.get(Calendar.DAY_OF_MONTH)

    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        isOtherMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        isOtherMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Column(
        modifier = Modifier
            .width(62.dp)
            .height(88.dp) // EXACT SAME FIXED SIZE FOR ALL DAY CARDS!
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(if (isToday && !isSelected) 1.5.dp else 0.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onDaySelected() }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Fixed top badge slot (18.dp) so every card is identical in height
        Box(
            modifier = Modifier
                .height(18.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isToday) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "HOY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            } else if (isOtherMonth) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        // Day of week text
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color.White.copy(alpha = 0.85f) else if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )

        // Day of month number
        Text(
            text = dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )

        // Fixed bottom dots slot (8.dp)
        Box(
            modifier = Modifier
                .height(8.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (dotsCount > 0) {
                val dotColor = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(dotsCount) {
                        Box(modifier = Modifier.size(3.5.dp).clip(CircleShape).background(dotColor))
                    }
                }
            }
        }
    }
}

@Composable
fun DaySummaryBar(
    summaryDateStr: String,
    sessionsCount: Int,
    tasksCount: Int,
    examsCount: Int,
    onSyncDayToCalendar: () -> Unit
) {
    val summarySubtitle = buildString {
        val parts = mutableListOf<String>()
        if (sessionsCount > 0) parts.add("$sessionsCount ${if (sessionsCount == 1) "cátedra" else "cátedras"}")
        if (tasksCount > 0) parts.add("$tasksCount ${if (tasksCount == 1) "tarea" else "tareas"}")
        if (examsCount > 0) parts.add("$examsCount ${if (examsCount == 1) "examen" else "exámenes"}")
        if (parts.isEmpty()) append("Sin cátedras agendadas") else append(parts.joinToString(" • "))
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(summaryDateStr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(summarySubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (sessionsCount > 0) {
                IconButton(
                    onClick = onSyncDayToCalendar,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = "Agendar día",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DailyEventsList(
    dailySessions: List<ClassSessionEntity>,
    dailyTasks: List<TaskEntity>,
    dailyExams: List<ExamEntity>,
    allCycleSessions: List<ClassSessionEntity>,
    totalDayEvents: Int,
    onEditSession: (ClassSessionEntity) -> Unit,
    onViewTask: (TaskEntity) -> Unit,
    onViewExam: (ExamEntity) -> Unit,
    onAddSessionToCalendar: (ClassSessionEntity) -> Unit
) {
    val context = LocalContext.current

    if (totalDayEvents == 0) {
        // Zero blank space! When there are no events for the selected day, show a warm card AND
        // the complete list of cycle courses so the user can always see and manage their subjects!
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("🎉", fontSize = 28.sp)
                        Column {
                            Text("Día Libre de Cátedra", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "No tienes sesiones programadas para esta fecha. ¡Aprovecha para repasar tus cursos!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(
                        "Cursos del Ciclo Académico",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    "Toca cualquier curso para editar su horario o agendarlo en Google Calendar:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val uniqueCourses = allCycleSessions.distinctBy { it.courseName }
            items(uniqueCourses, key = { it.id }) { session ->
                SessionCardItem(
                    session = session,
                    onEditClick = { onEditSession(session) },
                    onAddToCalendar = { onAddSessionToCalendar(session) }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Courses / Sessions (EDITABLE in Calendar!)
            items(dailySessions, key = { it.id }) { session ->
                SessionCardItem(
                    session = session,
                    onEditClick = { onEditSession(session) },
                    onAddToCalendar = { onAddSessionToCalendar(session) }
                )
            }

            // Tasks (View only in Calendar)
            items(dailyTasks, key = { it.id }) { task ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewTask(task) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.courseName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Entrega: ${task.dueTime.ifBlank { "23:59" }} • ${task.urgency} (Solo ver)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Exams (View only in Calendar)
            items(dailyExams, key = { it.id }) { exam ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewExam(exam) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(exam.courseName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                            Text(exam.type, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Horario: ${exam.examTime} • ${exam.modality} (Solo ver)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/**
 * Complete, unclipped Course Card displaying full information:
 * Course Title, Modality chip, Docente, Schedule, Location/Link,
 * "Editar Horario" button (EDITABLE in calendar!), and "Agendar en Google Calendar" button!
 */
@Composable
fun SessionCardItem(
    session: ClassSessionEntity,
    onEditClick: () -> Unit,
    onAddToCalendar: () -> Unit
) {
    val isVirtual = session.modality.equals("Virtual", ignoreCase = true)
    val context = LocalContext.current

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Header Row: Course Title & Modality
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isVirtual) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = session.modality,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isVirtual) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details Row: Schedule & Location / Modality
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${session.startTime} - ${session.endTime}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }

                if (isVirtual) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LaptopMac, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clase Virtual", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(session.locationRoom ?: "Aula asignada", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Docente info
            if (!session.docente.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(session.docente, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: "Editar Horario" and "Google Calendar"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Editar Horario", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                FilledTonalButton(
                    onClick = onAddToCalendar,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Google Calendar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
