package com.floresvalle.schooltime.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floresvalle.schooltime.util.AuthPreferences
import kotlinx.coroutines.launch

data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val badge: String,
    val illustrationType: IllustrationType
)

enum class IllustrationType {
    STUDENT_JOURNEY,
    SCHEDULE_CALENDAR,
    ACADEMIC_PERFORMANCE
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val steps = remember {
        listOf(
            OnboardingStep(
                title = "¡Encantados de Acompañar Tu Camino!",
                subtitle = "Organiza tus cursos, horarios y evaluaciones universitarias en una sola aplicación moderna y potente.",
                badge = "BIENVENIDO A SCHOOLTIME",
                illustrationType = IllustrationType.STUDENT_JOURNEY
            ),
            OnboardingStep(
                title = "Horarios y Calendario sin Conflictos",
                subtitle = "Visualiza tus clases semanales por colores, aulas, docentes y fechas de ciclo con sincronización automática.",
                badge = "GESTIÓN DE HORARIOS",
                illustrationType = IllustrationType.SCHEDULE_CALENDAR
            ),
            OnboardingStep(
                title = "Domina Tus Tareas y Promedio Real",
                subtitle = "Calcula tu promedio ponderado de ciclo, registra entregas con recordatorios y lleva el control total de tus notas.",
                badge = "RENDIMIENTO ACADÉMICO",
                illustrationType = IllustrationType.ACADEMIC_PERFORMANCE
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { steps.size })

    fun completeOnboarding() {
        AuthPreferences.setOnboardingCompleted(context, true)
        onFinish()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val step = steps[pageIndex]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 220.dp), // Leaves space for the bottom floating sheet
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (step.illustrationType) {
                            IllustrationType.STUDENT_JOURNEY -> StudentJourneyIllustration()
                            IllustrationType.SCHEDULE_CALENDAR -> ScheduleCalendarIllustration()
                            IllustrationType.ACADEMIC_PERFORMANCE -> AcademicPerformanceIllustration()
                        }
                    }
                }
            }

            // Top Bar with App Badge & Skip ("Omitir") button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "SchoolTime 2026",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (pagerState.currentPage < steps.size - 1) {
                    TextButton(
                        onClick = { completeOnboarding() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text(
                            text = "Omitir",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Bottom Floating Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                        ambientColor = Color.Black.copy(alpha = 0.15f),
                        spotColor = Color.Black.copy(alpha = 0.25f)
                    ),
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                val currentStep = steps[pagerState.currentPage]
                val isLastPage = pagerState.currentPage == steps.size - 1

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Category Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = currentStep.badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title
                    Text(
                        text = currentStep.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 23.sp,
                            lineHeight = 30.sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Subtitle / Description
                    Text(
                        text = currentStep.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Page Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(steps.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(if (isSelected) 24.dp else 6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Navigation Controls Row:
                    // If on page > 0, shows a back button "<" on the left
                    // Main action button: "Continuar" with ">" on steps 0..N-2,
                    // and on final step, "Comenzar ahora" in full app primary brand palette
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Back "<" Button (only shown when page > 0)
                        if (pagerState.currentPage > 0) {
                            FilledTonalIconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("onboarding_back_button"),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Regresar",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Primary Action Button
                        if (isLastPage) {
                            // Final Step: "Comenzar ahora" in app's vibrant primary palette
                            Button(
                                onClick = { completeOnboarding() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("onboarding_start_button"),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 8.dp
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RocketLaunch,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Comenzar ahora",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Steps 0 and 1: Clean button with "Continuar" and right arrow ">"
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("onboarding_continue_button"),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 6.dp
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Spacer(modifier = Modifier.width(24.dp))
                                    Text(
                                        text = "Continuar",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Siguiente",
                                        modifier = Modifier.size(26.dp)
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

/**
 * Illustration 1: Student with study books and vibrant geometric background arcs
 * directly matching the design & color scheme of the user reference image.
 */
@Composable
fun StudentJourneyIllustration() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val coralColor = Color(0xFFFF8A65)
    val mintColor = Color(0xFF4DB6AC)
    val yellowColor = Color(0xFFFFD54F)
    val skyColor = Color(0xFF4FC3F7)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.55f)
            val baseRadius = size.minDimension * 0.42f

            // Background concentric colored arcs (like the user reference image)
            drawArc(
                color = mintColor.copy(alpha = 0.85f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - baseRadius * 1.05f, center.y - baseRadius * 1.05f),
                size = Size(baseRadius * 2.1f, baseRadius * 2.1f),
                style = Stroke(width = 32f, cap = StrokeCap.Round)
            )

            drawArc(
                color = coralColor.copy(alpha = 0.85f),
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(center.x - baseRadius * 0.82f, center.y - baseRadius * 0.82f),
                size = Size(baseRadius * 1.64f, baseRadius * 1.64f),
                style = Stroke(width = 26f, cap = StrokeCap.Round)
            )

            drawArc(
                color = skyColor.copy(alpha = 0.75f),
                startAngle = 210f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(center.x - baseRadius * 0.6f, center.y - baseRadius * 0.6f),
                size = Size(baseRadius * 1.2f, baseRadius * 1.2f),
                style = Stroke(width = 20f, cap = StrokeCap.Round)
            )

            // Central circle halo
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = baseRadius * 0.55f,
                center = center
            )
        }

        // Student Character Layout with Backpack & Book
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Student Avatar Face
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFCC80), Color(0xFFFFB74D))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Smile face & Hair
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Hair
                    drawArc(
                        color = Color(0xFF3E2723),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(0f, 0f),
                        size = Size(w, h * 0.65f)
                    )

                    // Friendly eyes
                    drawCircle(color = Color(0xFF212121), radius = 4f, center = Offset(w * 0.36f, h * 0.45f))
                    drawCircle(color = Color(0xFF212121), radius = 4f, center = Offset(w * 0.64f, h * 0.45f))

                    // Smile
                    drawArc(
                        color = Color(0xFFD84315),
                        startAngle = 20f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(w * 0.32f, h * 0.52f),
                        size = Size(w * 0.36f, h * 0.25f),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Student Jacket & Backpack
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Left backpack strap
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(yellowColor)
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Green Jacket / Body holding golden book (just like the reference!)
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 90.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(Color(0xFF2E7D32)), // Dark college green jacket
                    contentAlignment = Alignment.Center
                ) {
                    // Textbook in hands (Yellow / orange notebook)
                    Surface(
                        modifier = Modifier
                            .size(width = 64.dp, height = 74.dp)
                            .shadow(6.dp, RoundedCornerShape(6.dp)),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFB300)
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(4.dp)
                                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(3.dp)
                                    .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text("★", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Right backpack strap
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(yellowColor)
                )
            }
        }
    }
}

/**
 * Illustration 2: Modern Schedule & Calendar Matrix
 */
@Composable
fun ScheduleCalendarIllustration() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tealColor = Color(0xFF00897B)
    val purpleColor = Color(0xFF7E57C2)
    val orangeColor = Color(0xFFFB8C00)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background geometric cards
        Box(
            modifier = Modifier
                .size(230.dp, 190.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        )

        // Main Weekly Calendar Board Card
        Surface(
            modifier = Modifier
                .size(250.dp, 210.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Days of the week pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("LUN", "MAR", "MIÉ", "JUE", "VIE").forEachIndexed { i, day ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (i == 1) primaryColor else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(38.dp, 24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = day,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (i == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Class Session Cards Stack
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Class 1
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = tealColor.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(tealColor))
                                Text("Cálculo Avanzado", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = tealColor)
                            }
                            Text("08:00 - 10:00", fontSize = 10.sp, color = tealColor, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Class 2
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = purpleColor.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(purpleColor))
                                Text("Base de Datos I", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = purpleColor)
                            }
                            Text("10:30 - 12:45", fontSize = 10.sp, color = purpleColor, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Class 3
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = orangeColor.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(orangeColor))
                                Text("Algoritmos II", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = orangeColor)
                            }
                            Text("14:00 - 16:15", fontSize = 10.sp, color = orangeColor, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Illustration 3: Academic Performance, GPA Trophy & Grades Tracker
 */
@Composable
fun AcademicPerformanceIllustration() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val greenColor = Color(0xFF2E7D32)
    val goldColor = Color(0xFFFFB300)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background glow
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Trophy and Score Badge
            Surface(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(12.dp, CircleShape),
                shape = CircleShape,
                color = goldColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🏆",
                            fontSize = 32.sp
                        )
                        Text(
                            text = "18.5",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF3E2723)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics Preview Card
            Surface(
                modifier = Modifier
                    .width(260.dp)
                    .shadow(10.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Promedio", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Sobresaliente", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = greenColor)
                    }
                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Créditos", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("22 Aprobados", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = primaryColor)
                    }
                }
            }
        }
    }
}
