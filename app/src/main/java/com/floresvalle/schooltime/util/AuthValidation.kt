package com.floresvalle.schooltime.util

import android.util.Patterns
import com.google.firebase.auth.FirebaseAuthException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class CountryPhoneCode(
    val code: String,
    val name: String,
    val dialCode: String,
    val flag: String,
    val expectedLength: Int,
    val placeholder: String
)

object AuthValidation {
    val SUPPORTED_COUNTRIES = listOf(
        CountryPhoneCode("PE", "Perú", "+51", "🇵🇪", 9, "999 999 99"),
        CountryPhoneCode("CO", "Colombia", "+57", "🇨🇴", 10, "300 300 0000"),
        CountryPhoneCode("MX", "México", "+52", "🇲🇽", 10, "55 5555 5555"),
        CountryPhoneCode("AR", "Argentina", "+54", "🇦🇷", 10, "11 1111 1111"),
        CountryPhoneCode("CL", "Chile", "+56", "🇨🇱", 9, "9 1234 5678"),
        CountryPhoneCode("EC", "Ecuador", "+593", "🇪🇨", 9, "9 1234 5678"),
        CountryPhoneCode("BO", "Bolivia", "+591", "🇧🇴", 8, "7123 4567"),
        CountryPhoneCode("ES", "España", "+34", "🇪🇸", 9, "612 345 678"),
        CountryPhoneCode("US", "Estados Unidos", "+1", "🇺🇸", 10, "202 555 0123"),
        CountryPhoneCode("BR", "Brasil", "+55", "🇧🇷", 11, "11 91234 5678"),
        CountryPhoneCode("VE", "Venezuela", "+58", "🇻🇪", 10, "412 123 4567"),
        CountryPhoneCode("UY", "Uruguay", "+598", "🇺🇾", 8, "91 234 567"),
        CountryPhoneCode("PY", "Paraguay", "+595", "🇵🇾", 9, "981 123 456"),
        CountryPhoneCode("PA", "Panamá", "+507", "🇵🇦", 8, "6123 4567"),
        CountryPhoneCode("CR", "Costa Rica", "+506", "🇨🇷", 8, "8123 4567"),
        CountryPhoneCode("DO", "Rep. Dominicana", "+1", "🇩🇴", 10, "809 111 1111"),
        CountryPhoneCode("GT", "Guatemala", "+502", "🇬🇹", 8, "5555 5555")
    )

    val PREDEFINED_CAREERS = listOf(
        "Selecciona tu carrera",
        "Ingeniería de Sistemas",
        "Ingeniería de Software",
        "Ingeniería Informática",
        "Ingeniería Industrial",
        "Ingeniería Civil",
        "Ingeniería Mecatrónica",
        "Ingeniería Electrónica",
        "Ingeniería Ambiental",
        "Medicina Humana",
        "Enfermería",
        "Psicología",
        "Derecho",
        "Administración de Empresas",
        "Contabilidad y Finanzas",
        "Arquitectura y Urbanismo",
        "Economía",
        "Ciencias de la Comunicación",
        "Educación",
        "Otra (Especificar)"
    )

    private val semesterRegex = Regex("""^\d{4}-(I|II)$""")
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun filterNameInput(input: String): String {
        // Solo permite letras y espacios. Bloquea absolutamente números y caracteres especiales
        return input.filter { it.isLetter() || it.isWhitespace() }
    }

    fun formatSemesterInput(currentText: String, newText: String): String {
        // Manejo de borrado (retroceso)
        if (newText.length < currentText.length) {
            if (currentText.endsWith("-") && !newText.endsWith("-")) {
                return currentText.dropLast(1)
            }
            return newText
        }

        val cleaned = newText.uppercase()
        val yearDigits = cleaned.takeWhile { it.isDigit() }.take(4)
        if (yearDigits.length < 4) {
            return yearDigits
        }

        // Ya hay 4 dígitos (ej. 2026)
        val rawAfter = cleaned.drop(4).removePrefix("-")
        val convertedAfter = rawAfter.replace("1", "I").replace("2", "II")
        val period = convertedAfter.filter { it == 'I' }.take(2)

        return if (period.isEmpty()) {
            "$yearDigits-"
        } else {
            "$yearDigits-$period"
        }
    }

    fun normalizeEmail(email: String): String = email.trim()

    fun emailError(email: String): String? {
        val value = normalizeEmail(email)
        if (value.isBlank()) return "Ingresa tu correo electrónico."
        if (value.contains(" ")) return "El correo no debe contener espacios."
        if (!value.contains("@")) {
            return "Falta el símbolo '@' (ej. usuario@gmail.com)."
        }
        val atCount = value.count { it == '@' }
        if (atCount > 1) {
            return "El correo solo debe contener un símbolo '@'."
        }

        val parts = value.split("@")
        val localPart = parts[0]
        val domainPart = parts.getOrNull(1) ?: ""

        if (localPart.isBlank()) {
            return "Falta el nombre de usuario antes del '@'."
        }
        if (domainPart.isBlank()) {
            return "Falta el dominio después del '@' (ej. @gmail.com, @hotmail.com)."
        }
        if (!domainPart.contains(".")) {
            return "Falta el punto '.' después del dominio (ej. @gmail.com o @hotmail.com)."
        }
        if (domainPart.startsWith(".") || domainPart.endsWith(".")) {
            return "El dominio no puede empezar ni terminar con un punto '.'."
        }

        val domainTokens = domainPart.split(".")
        if (domainTokens.any { it.isBlank() }) {
            return "El dominio no puede tener puntos consecutivos."
        }
        val extension = domainTokens.last()
        if (extension.length < 2 || !extension.all { it.isLetter() }) {
            return "El correo debe terminar en una extensión válida (ej. .com, .edu, .pe)."
        }

        if (!emailRegex.matches(value)) {
            return "El formato del correo es inválido (ej. usuario@gmail.com)."
        }
        return null
    }

