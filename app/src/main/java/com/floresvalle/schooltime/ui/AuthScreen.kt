package com.floresvalle.schooltime.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.floresvalle.schooltime.R
import com.floresvalle.schooltime.util.AuthValidation
import com.floresvalle.schooltime.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToGoogleOnboarding: (email: String, name: String, photo: String?, uid: String) -> Unit = { _, _, _, _ -> },
    onNavigateToOnboarding: () -> Unit = {}
) {
    var email by remember { mutableStateOf(viewModel.initialEmail) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(viewModel.initialRememberMe) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var attemptedSubmit by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val isEmailLoading = authState is AuthViewModel.AuthState.Loading && (authState as AuthViewModel.AuthState.Loading).mode == AuthViewModel.AuthMode.EMAIL
    val isGoogleLoading = authState is AuthViewModel.AuthState.Loading && (authState as AuthViewModel.AuthState.Loading).mode == AuthViewModel.AuthMode.GOOGLE
    val isAnyLoading = authState is AuthViewModel.AuthState.Loading

    val snackbarHostState = remember { SnackbarHostState() }

    fun validateLoginFields(): Boolean {
        val nextEmailError = AuthValidation.emailError(email)
        val nextPasswordError = AuthValidation.passwordError(password, forLogin = true)
        emailError = nextEmailError
        passwordError = nextPasswordError
        return nextEmailError == null && nextPasswordError == null
    }

    fun submitLogin() {
        attemptedSubmit = true
        viewModel.clearError()
        if (validateLoginFields()) {
            viewModel.login(email, password, rememberMe)
        }
    }

    LaunchedEffect(authState) {
        val state = authState
        when (state) {
            is AuthViewModel.AuthState.Success -> {
                state.message?.let { msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                }
                onNavigateToHome()
            }
            is AuthViewModel.AuthState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearError()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                ) {
                    Text(
                        text = data.visuals.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Badge
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.wrapContentSize()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "PORTAL ACADÉMICO 2026-II",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Logo
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Logo",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Subtitle
            Text(
                text = "SchoolTime",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Tu vida universitaria organizada en un solo lugar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Container Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Correo",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            viewModel.clearError()
                            emailError = if (attemptedSubmit) AuthValidation.emailError(it) else null
                        },
                        placeholder = { Text("ejemplo@gmail.com", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AlternateEmail,
                                contentDescription = "Email",
                                tint = if (emailError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        isError = emailError != null,
                        supportingText = emailError?.let { { Text(it) } }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Contraseña",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            viewModel.clearError()
                            passwordError = if (attemptedSubmit) AuthValidation.passwordError(it, forLogin = true) else null
                        },
                        placeholder = { Text("••••••••••••", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Password",
                                tint = if (passwordError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitLogin() }),
                        singleLine = true,
                        isError = passwordError != null,
                        supportingText = passwordError?.let { { Text(it) } }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text("Recordar sesión", style = MaterialTheme.typography.bodyMedium)
                        }

                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                                Text("Red Campus OK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { submitLogin() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !isAnyLoading
                    ) {
                        if (isEmailLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Iniciar Sesión", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(" O CONTINÚA CON ", modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google Button with official 4-color Google "G" Logo
            OutlinedButton(
                onClick = {
                    viewModel.clearError()
                    viewModel.loginWithGoogle(
                        context = context,
                        rememberMe = rememberMe,
                        onNewUser = { googleEmail, googleName, googlePhoto, googleUid ->
                            onNavigateToGoogleOnboarding(googleEmail, googleName, googlePhoto, googleUid)
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                enabled = !isAnyLoading
            ) {
                if (isGoogleLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Logo oficial de Google",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                        Text("Continuar con Google", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("¿Eres nuevo estudiante?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onNavigateToRegister,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Crear cuenta", modifier = Modifier.size(20.dp))
                    Text("Crear cuenta nueva", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onNavigateToOnboarding,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ver introducción y guía", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text("•", color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.width(8.dp))

                var showClearRoomConfirmDialog by remember { mutableStateOf(false) }

                TextButton(
                    onClick = { showClearRoomConfirmDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Limpiar Room", style = MaterialTheme.typography.labelSmall)
                }

                if (showClearRoomConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearRoomConfirmDialog = false },
                        icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        title = { Text("¿Borrar datos en Room?", fontWeight = FontWeight.Bold) },
                        text = {
                            Text("Esta acción eliminará todos los registros de usuarios, periodos y cursos almacenados localmente en la base de datos Room del dispositivo.")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showClearRoomConfirmDialog = false
                                    viewModel.clearRoomData {
                                        Toast.makeText(context, "Base de datos Room limpiada exitosamente.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Borrar Datos de Room")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearRoomConfirmDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
