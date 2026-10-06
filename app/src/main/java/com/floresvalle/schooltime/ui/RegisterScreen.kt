package com.floresvalle.schooltime.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    BackHandler {
        viewModel.cancelRegistration()
        onNavigateBack()
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
    var attemptedSubmit by remember { mutableStateOf(false) }
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

    fun validateRegisterFields(): Boolean {
        firstNameError = AuthValidation.requiredNameError(firstName, "nombres")
        lastNameError = AuthValidation.requiredNameError(lastName, "apellidos")
        emailFieldError = if (isGoogleSignUp) null else AuthValidation.emailError(email)
        phoneError = AuthValidation.phoneError(phone, selectedCountry.expectedLength, selectedCountry.dialCode)
        careerError = AuthValidation.careerError(effectiveCareer())
        semesterError = AuthValidation.semesterError(semester)
        startDateError = AuthValidation.startDateError(startDate)
        endDateError = AuthValidation.endDateError(startDate, endDate)
        passwordFieldError = if (isGoogleSignUp) null else AuthValidation.passwordError(password)
        confirmPasswordError = if (isGoogleSignUp) null else AuthValidation.confirmPasswordError(password, confirmPassword)
        termsError = if (!acceptTerms) "Debes aceptar los términos para continuar." else null
        return listOf(
            firstNameError, lastNameError, emailFieldError, phoneError, careerError,
            semesterError, startDateError, endDateError, passwordFieldError, confirmPasswordError, termsError
        ).all { it == null }
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

    // Modal 2: Hemos enviado un enlace de verificación (Elegante, sin solapamientos ni distorsión)
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

    // Modal Selector de Código de País (Experiencia Élite)
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
                        Text(if (isGoogleSignUp) "Completar Registro con Google" else "Crear Cuenta", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("SchoolTime Universitario", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancelRegistration()
                        onNavigateBack()
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
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Logo",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(if (isGoogleSignUp) "Onboarding Google" else "Nuevo Ingreso", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = if (isGoogleSignUp) "Completa tu Perfil" else "Únete a SchoolTime", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isGoogleSignUp) "Tu cuenta de Google fue verificada. Puedes editar tus nombres y completar tu carrera y fechas de ciclo." else "Configura tu perfil institucional para automatizar tus horarios y ciclo académico.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Form Fields: Nombres & Apellidos (Bloqueo absoluto de números)
            Text("Nombres *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = firstName,
                onValueChange = {
                    firstName = AuthValidation.filterNameInput(it)
                    if (attemptedSubmit) {
                        firstNameError = AuthValidation.requiredNameError(firstName, "nombres")
                    }
                },
                placeholder = { Text("Ej. Harol") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                isError = attemptedSubmit && firstNameError != null,
                supportingText = if (attemptedSubmit) firstNameError?.let { { Text(it) } } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Apellidos *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = {
                    lastName = AuthValidation.filterNameInput(it)
                    if (attemptedSubmit) {
                        lastNameError = AuthValidation.requiredNameError(lastName, "apellidos")
                    }
                },
                placeholder = { Text("Ej. Flores Valle") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                isError = attemptedSubmit && lastNameError != null,
                supportingText = if (attemptedSubmit) lastNameError?.let { { Text(it) } } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email (LOCKED for Google, editable for Email sign up)
            Text("Correo Electrónico *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {
                    if (!isGoogleSignUp) {
                        email = it.trim()
                        if (attemptedSubmit) {
                            emailFieldError = AuthValidation.emailError(email)
                        }
                    }
                },
                readOnly = isGoogleSignUp,
                enabled = !isGoogleSignUp,
                isError = attemptedSubmit && emailFieldError != null,
                supportingText = if (attemptedSubmit && emailFieldError != null) {
                    { Text(emailFieldError ?: "") }
                } else if (isGoogleSignUp) {
                    { Text("Verificado con Google (no modificable)") }
                } else {
                    { Text("Debe incluir '@' y dominio completo (ej. @gmail.com, @hotmail.com, @uni.edu.pe)") }
                },
                placeholder = { Text("ejemplo@gmail.com") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Teléfono Celular con Código de País (Solo números, longitud controlada)
            Text("Teléfono celular *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { input ->
                    phone = input.filter { it.isDigit() }.take(selectedCountry.expectedLength)
                    if (attemptedSubmit) {
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
                isError = attemptedSubmit && phoneError != null,
                supportingText = if (attemptedSubmit && phoneError != null) {
                    { Text(phoneError ?: "") }
                } else {
                    { Text("${selectedCountry.name} (${selectedCountry.dialCode}): ${phone.length}/${selectedCountry.expectedLength} dígitos (solo números)") }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Carrera Profesional: Dropdown Select con "Otra (Especificar)"
            Text("Carrera Profesional *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
                    shape = RoundedCornerShape(12.dp)
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
                                if (attemptedSubmit) {
                                    careerError = AuthValidation.careerError(effectiveCareer())
                                }
                            }
                        )
                    }
                }
            }

            if (selectedCareer.startsWith("Otra", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Escribe tu carrera profesional *", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF006C88))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = customCareer,
                    onValueChange = {
                        customCareer = AuthValidation.filterNameInput(it)
                        if (attemptedSubmit) {
                            careerError = AuthValidation.careerError(customCareer)
                        }
                    },
                    placeholder = { Text("Ej. Ingeniería Biomédica") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    isError = attemptedSubmit && careerError != null,
                    supportingText = if (attemptedSubmit) careerError?.let { { Text(it) } } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Academic Period Section Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Periodo Académico (Ciclo)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Semestre Académico *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = semester,
                        onValueChange = { input ->
                            semester = AuthValidation.formatSemesterInput(semester, input)
                            if (attemptedSubmit) {
                                semesterError = AuthValidation.semesterError(semester)
                            }
                            // Si se completa el semestre (ej. 2026-I o 2026-II) y las fechas están vacías, autocompletar 16 semanas exactas
                            val year = semester.take(4).toIntOrNull() ?: java.time.LocalDate.now().year
                            if (semester.endsWith("-I")) {
                                startDate = "$year-03-16"
                                endDate = "$year-07-06" // 16 semanas exactas
                                if (attemptedSubmit) {
                                    startDateError = null
                                    endDateError = null
                                }
                            } else if (semester.endsWith("-II")) {
                                startDate = "$year-09-07"
                                endDate = "$year-12-28" // 16 semanas fijas exactas (28 Dic)
                                if (attemptedSubmit) {
                                    startDateError = null
                                    endDateError = null
                                }
                            }
                        },
                        placeholder = { Text("Ej. 2026-II") },
                        singleLine = true,
                        isError = attemptedSubmit && semesterError != null,
                        supportingText = if (attemptedSubmit && semesterError != null) {
                            { Text(semesterError ?: "") }
                        } else {
                            { Text("Escribe los 4 dígitos del año y se agregará '-' automáticamente. Luego solo ingresa 'I' o 'II'.") }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Ciclo de Estudios", style = MaterialTheme.typography.labelMedium)
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
                                color = if (isSelected) Color(0xFF006C88) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                onClick = { selectedCycle = c }
                            ) {
                                Text(
                                    text = c,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Duración Académica (16 semanas de estudio + 1 semana aplazados)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentYear = remember {
                            semester.take(4).toIntOrNull() ?: java.time.LocalDate.now().year
                        }
                        AssistChip(
                            onClick = {
                                startDate = "$currentYear-09-07"
                                endDate = "$currentYear-12-28" // 16 semanas fijas
                                if (attemptedSubmit) {
                                    startDateError = null
                                    endDateError = null
                                }
                            },
                            label = { Text("Ciclo II: 07 Sep - 28 Dic (16 sem)") }
                        )
                        AssistChip(
                            onClick = {
                                startDate = "$currentYear-09-07"
                                val nextYear = currentYear + 1
                                endDate = "$nextYear-01-04" // 17 semanas con aplazados
                                if (attemptedSubmit) {
                                    startDateError = null
                                    endDateError = null
                                }
                            },
                            label = { Text("Ciclo II + Aplazados (17 sem)") }
                        )
                        AssistChip(
                            onClick = {
                                startDate = "$currentYear-03-16"
                                endDate = "$currentYear-07-06" // 16 semanas fijas
                                if (attemptedSubmit) {
                                    startDateError = null
                                    endDateError = null
                                }
                            },
                            label = { Text("Ciclo I: 16 Mar - 06 Jul (16 sem)") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // MANDATORY CYCLE START & END DATES (Clickable overlay + Error validation)
                    Text("Fecha de Inicio del Ciclo *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = {},
                            readOnly = true,
                            isError = attemptedSubmit && startDateError != null,
                            supportingText = if (attemptedSubmit) startDateError?.let { { Text(it) } } else null,
                            placeholder = { Text("Ej. 2026-09-07") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Inicio de ciclo", tint = Color(0xFF006C88)) },
                            trailingIcon = {
                                IconButton(onClick = { showStartDatePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Elegir fecha de inicio")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                errorBorderColor = MaterialTheme.colorScheme.error
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showStartDatePicker = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Fecha de Fin del Ciclo *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = {},
                            readOnly = true,
                            isError = attemptedSubmit && endDateError != null,
                            supportingText = if (attemptedSubmit) endDateError?.let { { Text(it) } } else null,
                            placeholder = { Text("Ej. 2026-12-28") },
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = "Fin de ciclo", tint = Color(0xFF006C88)) },
                            trailingIcon = {
                                IconButton(onClick = { showEndDatePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Elegir fecha de fin")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                errorBorderColor = MaterialTheme.colorScheme.error
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showEndDatePicker = true }
                        )
                    }

                    if (startDate.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = {
                                AuthValidation.parseDate(startDate)?.let {
                                    endDate = it.plusWeeks(16).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                                    if (attemptedSubmit) endDateError = null
                                }
                            }) {
                                Text("Ajustar a 16 semanas de clases", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = {
                                AuthValidation.parseDate(startDate)?.let {
                                    endDate = it.plusWeeks(17).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                                    if (attemptedSubmit) endDateError = null
                                }
                            }) {
                                Text("+ 1 sem. aplazados (17 sem)", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isGoogleSignUp) {
                // Password Section (Includes Strength Meter & Eye Icon Toggle)
                Text("Contraseña *", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("••••••••••••") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = "Mostrar contraseña")
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Password Strength Bar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val filledColor = if (strengthLevel >= 1) strengthColor else MaterialTheme.colorScheme.outlineVariant
                    Box(modifier = Modifier.weight(1f).height(4.dp).background(filledColor, RoundedCornerShape(50)))

                    val filledColor2 = if (strengthLevel >= 2) strengthColor else MaterialTheme.colorScheme.outlineVariant
                    Box(modifier = Modifier.weight(1f).height(4.dp).background(filledColor2, RoundedCornerShape(50)))

                    val filledColor3 = if (strengthLevel >= 3) strengthColor else MaterialTheme.colorScheme.outlineVariant
                    Box(modifier = Modifier.weight(1f).height(4.dp).background(filledColor3, RoundedCornerShape(50)))

                    val filledColor4 = if (strengthLevel >= 4) strengthColor else MaterialTheme.colorScheme.outlineVariant
                    Box(modifier = Modifier.weight(1f).height(4.dp).background(filledColor4, RoundedCornerShape(50)))
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (strengthLevel == 4) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp), tint = strengthColor)
                            Spacer(modifier = Modifier.width(4.dp))
                        } else if (strengthLevel in 1..3) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(12.dp), tint = strengthColor)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(strengthText, style = MaterialTheme.typography.labelSmall, color = strengthColor)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val isConfirmPasswordError = confirmPassword.isNotEmpty() && confirmPassword != password

                Text("Confirmar Contraseña *", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = { Text("••••••••••••") },
                    leadingIcon = { Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (confirmPassword.isNotEmpty() && confirmPassword == password) {
                                Icon(
                                    imageVector = Icons.Default.Check,
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = isConfirmPasswordError
                )
                if (isConfirmPasswordError) {
                    Text(
                        text = "Las contraseñas no coinciden",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Checkbox(
                        checked = acceptTerms,
                        onCheckedChange = { acceptTerms = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text("Acepto Términos de Servicio y Sincronización", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Cifrado de extremo a extremo y políticas de privacidad.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    attemptedSubmit = true
                    if (!validateRegisterFields()) {
                        return@Button
                    }
                    if (isGoogleSignUp) {
                        viewModel.completeGoogleRegistration(
                            firstName = firstName, lastName = lastName, email = email,
                            phone = effectivePhone(), career = effectiveCareer(), semester = semester, cycle = selectedCycle,
                            startDate = startDate, endDate = endDate
                        )
                    } else {
                        // Antes de registrar o enviar enlace, mostrar modal de confirmación de correo
                        showConfirmEmailDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = uiState !is RegisterViewModel.RegisterUiState.Loading && email.isNotBlank() && acceptTerms
            ) {
                if (uiState is RegisterViewModel.RegisterUiState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (isGoogleSignUp) "Completar Registro con Google" else "Registrar y Comenzar Ciclo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // DatePickers with UTC TimeZone conversion & dynamic month/year centering
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

                    // El ciclo universitario dura 16 semanas fijas de clases (112 días)
                    val parsed = AuthValidation.parseDate(formatted)
                    if (parsed != null) {
                        endDate = parsed.plusWeeks(16).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    }

                    if (attemptedSubmit) {
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
                    if (attemptedSubmit) {
                        endDateError = AuthValidation.endDateError(startDate, endDate)
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = currentEndDatePickerState) }
    }
}
