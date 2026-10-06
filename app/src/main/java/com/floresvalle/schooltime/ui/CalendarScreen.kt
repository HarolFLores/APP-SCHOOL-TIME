package com.floresvalle.schooltime.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floresvalle.schooltime.data.entity.ClassSessionEntity
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.TaskEntity
import com.floresvalle.schooltime.ui.components.ClassDetailEditDialog
import com.floresvalle.schooltime.ui.components.ExamDetailEditDialog
import com.floresvalle.schooltime.ui.components.TaskDetailEditDialog
import com.floresvalle.schooltime.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

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

    // Auto-scroll to today's date on initial load
    LaunchedEffect(Unit) {
        val initialIndex = (todayCal.get(Calendar.DAY_OF_MONTH) - 1).coerceAtLeast(0)
        lazyListState.scrollToItem(initialIndex)
    }

    val allSessions by viewModel.sessions.collectAsState()
    val allTasks by viewModel.tasks.collectAsState()
    val allExams by viewModel.exams.collectAsState()
    val activePeriod by viewModel.activeAcademicPeriod.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val liveNotifications by viewModel.liveNotifications.collectAsState()
    val unreadNotifCount = liveNotifications.count { !it.isRead }

    // Dialog & Sheet States
    var selectedSessionForEdit by remember { mutableStateOf<ClassSessionEntity?>(null) }
    var selectedTaskForEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var selectedExamForEdit by remember { mutableStateOf<ExamEntity?>(null) }
    var selectedMonthDayEvents by remember { mutableStateOf<Triple<String, List<ClassSessionEntity>, List<Any>>?>(null) }

    // Real Academic Period Check (Only completes when now > activePeriod.endDate)
    val nowLocalDate = LocalDate.now()
    val isCycleCompleted = remember(activePeriod) {
        val periodEnd = activePeriod?.endDate
        if (!periodEnd.isNullOrBlank()) {
            try {
                val endDate = LocalDate.parse(periodEnd, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
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
                        Text("Calendario", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        ) {
            // Month Header & Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                            Text("Semana", style = MaterialTheme.typography.labelMedium, color = if (isWeekView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isWeekView) FontWeight.Bold else FontWeight.Normal)
                        }
                        Box(
                            modifier = Modifier
                                .clickable { isWeekView = false }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (!isWeekView) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        ) {
                            Text("Mes", style = MaterialTheme.typography.labelMedium, color = if (!isWeekView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (!isWeekView) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            // Sync Status & Refresh Button
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                    Text("Room & Firebase Cloud Sync • Activo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Row(
                    modifier = Modifier.clickable { viewModel.refreshData() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(if (isRefreshing) "Sincronizando..." else "Actualizar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            if (isCycleCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🎓", style = MaterialTheme.typography.headlineLarge)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("¡Felicidades!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("¡Has concluido el ciclo académico con éxito!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            if (isWeekView) {
                LazyRow(
                    state = lazyListState,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Previous month boundary transition day
                    val prevCal = currentCalendar.clone() as Calendar
                    prevCal.add(Calendar.MONTH, -1)
                    val daysInPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                    item {
                        val edgeCal = prevCal.clone() as Calendar
                        edgeCal.set(Calendar.DAY_OF_MONTH, daysInPrevMonth)
                        val isEdgeToday = (daysInPrevMonth == realTodayDay &&
                                          prevCal.get(Calendar.MONTH) == realTodayMonth &&
                                          prevCal.get(Calendar.YEAR) == realTodayYear)
                        DayItem(
                            date = edgeCal,
                            isSelected = false,
                            isToday = isEdgeToday,
                            dotsCount = 0,
                            onDaySelected = {
                                currentCalendar = prevCal
                                selectedDate = daysInPrevMonth
                            }
                        )
                    }

                    // Current month days
                    items(daysInMonth) { dayIdx ->
                        val dayNum = dayIdx + 1
                        val calItem = currentCalendar.clone() as Calendar
                        calItem.set(Calendar.DAY_OF_MONTH, dayNum)

                        val isRealToday = (dayNum == realTodayDay &&
                                           currentCalendar.get(Calendar.MONTH) == realTodayMonth &&
                                           currentCalendar.get(Calendar.YEAR) == realTodayYear)

                        val dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                            calItem.get(Calendar.YEAR),
                            calItem.get(Calendar.MONTH) + 1,
                            dayNum
                        )
                        val sessionsCount = allSessions.count { it.sessionDate == dateStr }
                        val tasksCount = allTasks.count { it.dueDate == dateStr }
                        val examsCount = allExams.count { it.examDate == dateStr }
                        val totalEvents = sessionsCount + tasksCount + examsCount

                        DayItem(
                            date = calItem,
                            isSelected = dayNum == selectedDate,
                            isToday = isRealToday,
                            dotsCount = minOf(totalEvents, 3),
                            onDaySelected = { selectedDay ->
                                selectedDate = selectedDay
                            }
                        )
                    }

                    // Next month boundary transition day
                    val nextCal = currentCalendar.clone() as Calendar
                    nextCal.add(Calendar.MONTH, 1)

                    item {
                        val edgeCal = nextCal.clone() as Calendar
                        edgeCal.set(Calendar.DAY_OF_MONTH, 1)
                        val isEdgeToday = (1 == realTodayDay &&
                                          nextCal.get(Calendar.MONTH) == realTodayMonth &&
                                          nextCal.get(Calendar.YEAR) == realTodayYear)
                        DayItem(
                            date = edgeCal,
                            isSelected = false,
                            isToday = isEdgeToday,
                            dotsCount = 0,
                            onDaySelected = {
                                currentCalendar = nextCal
                                selectedDate = 1
                            }
                        )
                    }
                }

                val formattedSelectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    currentCalendar.get(Calendar.YEAR),
                    currentCalendar.get(Calendar.MONTH) + 1,
                    selectedDate
                )
                val dailySessions = allSessions.filter { it.sessionDate == formattedSelectedDate }
                val dailyTasks = allTasks.filter { it.dueDate == formattedSelectedDate }
                val dailyExams = allExams.filter { it.examDate == formattedSelectedDate }
                val totalDayEvents = dailySessions.size + dailyTasks.size + dailyExams.size

                val summaryCal = currentCalendar.clone() as Calendar
                summaryCal.set(Calendar.DAY_OF_MONTH, selectedDate)
                val summaryFormatter = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault())
                val summaryDateStr = summaryFormatter.format(summaryCal.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                val summarySubtitle = buildString {
                    val parts = mutableListOf<String>()
                    if (dailySessions.isNotEmpty()) parts.add("${dailySessions.size} ${if (dailySessions.size == 1) "cátedra" else "cátedras"}")
                    if (dailyTasks.isNotEmpty()) parts.add("${dailyTasks.size} ${if (dailyTasks.size == 1) "tarea" else "tareas"}")
                    if (dailyExams.isNotEmpty()) parts.add("${dailyExams.size} ${if (dailyExams.size == 1) "examen" else "exámenes"}")
                    if (parts.isEmpty()) append("Sin actividades programadas") else append(parts.joinToString(" • "))
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.elevatedCardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(summaryDateStr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(summarySubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (totalDayEvents == 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🎉", style = MaterialTheme.typography.displayMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Sin pendientes este día", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Tómate un respiro, no hay clases ni evaluaciones agendadas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(dailySessions, key = { it.id }) { session ->
                            SessionCardItem(
                                session = session,
                                onClick = { selectedSessionForEdit = session },
                                onOpenUrl = { url ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            )
                        }

                        items(dailyTasks, key = { it.id }) { task ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth().clickable { selectedTaskForEdit = task },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
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
                                        Text("Entrega límite: ${task.dueTime.ifBlank { "23:59" }} • ${task.urgency}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        items(dailyExams, key = { it.id }) { exam ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth().clickable { selectedExamForEdit = exam },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
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
                                        Icon(Icons.Default.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(exam.courseName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                                        Text(exam.type, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("Horario: ${exam.examTime} • ${exam.modality}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                val yearMonth = YearMonth.of(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH) + 1)
                val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7

                Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
                        listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb").forEach { day ->
                            Text(day, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(firstDayOfWeek) {
                            Box(modifier = Modifier.size(40.dp))
                        }

                        items(daysInMonth) { dayIdx ->
                            val dayNum = dayIdx + 1
                            val isRealTodayInMonth = (dayNum == realTodayDay &&
                                                      currentCalendar.get(Calendar.MONTH) == realTodayMonth &&
                                                      currentCalendar.get(Calendar.YEAR) == realTodayYear)

                            val dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                                currentCalendar.get(Calendar.YEAR),
                                currentCalendar.get(Calendar.MONTH) + 1,
                                dayNum
                            )

                            val daySessions = allSessions.filter { it.sessionDate == dateStr }
                            val dayTasks = allTasks.filter { it.dueDate == dateStr }
                            val dayExams = allExams.filter { it.examDate == dateStr }
                            val totalEvents = daySessions.size + dayTasks.size + dayExams.size

                            val isSelected = dayNum == selectedDate

                            val boxBgColor = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isRealTodayInMonth -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(boxBgColor)
                                    .border(if (isRealTodayInMonth && !isSelected) 1.5.dp else 0.dp, if (isRealTodayInMonth) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedDate = dayNum
                                        if (totalEvents > 0) {
                                            val allEvents = mutableListOf<Any>()
                                            allEvents.addAll(dayTasks)
                                            allEvents.addAll(dayExams)
                                            selectedMonthDayEvents = Triple(dateStr, daySessions, allEvents)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (isRealTodayInMonth) {
                                        Text("HOY", style = MaterialTheme.typography.labelSmall, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                    }
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (isRealTodayInMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (totalEvents > 0) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            repeat(minOf(totalEvents, 3)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
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
                }
            }
        }
    }

    // Modal Sheet for Day Events in Month View
    selectedMonthDayEvents?.let { (dateStr, sessionsList, otherEvents) ->
        ModalBottomSheet(
            onDismissRequest = { selectedMonthDayEvents = null },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Eventos del $dateStr", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(max = 350.dp)) {
                    items(sessionsList) { session ->
                        SessionCardItem(session = session, onClick = {
                            selectedMonthDayEvents = null
                            selectedSessionForEdit = session
                        })
                    }
                    items(otherEvents) { event ->
                        if (event is TaskEntity) {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedMonthDayEvents = null
                                    selectedTaskForEdit = event
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(event.title, fontWeight = FontWeight.Bold)
                                        Text("${event.courseName} • Tarea", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        } else if (event is ExamEntity) {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedMonthDayEvents = null
                                    selectedExamForEdit = event
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(event.type, fontWeight = FontWeight.Bold)
                                        Text("${event.courseName} • Examen", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail / Edit Dialogs
    selectedSessionForEdit?.let { session ->
        ClassDetailEditDialog(
            session = session,
            canEdit = true, // Permite editar el curso en Calendario
            onDismiss = { selectedSessionForEdit = null },
            onSave = { docente, startTime, endTime, sessionDate ->
                viewModel.updateSessionDetails(session.id, docente, startTime, endTime, sessionDate)
            }
        )
    }

    selectedTaskForEdit?.let { task ->
        TaskDetailEditDialog(
            task = task,
            canEdit = false, // Solo ver en calendario
            onDismiss = { selectedTaskForEdit = null },
            onSave = { dueDate, dueTime ->
                viewModel.updateTaskDateTime(task.id, dueDate, dueTime)
            }
        )
    }

    selectedExamForEdit?.let { exam ->
        ExamDetailEditDialog(
            exam = exam,
            canEdit = false, // Solo ver en calendario
            onDismiss = { selectedExamForEdit = null },
            onSave = { examDate, examTime ->
                viewModel.updateExamDateTime(exam.id, examDate, examTime)
            }
        )
    }
}

@Composable
fun DayItem(
    date: Calendar,
    isSelected: Boolean,
    isToday: Boolean,
    dotsCount: Int,
    onDaySelected: (Int) -> Unit
) {
    val dayName = date.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: ""
    val dayOfMonth = date.get(Calendar.DAY_OF_MONTH)

    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Column(
        modifier = Modifier
            .width(58.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .border(if (isToday && !isSelected) 1.5.dp else 0.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable { onDaySelected(dayOfMonth) }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }

        Text(
            text = dayName.take(3),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color.White.copy(alpha = 0.8f) else if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (dotsCount > 0) {
                val dotColor = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                repeat(dotsCount) {
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(dotColor))
                }
            } else {
                Box(modifier = Modifier.size(4.dp).background(Color.Transparent))
            }
        }
    }
}

@Composable
fun SessionCardItem(
    session: ClassSessionEntity,
    onClick: () -> Unit,
    onOpenUrl: (String) -> Unit = {}
) {
    val isVirtual = session.modality.equals("Virtual", ignoreCase = true)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(session.courseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text("${session.startTime} - ${session.endTime}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isVirtual) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LaptopMac, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clase Virtual / En línea", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(session.locationRoom ?: "Aula por asignar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