    fun passwordError(password: String, forLogin: Boolean = false): String? {
        if (password.isBlank()) return "Ingresa tu contraseña."
        if (!forLogin) {
            if (password.length < 8) return "La contraseña debe tener al menos 8 caracteres."
            val hasLetters = password.any { it.isLetter() }
            val hasDigitsOrSymbol = password.any { it.isDigit() || !it.isLetterOrDigit() }
            if (!hasLetters || !hasDigitsOrSymbol) {
                return "La contraseña debe combinar letras con números o símbolos."
            }
        }
        return null
    }

    fun confirmPasswordError(password: String, confirm: String): String? {
        if (confirm.isBlank()) return "Confirma tu contraseña."
        if (confirm != password) return "Las contraseñas no coinciden."
        return null
    }

    fun requiredNameError(value: String, field: String): String? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return "Ingresa tus $field."
        if (trimmed.length < 2) return "Los $field deben tener al menos 2 caracteres."
        if (trimmed.any { it.isDigit() }) return "Los $field no deben contener números."
        return null
    }

    fun phoneError(phone: String, expectedLength: Int = 9, dialCode: String = "+51"): String? {
        val trimmed = phone.trim()
        if (trimmed.isBlank()) return "Ingresa tu teléfono celular."
        val digits = phone.filter { it.isDigit() }
        if (dialCode == "+51" || phone.startsWith("+51")) {
            val local = if (digits.startsWith("51") && digits.length == 11) digits.removePrefix("51") else digits
            if (local.length != 9 || !local.startsWith("9")) {
                return "Ingresa un celular peruano válido (9 dígitos, inicia con 9)."
            }
        } else {
            if (digits.length != expectedLength) {
                return "El celular debe tener $expectedLength dígitos para $dialCode."
            }
        }
        return null
    }

    fun careerError(career: String): String? {
        if (career.trim().isBlank()) return "Ingresa tu carrera profesional."
        if (career.trim().length < 3) return "La carrera es demasiado corta."
        return null
    }

    fun semesterError(semester: String): String? {
        val value = semester.trim()
        if (value.isBlank()) return "Ingresa el semestre (ej. 2026-I)."
        if (!semesterRegex.matches(value)) return "Usa el formato 2026-I o 2026-II."
        return null
    }

    fun parseDate(value: String): LocalDate? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null

        val patterns = listOf(
            "yyyy-MM-dd",
            "d/M/yyyy",
            "dd/MM/yyyy",
            "d-M-yyyy",
            "dd-MM-yyyy",
            "yyyy/M/d",
            "yyyy/MM/dd"
        )
        for (pattern in patterns) {
            try {
                return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern(pattern, java.util.Locale.US))
            } catch (_: Exception) {}
        }
        return try {
            LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            null
        }
    }

    fun normalizeDate(value: String): String {
        return parseDate(value)?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: value.trim()
    }

    fun startDateError(startDate: String): String? {
        val trimmed = startDate.trim()
        if (trimmed.isBlank()) return "Selecciona la fecha de inicio del ciclo."
        val date = parseDate(trimmed) ?: return "La fecha de inicio no es válida (ej. 2026-03-16)."
        if (date.year < 2020 || date.year > 2035) {
            return "El año de inicio (${date.year}) no es válido."
        }
        return null
    }

    fun endDateError(startDate: String, endDate: String): String? {
        val trimmedEnd = endDate.trim()
        if (trimmedEnd.isBlank()) return "Selecciona la fecha de fin del ciclo."
        val end = parseDate(trimmedEnd) ?: return "La fecha de fin no es válida (ej. 2026-07-20)."
        val start = parseDate(startDate.trim())
        if (start != null) {
            if (!end.isAfter(start)) {
                return "La fecha de fin debe ser posterior a la de inicio."
            }
            if (end.year > start.year + 1) {
                return "La fecha de fin no debe superar el año del ciclo."
            }
        }
        return null
    }

    fun parseOptionalNavArg(value: String?): String? {
        return value?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    }

    fun mapFirebaseAuthError(error: Throwable, isRegister: Boolean = false): String {
        val code = (error as? FirebaseAuthException)?.errorCode.orEmpty()
        return when (code) {
            "ERROR_INVALID_EMAIL" -> "El correo no es válido."
            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_INVALID_LOGIN_CREDENTIALS" ->
                if (isRegister) "No se pudo crear la cuenta. Revisa los datos."
                else "Correo o contraseña incorrectos."
            "ERROR_USER_NOT_FOUND" -> "No existe una cuenta con este correo."
            "ERROR_USER_DISABLED" -> "Esta cuenta está deshabilitada."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo ya está registrado. Inicia sesión o usa Google."
            "ERROR_WEAK_PASSWORD" -> "La contraseña es demasiado débil. Usa al menos 8 caracteres."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Sin conexión. Revisa tu internet e inténtalo de nuevo."
            "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Espera un momento e inténtalo otra vez."
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "Ya existe una cuenta con este correo usando otro método de acceso."
            else -> error.localizedMessage?.takeIf { it.isNotBlank() }
                ?: if (isRegister) "No se pudo completar el registro." else "No se pudo iniciar sesión."
        }
    }
}
