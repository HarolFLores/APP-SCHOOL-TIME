package com.floresvalle.schooltime.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    private val semesterRegex = Regex("^\\d{4}-(I|II)$")
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun filterNameInput(input: String): String {
        return input.filter { it.isLetter() || it.isWhitespace() }
    }

    fun formatSemesterInput(currentText: String, newText: String): String {
        if (newText.length < currentText.length) {
            return if (currentText.endsWith("-") && !newText.endsWith("-")) {
                currentText.dropLast(1)
            } else newText
        }
        val cleaned = newText.uppercase(Locale.ROOT)
        val digits = cleaned.takeWhile { it.isDigit() }.take(4)
        if (digits.length == 4) {
            val suffix = cleaned.removePrefix(digits).removePrefix("-")
            return if (suffix.isNotEmpty()) "$digits-$suffix" else "$digits-"
        }
        return digits
    }

    fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.ROOT)

    fun emailError(email: String): String? {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return "El correo es requerido"
        if (!emailRegex.matches(trimmed)) return "Ingresa un correo institucional o válido"
        return null
    }

    fun passwordError(password: String, forLogin: Boolean = false): String? {
        if (password.isEmpty()) return "La contraseña es requerida"
        if (!forLogin && password.length < 6) return "Mínimo 6 caracteres"
        return null
    }

    fun confirmPasswordError(password: String, confirm: String): String? {
        if (confirm.isEmpty()) return "Confirma tu contraseña"
        if (password != confirm) return "Las contraseñas no coinciden"
        return null
    }

    fun requiredNameError(value: String, field: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "$field es requerido"
        if (trimmed.length < 2) return "$field muy corto"
        return null
    }

    fun phoneError(phone: String, expectedLength: Int = 9, dialCode: String = "+51"): String? {
        val digits = phone.filter { it.isDigit() }
        if (digits.isEmpty()) return "Número de teléfono requerido"
        if (digits.length != expectedLength) return "Debe tener $expectedLength dígitos ($dialCode)"
        return null
    }

    fun careerError(career: String): String? {
        if (career.trim().isEmpty() || career == "Selecciona tu carrera") return "Selecciona tu carrera profesional"
        return null
    }

    fun semesterError(semester: String): String? {
        val trimmed = semester.trim()
        if (trimmed.isEmpty()) return "Ciclo/Semestre requerido (ej. 2025-I)"
        if (!semesterRegex.matches(trimmed)) return "Formato inválido. Usa AAAA-I o AAAA-II (ej. 2025-I)"
        return null
    }

    fun parseDate(value: String): LocalDate? {
        return try {
            LocalDate.parse(value, dateFormatter)
        } catch (e: Exception) {
            null
        }
    }

    fun normalizeDate(value: String): String {
        return parseDate(value)?.format(dateFormatter) ?: value
    }

    fun startDateError(startDate: String): String? {
        val d = parseDate(startDate) ?: return "Fecha de inicio inválida (AAAA-MM-DD)"
        return null
    }

    fun endDateError(startDate: String, endDate: String): String? {
        val s = parseDate(startDate) ?: return null
        val e = parseDate(endDate) ?: return "Fecha de fin inválida (AAAA-MM-DD)"
        if (e.isBefore(s)) return "La fecha de fin debe ser posterior a la fecha de inicio"
        return null
    }

    fun mapFirebaseAuthError(error: Throwable, isRegister: Boolean = false): String {
        val msg = error.message?.lowercase(Locale.ROOT) ?: ""
        return when {
            msg.contains("email-already-in-use") || msg.contains("already exists") -> "Este correo ya está registrado. Inicia sesión."
            msg.contains("wrong-password") || msg.contains("invalid-credential") -> "Credenciales incorrectas. Verifica tu contraseña."
            msg.contains("user-not-found") -> "No existe cuenta con este correo. Regístrate."
            msg.contains("user-disabled") -> "Esta cuenta ha sido inhabilitada."
            msg.contains("too-many-requests") -> "Demasiados intentos fallidos. Intenta más tarde."
            msg.contains("network-request-failed") -> "Error de conexión a internet."
            else -> error.localizedMessage ?: "Ocurrió un error inesperado"
        }
    }
}
