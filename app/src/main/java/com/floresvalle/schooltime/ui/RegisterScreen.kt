package com.floresvalle.schooltime.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.floresvalle.schooltime.ui.components.UserAvatar
import com.floresvalle.schooltime.util.AuthValidation
import com.floresvalle.schooltime.viewmodel.RegisterViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    googleEmail: String? = null,
    googleName: String? = null,
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    // Current Step in the 3-step registration wizard
    var currentStep by remember { mutableIntStateOf(1) }

    BackHandler {
        if (currentStep > 1) {
            currentStep--
        } else {
            viewModel.cancelRegistration()
            onNavigateBack()
        }
    }

    val currentGoogleUser = try { Firebase.auth.currentUser } catch (_: Exception) { null }
    val isGoogleSignUp = viewModel.isGoogleFlow || 
        !googleEmail.isNullOrBlank() || 
        (currentGoogleUser?.providerData?.any { it.providerId == "google.com" } == true)

    val rawGoogleEmail = viewModel.googleFlowEmail.ifBlank { googleEmail ?: currentGoogleUser?.email ?: "" }
    val decodedGoogleEmail = try { android.net.Uri.decode(rawGoogleEmail) } catch (_: Exception) { rawGoogleEmail }

    val rawGoogleName = viewModel.googleFlowName.ifBlank { googleName ?: currentGoogleUser?.displayName ?: "" }
    val decodedGoogleName = try { android.net.Uri.decode(rawGoogleName) } catch (_: Exception) { rawGoogleName }

    val googleNames = decodedGoogleName.trim().split(" ").filter { it.isNotBlank() }
    val initialFirstName = if (isGoogleSignUp) googleNames.firstOrNull() ?: "Estudiante" else ""
    val initialLastName = if (isGoogleSignUp) (if (googleNames.size > 1) googleNames.drop(1).joinToString(" ") else "") else ""

    var firstName by remember(decodedGoogleName) { mutableStateOf(initialFirstName) }
    var lastName by remember(decodedGoogleName) { mutableStateOf(initialLastName) }
    var email by remember(decodedGoogleEmail) { mutableStateOf(if (isGoogleSignUp) decodedGoogleEmail else "") }
    var phone by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Country Phone Code Selector (Elite Apps feature)
    var selectedCountry by remember { mutableStateOf(AuthValidation.SUPPORTED_COUNTRIES.first()) }
    var showCountryDialog by remember { mutableStateOf(false) }

    // Predefined Career Select + "Otra"
    var selectedCareer by remember { mutableStateOf(AuthValidation.PREDEFINED_CAREERS.first()) }
    var customCareer by remember { mutableStateOf("") }
    var careerExpanded by remember { mutableStateOf(false) }

    fun effectiveCareer(): String {
        return if (selectedCareer.startsWith("Otra", ignoreCase = true)) {
            customCareer.trim()
        } else {
            selectedCareer.trim()
        }
    }

    fun effectivePhone(): String {
        val trimmed = phone.trim()
        return if (trimmed.isNotBlank()) "${selectedCountry.dialCode} $trimmed" else ""
    }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var acceptTerms by remember { mutableStateOf(false) }

    var selectedCycle by remember { mutableStateOf("1er Ciclo") }
    var attemptedSubmitStep1 by remember { mutableStateOf(false) }
    var attemptedSubmitStep2 by remember { mutableStateOf(false) }
    var attemptedSubmitStep3 by remember { mutableStateOf(false) }

    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var emailFieldError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var careerError by remember { mutableStateOf<String?>(null) }
    var semesterError by remember { mutableStateOf<String?>(null) }
    var startDateError by remember { mutableStateOf<String?>(null) }
    var endDateError by remember { mutableStateOf<String?>(null) }
    var passwordFieldError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var termsError by remember { mutableStateOf<String?>(null) }

    val sdfUtc = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showConfirmEmailDialog by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (uiState) {
            is RegisterViewModel.RegisterUiState.Success -> {
                Toast.makeText(context, "¡Registro completado con éxito!", Toast.LENGTH_LONG).show()
                viewModel.resetState()
                onRegisterSuccess()
            }
            is RegisterViewModel.RegisterUiState.Error -> {
                val errorMsg = (uiState as RegisterViewModel.RegisterUiState.Error).message
                snackbarHostState.showSnackbar(message = errorMsg)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (uiState is RegisterViewModel.RegisterUiState.AwaitingVerification) {
                    viewModel.checkVerificationImmediately()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Password Strength Meter Helper
    fun calculatePasswordStrength(pass: String): Int {
        if (pass.isEmpty()) return 0
        if (pass.length < 8) return 1

        val hasLower = pass.any { it.isLowerCase() }
        val hasUpper = pass.any { it.isUpperCase() }
        val hasDigit = pass.any { it.isDigit() }
        val hasSpecial = pass.any { !it.isLetterOrDigit() }

        if (!hasUpper && !hasDigit && !hasSpecial) return 1
        if (hasLower && hasUpper && hasDigit && hasSpecial) return 4
        if (hasLower && hasUpper && hasDigit) return 3
        if ((hasLower || hasUpper) && hasDigit) return 2

        return 2
    }

    val strengthLevel = calculatePasswordStrength(password)
    val (strengthText, strengthColor) = when (strengthLevel) {
        0 -> "Mínimo 8 caracteres" to MaterialTheme.colorScheme.onSurfaceVariant
        1 -> "Seguridad Baja (1/4)" to Color(0xFFD32F2F)
        2 -> "Seguridad Media (2/4)" to Color(0xFFF57F17)
        3 -> "Seguridad Media (3/4)" to Color(0xFFF57F17)
        4 -> "Seguridad Alta (4/4)" to Color(0xFF2E7D32)
        else -> "Mínimo 8 caracteres" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Completion checks per step to disable/dim the "Continuar" button until all fields are filled
    val isStep1Complete = firstName.trim().isNotBlank() &&
            lastName.trim().isNotBlank() &&
            phone.filter { it.isDigit() }.length == selectedCountry.expectedLength

    val isStep2Complete = effectiveCareer().isNotBlank() &&
            semester.trim().length >= 6 &&
            (semester.endsWith("-I") || semester.endsWith("-II")) &&
            startDate.isNotBlank() &&
            endDate.isNotBlank() &&
            selectedCycle.isNotBlank()

    val isStep3Complete = email.trim().isNotBlank() &&
            email.contains("@") &&
            email.contains(".") &&
            (isGoogleSignUp || (password.length >= 8 && confirmPassword.isNotEmpty() && confirmPassword == password)) &&
            acceptTerms

    // Validation functions per step
    fun validateStep1(): Boolean {
        firstNameError = AuthValidation.requiredNameError(firstName, "nombres")
        lastNameError = AuthValidation.requiredNameError(lastName, "apellidos")
        phoneError = AuthValidation.phoneError(phone, selectedCountry.expectedLength, selectedCountry.dialCode)
        return firstNameError == null && lastNameError == null && phoneError == null
    }

    fun validateStep2(): Boolean {
        careerError = AuthValidation.careerError(effectiveCareer())
        semesterError = AuthValidation.semesterError(semester)
        startDateError = AuthValidation.startDateError(startDate)
        endDateError = AuthValidation.endDateError(startDate, endDate)
        return careerError == null && semesterError == null && startDateError == null && endDateError == null
    }

    fun validateStep3(): Boolean {
        emailFieldError = if (isGoogleSignUp) null else AuthValidation.emailError(email)
        passwordFieldError = if (isGoogleSignUp) null else AuthValidation.passwordError(password)
        confirmPasswordError = if (isGoogleSignUp) null else AuthValidation.confirmPasswordError(password, confirmPassword)
        termsError = if (!acceptTerms) "Debes aceptar los términos para continuar." else null
        return emailFieldError == null && passwordFieldError == null && confirmPasswordError == null && termsError == null
    }

    // Modal 1: Confirmación de Correo Electrónico Correcto (Antes de enviar enlace)
    if (showConfirmEmailDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmEmailDialog = false },
            icon = {
                Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF006C88), modifier = Modifier.size(36.dp))
            },
            title = {
                Text(
                    text = "¿Tu correo electrónico es correcto?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = email.trim(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF006C88),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Te enviaremos un enlace de verificación obligatorio para activar tu cuenta y guardar tus datos académicos. Si el correo tiene un error, no podrás completar el registro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmEmailDialog = false
                        viewModel.registerWithEmail(
                            email = email, pass = password, confirmPass = confirmPassword,
                            firstName = firstName, lastName = lastName, phone = effectivePhone(),
                            career = effectiveCareer(), semester = semester, cycle = selectedCycle,
                            startDate = startDate, endDate = endDate
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006C88)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sí, es correcto", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmEmailDialog = false }) {
                    Text("Modificar correo")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Modal 2: Hemos enviado un enlace de verificación (Elegante, sin distorsión)
    if (uiState is RegisterViewModel.RegisterUiState.AwaitingVerification) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color(0xFF006C88).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = Color(0xFF006C88),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Hemos enviado un enlace de verificación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Enviamos el enlace para activar tu cuenta a:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = email.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF006C88),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = Color(0xFF006C88).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF006C88)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Esperando que confirmes tu enlace...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF006C88)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Abre tu correo y haz clic en el enlace. Tus datos se guardarán automáticamente en la base de datos al verificar.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.completeRegistration()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006C88))
                    ) {
                        Text("Continuar a SchoolTime", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.resendVerification()
                                Toast.makeText(context, "Enlace reenviado. Revisa tu correo y spam.", Toast.LENGTH_LONG).show()
                            }
                        ) {
                            Text("Reenviar enlace", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF006C88))
                        }

                        TextButton(
                            onClick = { viewModel.cancelRegistration() }
                        ) {
                            Text(
                                text = "Cancelar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal 3: Correo Verificado con Éxito (Diseño impecable y espaciado)
    if (uiState is RegisterViewModel.RegisterUiState.VerifiedReady) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(Color(0xFF2E7D32).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "¡Correo Verificado con Éxito!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tu correo electrónico ha sido confirmado. Tu usuario, carrera y ciclo han sido guardados y activados en SchoolTime.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.completeRegistration()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Ingresar a SchoolTime", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }

    // Modal Selector de Código de País
    if (showCountryDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredCountries = remember(searchQuery) {
            if (searchQuery.isBlank()) AuthValidation.SUPPORTED_COUNTRIES
            else AuthValidation.SUPPORTED_COUNTRIES.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.dialCode.contains(searchQuery) ||
                it.code.contains(searchQuery, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showCountryDialog = false },
            icon = { Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF006C88)) },
            title = {
                Text("Selecciona tu país", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar país o código...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredCountries) { country ->
                            val isSelected = country.code == selectedCountry.code
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCountry = country
                                        phone = phone.take(country.expectedLength)
                                        showCountryDialog = false
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(country.flag, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = country.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = country.dialCode,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF006C88)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCountryDialog = false }) {
                    Text("Cerrar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isGoogleSignUp) "Registro con Google" else "Crear Cuenta",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Paso $currentStep de 3 • " + when (currentStep) {
                                1 -> "Datos Personales"
                                2 -> "Información Académica"
                                else -> "Seguridad y Acceso"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) {
                            currentStep--
                        } else {
                            viewModel.cancelRegistration()
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Modern Step Timeline Component
            RegistrationTimeline(
                currentStep = currentStep,
                onStepClick = { targetStep ->
                    // Permite retroceder a un paso anterior ya visitado
                    if (targetStep < currentStep) {
                        currentStep = targetStep
                    } else if (targetStep == 2 && currentStep == 1 && isStep1Complete) {
                        attemptedSubmitStep1 = true
                        if (validateStep1()) currentStep = 2
                    } else if (targetStep == 3 && currentStep == 2 && isStep2Complete) {
                        attemptedSubmitStep2 = true
                        if (validateStep2()) currentStep = 3
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step Content Animated
            AnimatedContent(
                targetState = currentStep,
                label = "RegistrationStepAnimation"
            ) { step ->
                when (step) {
                    1 -> {
                        // ==================== PASO 1: DATOS PERSONALES ====================
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Section Header Card
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (isGoogleSignUp) "Verifica tu Identidad" else "¿Cómo te llamas?",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Ingresa tus nombres completos y número de teléfono celular.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Field: Nombres
                            Text("Nombres:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = {
                                    firstName = AuthValidation.filterNameInput(it)
                                    if (attemptedSubmitStep1) {
                                        firstNameError = AuthValidation.requiredNameError(firstName, "nombres")
                                    }
                                },
                                placeholder = { Text("Ej. Carlos Alberto") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                singleLine = true,
                                isError = attemptedSubmitStep1 && firstNameError != null,
                                supportingText = if (attemptedSubmitStep1 && firstNameError != null) { { Text(firstNameError ?: "") } } else null,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Field: Apellidos
                            Text("Apellidos:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = {
                                    lastName = AuthValidation.filterNameInput(it)
                                    if (attemptedSubmitStep1) {
                                        lastNameError = AuthValidation.requiredNameError(lastName, "apellidos")
                                    }
                                },
                                placeholder = { Text("Ej. Flores Valle") },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                singleLine = true,
                                isError = attemptedSubmitStep1 && lastNameError != null,
                                supportingText = if (attemptedSubmitStep1 && lastNameError != null) { { Text(lastNameError ?: "") } } else null,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Field: Teléfono Celular con Código de País
                            Text("Teléfono Celular:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { input ->
                                    phone = input.filter { it.isDigit() }.take(selectedCountry.expectedLength)
                                    if (attemptedSubmitStep1) {
                                        phoneError = AuthValidation.phoneError(phone, selectedCountry.expectedLength, selectedCountry.dialCode)
                                    }
                                },
                                placeholder = { Text(selectedCountry.placeholder) },
                                leadingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable { showCountryDialog = true }
                                            .padding(start = 12.dp, end = 6.dp)
                                    ) {
                                        Text(selectedCountry.flag, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(selectedCountry.dialCode, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Elegir país", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(modifier = Modifier.height(20.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                    }
                                },
                                singleLine = true,
                                isError = attemptedSubmitStep1 && phoneError != null,
                                supportingText = if (attemptedSubmitStep1 && phoneError != null) {
                                    { Text(phoneError ?: "") }
                                } else {
                                    { Text("${selectedCountry.name} (${selectedCountry.dialCode}): ${phone.length}/${selectedCountry.expectedLength} dígitos") }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Step 1 Action Button - Disabled & Darkened until all fields are complete
                            Button(
                                onClick = {
                                    attemptedSubmitStep1 = true
                                    if (validateStep1()) {
                                        currentStep = 2
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = isStep1Complete,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Continuar a Información Académica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                                }
                            }

                            if (!isStep1Complete) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Completa tus nombres, apellidos y teléfono celular para continuar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    2 -> {
                        // ==================== PASO 2: INFORMACIÓN ACADÉMICA ====================
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Section Header Card
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.School,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Información Universitaria",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Selecciona tu carrera, ciclo de estudios y fechas del semestre.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Carrera Profesional Dropdown
                            Text("Carrera Profesional:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = careerExpanded,
                                onExpandedChange = { careerExpanded = !careerExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedCareer,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = careerExpanded) },
                                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = careerExpanded,
                                    onDismissRequest = { careerExpanded = false }
                                ) {
                                    AuthValidation.PREDEFINED_CAREERS.forEach { careerItem ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = careerItem,
                                                    fontWeight = if (careerItem == selectedCareer) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                selectedCareer = careerItem
                                                careerExpanded = false
                                                if (attemptedSubmitStep2) {
                                                    careerError = AuthValidation.careerError(effectiveCareer())
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            if (selectedCareer.startsWith("Otra", ignoreCase = true)) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Escribe tu carrera profesional:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = customCareer,
                                    onValueChange = {
                                        customCareer = AuthValidation.filterNameInput(it)
                                        if (attemptedSubmitStep2) {
                                            careerError = AuthValidation.careerError(customCareer)
                                        }
                                    },
                                    placeholder = { Text("Carrera Profesional") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    singleLine = true,
                                    isError = attemptedSubmitStep2 && careerError != null,
                                    supportingText = if (attemptedSubmitStep2) careerError?.let { { Text(it) } } else null,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Semestre Académico
                            Text("Semestre Académico:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = semester,
                                onValueChange = { input ->
                                    semester = AuthValidation.formatSemesterInput(semester, input)
                                    if (attemptedSubmitStep2) {
                                        semesterError = AuthValidation.semesterError(semester)
                                    }
                                    // Autocompletar 16 semanas si se define año y ciclo
                                    val now = java.time.LocalDate.now()
                                    val typedYear = semester.take(4).toIntOrNull() ?: now.year
                                    if (semester.endsWith("-I")) {
                                        val targetYear = if (now.monthValue >= 8 && typedYear <= now.year) now.year + 1 else typedYear
                                        startDate = "$targetYear-03-16"
                                        endDate = "$targetYear-07-06"
                                        if (attemptedSubmitStep2) {
                                            startDateError = null
                                            endDateError = null
                                        }
                                    } else if (semester.endsWith("-II")) {
                                        startDate = "$typedYear-09-07"
                                        endDate = "$typedYear-12-28"
                                        if (attemptedSubmitStep2) {
                                            startDateError = null
                                            endDateError = null
                                        }
                                    }
                                },
                                placeholder = { Text("0000-II") },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                singleLine = true,
                                isError = attemptedSubmitStep2 && semesterError != null,
                                supportingText = if (attemptedSubmitStep2 && semesterError != null) {
                                    { Text(semesterError ?: "") }
                                } else {
                                    { Text("Escribe los 4 dígitos del año (ej. 2026-I o 2026-II)") }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Ciclo de Estudios (Chips)
                            Text("Ciclo de Estudios:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            val cycles = listOf("1er Ciclo", "2do Ciclo", "3er Ciclo", "4to Ciclo", "5to Ciclo", "6to Ciclo", "7mo Ciclo", "8vo Ciclo", "9no Ciclo", "10mo Ciclo")
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                cycles.forEach { c ->
                                    val isSelected = selectedCycle == c
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        onClick = { selectedCycle = c }
                                    ) {
                                        Text(
                                            text = c,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Duración Académica Quick Select
                            Text("Accesos Rápidos de Calendario:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val now = remember { java.time.LocalDate.now() }
                                val currentYear = now.year
                                val nextYear = currentYear + 1

                                AssistChip(
                                    onClick = {
                                        semester = "$currentYear-II"
                                        startDate = "$currentYear-09-07"
                                        endDate = "$currentYear-12-28"
                                        if (attemptedSubmitStep2) {
                                            semesterError = null
                                            startDateError = null
                                            endDateError = null
                                        }
                                    },
                                    label = { Text("Ciclo II: 07 Sep - 28 Dic (16 sem)") }
                                )
                                AssistChip(
                                    onClick = {
                                        val targetYear = if (now.monthValue >= 8) nextYear else currentYear
                                        semester = "$targetYear-I"
                                        startDate = "$targetYear-03-16"
                                        endDate = "$targetYear-07-06"
                                        if (attemptedSubmitStep2) {
                                            semesterError = null
                                            startDateError = null
                                            endDateError = null
                                        }
                                    },
                                    label = { Text("Ciclo I: 16 Mar - 06 Jul (16 sem)") }
                                )
                                AssistChip(
                                    onClick = {
                                        semester = "$currentYear-II"
                                        startDate = "$currentYear-09-07"
                                        endDate = "$nextYear-01-04"
                                        if (attemptedSubmitStep2) {
                                            semesterError = null
                                            startDateError = null
                                            endDateError = null
                                        }
                                    },
                                    label = { Text("Ciclo II: 07 Sep - 04 Ene (+ aplazados)") }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Fechas de Inicio y Fin
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Fecha Inicio
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Inicio de Ciclo:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = startDate,
                                            onValueChange = {},
                                            readOnly = true,
                                            isError = attemptedSubmitStep2 && startDateError != null,
                                            supportingText = if (attemptedSubmitStep2 && startDateError != null) { { Text(startDateError ?: "") } } else null,
                                            placeholder = { Text("yy-MM-dd") },
                                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clickable { showStartDatePicker = true }
                                        )
                                    }
                                }

                                // Fecha Fin
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Fin de Ciclo:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = endDate,
                                            onValueChange = {},
                                            readOnly = true,
                                            isError = attemptedSubmitStep2 && endDateError != null,
                                            supportingText = if (attemptedSubmitStep2 && endDateError != null) { { Text(endDateError ?: "") } } else null,
                                            placeholder = { Text("yy-MM-dd") },
                                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clickable { showEndDatePicker = true }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Step 2 Action Buttons
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { currentStep = 1 },
                                    modifier = Modifier.weight(0.7f).height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Atrás", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        attemptedSubmitStep2 = true
                                        if (validateStep2()) {
                                            currentStep = 3
                                        }
                                    },
                                    modifier = Modifier.weight(1.3f).height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = isStep2Complete,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    )
                                ) {
                                    Text("Continuar a Seguridad", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }

                            if (!isStep2Complete) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Completa tu carrera, semestre y fechas del ciclo para continuar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    3 -> {
                        // ==================== PASO 3: CUENTA Y SEGURIDAD ====================
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Section Header Card
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Security,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (isGoogleSignUp) "Acceso y Confirmación" else "Seguridad de tu Cuenta",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isGoogleSignUp) "Tu cuenta de Google fue vinculada con éxito. Revisa el resumen y finaliza." else "Ingresa tu correo y contraseña para crear tu acceso institucional.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Correo Electrónico
                            Text("Correo Electrónico:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    if (!isGoogleSignUp) {
                                        email = it.trim()
                                        if (attemptedSubmitStep3) {
                                            emailFieldError = AuthValidation.emailError(email)
                                        }
                                    }
                                },
                                readOnly = isGoogleSignUp,
                                enabled = !isGoogleSignUp,
                                isError = attemptedSubmitStep3 && emailFieldError != null,
                                supportingText = if (attemptedSubmitStep3 && emailFieldError != null) {
                                    { Text(emailFieldError ?: "") }
                                } else if (isGoogleSignUp) {
                                    { Text("Cuenta verificada con Google (no editable)") }
                                } else {
                                    { Text("Debe ser un correo válido para activar tu cuenta.") }
                                },
                                placeholder = { Text("ejemplo@gmail.com") },
                                leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )

                            if (!isGoogleSignUp) {
                                Spacer(modifier = Modifier.height(16.dp))

                                // Contraseña
                                Text("Contraseña:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = {
                                        password = it
                                        if (attemptedSubmitStep3) {
                                            passwordFieldError = AuthValidation.passwordError(password)
                                        }
                                    },
                                    placeholder = { Text("Mínimo 8 caracteres") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    trailingIcon = {
                                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(imageVector = image, contentDescription = "Mostrar contraseña")
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    isError = attemptedSubmitStep3 && passwordFieldError != null,
                                    supportingText = if (attemptedSubmitStep3 && passwordFieldError != null) { { Text(passwordFieldError ?: "") } } else null,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Password Strength Meter (4 Segmented Bars)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val filledColor1 = if (strengthLevel >= 1) strengthColor else MaterialTheme.colorScheme.outlineVariant
                                    Box(modifier = Modifier.weight(1f).height(4.dp).background(filledColor1, RoundedCornerShape(50)))

                                    val filledColor2 = if (strengthLevel >= 2) strengthColor else MaterialTheme.colorScheme.outlineVariant
                                    Box(modifier = Modifier.weight(1f).height(4.dp).background(filledColor2, RoundedCornerShape(50)))

                                    val filledColor3 = if (strengthLevel >= 3) strengthColor else MaterialTheme.colorScheme.outlineVariant
                                    Box(modifier = Modifier.weight(1f).height(4.dp).background(filledColor3, RoundedCornerShape(50)))

                                    val filledColor4 = if (strengthLevel >= 4) strengthColor else MaterialTheme.colorScheme.outlineVariant
                                    Box(modifier = Modifier.weight(1f).height(4.dp).background(filledColor4, RoundedCornerShape(50)))
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = strengthText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = strengthColor,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Confirmar Contraseña
                                val isConfirmMismatch = confirmPassword.isNotEmpty() && confirmPassword != password
                                Text("Confirmar Contraseña:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = {
                                        confirmPassword = it
                                        if (attemptedSubmitStep3) {
                                            confirmPasswordError = AuthValidation.confirmPasswordError(password, confirmPassword)
                                        }
                                    },
                                    placeholder = { Text("Repite tu contraseña") },
                                    leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (confirmPassword.isNotEmpty() && confirmPassword == password) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Contraseñas coinciden",
                                                    tint = Color(0xFF2E7D32),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                                Icon(imageVector = image, contentDescription = "Mostrar contraseña")
                                            }
                                        }
                                    },
                                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    isError = isConfirmMismatch || (attemptedSubmitStep3 && confirmPasswordError != null),
                                    supportingText = if (isConfirmMismatch) {
                                        { Text("Las contraseñas no coinciden") }
                                    } else if (attemptedSubmitStep3 && confirmPasswordError != null) {
                                        { Text(confirmPasswordError ?: "") }
                                    } else null,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Resumen del Registro Universitario
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Text("Ficha de Registro", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        UserAvatar(
                                            photoUri = null,
                                            name = "$firstName $lastName",
                                            size = 46.dp,
                                            fontSize = 18.sp
                                        )
                                        Column {
                                            Text(
                                                text = "${firstName.trim()} ${lastName.trim()}".ifBlank { "Estudiante" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${effectiveCareer()} • $selectedCycle",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Semestre: $semester • ${effectivePhone()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Términos de Servicio y Privacidad
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (attemptedSubmitStep3 && termsError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                    Checkbox(
                                        checked = acceptTerms,
                                        onCheckedChange = { acceptTerms = it },
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Column {
                                        Text("Acepto Términos de Servicio y Sincronización", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text("Cifrado de extremo a extremo, políticas de privacidad y sincronización académica.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            if (attemptedSubmitStep3 && termsError != null) {
                                Text(
                                    text = termsError ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 14.dp, top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            // Step 3 Action Buttons
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { currentStep = 2 },
                                    modifier = Modifier.weight(0.7f).height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
                                    enabled = uiState !is RegisterViewModel.RegisterUiState.Loading
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Atrás", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        attemptedSubmitStep3 = true
                                        if (!validateStep1()) {
                                            currentStep = 1
                                            return@Button
                                        }
                                        if (!validateStep2()) {
                                            currentStep = 2
                                            return@Button
                                        }
                                        if (!validateStep3()) {
                                            return@Button
                                        }

                                        if (isGoogleSignUp) {
                                            viewModel.completeGoogleRegistration(
                                                firstName = firstName, lastName = lastName, email = email,
                                                phone = effectivePhone(), career = effectiveCareer(), semester = semester, cycle = selectedCycle,
                                                startDate = startDate, endDate = endDate
                                            )
                                        } else {
                                            showConfirmEmailDialog = true
                                        }
                                    },
                                    modifier = Modifier.weight(1.3f).height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = isStep3Complete && uiState !is RegisterViewModel.RegisterUiState.Loading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    )
                                ) {
                                    if (uiState is RegisterViewModel.RegisterUiState.Loading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = if (isGoogleSignUp) "Completar con Google" else "Crear Cuenta",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }

                            if (!isStep3Complete) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isGoogleSignUp) {
                                        "Acepta los términos de servicio para completar tu cuenta"
                                    } else {
                                        "Ingresa correo, contraseña (mínimo 8 caracteres) y acepta los términos"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    // DatePickers with UTC TimeZone conversion & dynamic centering
    if (showStartDatePicker) {
        val startInitialMillis = remember(startDate) {
            val localDate = AuthValidation.parseDate(startDate) ?: java.time.LocalDate.now()
            localDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val currentStartDatePickerState = rememberDatePickerState(initialSelectedDateMillis = startInitialMillis)

        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = currentStartDatePickerState.selectedDateMillis ?: startInitialMillis
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = millis
                    }
                    val formatted = sdfUtc.format(cal.time)
                    startDate = formatted

                    val parsed = AuthValidation.parseDate(formatted)
                    if (parsed != null) {
                        endDate = parsed.plusWeeks(16).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    }

                    if (attemptedSubmitStep2) {
                        startDateError = AuthValidation.startDateError(startDate)
                        if (endDate.isNotBlank()) {
                            endDateError = AuthValidation.endDateError(startDate, endDate)
                        }
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = currentStartDatePickerState) }
    }

    if (showEndDatePicker) {
        val endInitialMillis = remember(startDate, endDate) {
            val localDate = AuthValidation.parseDate(endDate)
                ?: AuthValidation.parseDate(startDate)?.plusMonths(4)
                ?: java.time.LocalDate.now().plusMonths(4)
            localDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val currentEndDatePickerState = rememberDatePickerState(initialSelectedDateMillis = endInitialMillis)

        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = currentEndDatePickerState.selectedDateMillis ?: endInitialMillis
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = millis
                    }
                    endDate = sdfUtc.format(cal.time)
                    if (attemptedSubmitStep2) {
                        endDateError = AuthValidation.endDateError(startDate, endDate)
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = currentEndDatePickerState) }
    }
}

/**
 * Modern, interactive Registration Timeline component showing progression across
 * Step 1 (Personal), Step 2 (Académico), and Step 3 (Seguridad).
 */
@Composable
fun RegistrationTimeline(
    currentStep: Int,
    onStepClick: (Int) -> Unit = {}
) {
    val steps = listOf(
        Triple(1, "Personal", Icons.Default.Person),
        Triple(2, "Académico", Icons.Default.School),
        Triple(3, "Seguridad", Icons.Default.Security)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                steps.forEachIndexed { index, (stepNum, label, icon) ->
                    val isCompleted = currentStep > stepNum
                    val isActive = currentStep == stepNum

                    // Step Node
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable(enabled = isCompleted) { onStepClick(stepNum) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCompleted -> Color(0xFF2E7D32)
                                        isActive -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .then(
                                    if (isActive) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Paso completado",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = when {
                                isActive -> MaterialTheme.colorScheme.primary
                                isCompleted -> Color(0xFF2E7D32)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Connector line between step nodes
                    if (index < steps.size - 1) {
                        val lineFilled = currentStep > stepNum
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .padding(horizontal = 8.dp)
                                .padding(bottom = 18.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (lineFilled) Color(0xFF2E7D32)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar
            val progress = when (currentStep) {
                1 -> 0.33f
                2 -> 0.66f
                else -> 1f
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (currentStep == 3) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
