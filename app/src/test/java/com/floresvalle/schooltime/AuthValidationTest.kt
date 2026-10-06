package com.floresvalle.schooltime

import com.floresvalle.schooltime.util.AuthValidation
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test
    fun testEmailValidation() {
        assertNull(AuthValidation.emailError("estudiante@uni.edu.pe"))
        assertNull(AuthValidation.emailError("harol.flores@gmail.com"))
        assertNull(AuthValidation.emailError("usuario@hotmail.com"))
        assertNull(AuthValidation.emailError("contacto@empresa.org"))

        assertNotNull(AuthValidation.emailError(""))
        assertNotNull(AuthValidation.emailError("correo_invalido"))
        assertNotNull(AuthValidation.emailError("correo con espacios@gmail.com"))
        assertNotNull(AuthValidation.emailError("@gmail.com"))
        assertNotNull(AuthValidation.emailError("sin_arroba.com"))
        assertNotNull(AuthValidation.emailError("usuario@gmail")) // Missing dot and extension
        assertNotNull(AuthValidation.emailError("usuario@hotmail.")) // Ends with dot
        assertNotNull(AuthValidation.emailError("usuario@gmail.c")) // Extension too short
    }

    @Test
    fun testPasswordValidation() {
        // For login, any non-blank password is valid
        assertNull(AuthValidation.passwordError("123456", forLogin = true))
        assertNotNull(AuthValidation.passwordError("", forLogin = true))

        // For registration: >= 8 characters with letters and numbers/symbols
        assertNull(AuthValidation.passwordError("Password123!", forLogin = false))
        assertNull(AuthValidation.passwordError("claveSegura2026", forLogin = false))

        assertNotNull(AuthValidation.passwordError("corta", forLogin = false))
        assertNotNull(AuthValidation.passwordError("12345678", forLogin = false)) // Only digits
        assertNotNull(AuthValidation.passwordError("sololetras", forLogin = false)) // Only letters
    }

    @Test
    fun testNameValidation() {
        assertNull(AuthValidation.requiredNameError("Juan Carlos", "nombres"))
        assertNotNull(AuthValidation.requiredNameError("", "nombres"))
        assertNotNull(AuthValidation.requiredNameError("J", "nombres"))
        assertNotNull(AuthValidation.requiredNameError("Juan2", "nombres"))

        // Test filterNameInput blocks all numbers and special characters
        org.junit.Assert.assertEquals("Juan Carlos", AuthValidation.filterNameInput("Juan123 Carlos45!"))
        org.junit.Assert.assertEquals("María José", AuthValidation.filterNameInput("María99 José#"))
    }

    @Test
    fun testPhoneValidation() {
        assertNull(AuthValidation.phoneError("987654321"))
        assertNull(AuthValidation.phoneError("+51 987654321"))
        assertNotNull(AuthValidation.phoneError("123456789"))
        assertNotNull(AuthValidation.phoneError("98765432"))

        // International country test
        assertNull(AuthValidation.phoneError("3001234567", expectedLength = 10, dialCode = "+57"))
        assertNotNull(AuthValidation.phoneError("300123", expectedLength = 10, dialCode = "+57"))
    }

    @Test
    fun testSemesterValidation() {
        assertNull(AuthValidation.semesterError("2026-I"))
        assertNull(AuthValidation.semesterError("2026-II"))

        assertNotNull(AuthValidation.semesterError("2026-III"))
        assertNotNull(AuthValidation.semesterError("Ciclo 1"))
        assertNotNull(AuthValidation.semesterError("2026-1"))
        assertNotNull(AuthValidation.semesterError(""))
    }

    @Test
    fun testSemesterFormatting() {
        org.junit.Assert.assertEquals("2026-", AuthValidation.formatSemesterInput("202", "2026"))
        org.junit.Assert.assertEquals("2026-I", AuthValidation.formatSemesterInput("2026-", "2026-i"))
        org.junit.Assert.assertEquals("2026-II", AuthValidation.formatSemesterInput("2026-I", "2026-ii"))
        org.junit.Assert.assertEquals("2026-II", AuthValidation.formatSemesterInput("2026-II", "2026-III"))
        org.junit.Assert.assertEquals("2026", AuthValidation.formatSemesterInput("2026-", "2026"))
    }

    @Test
    fun testDateValidation() {
        // ISO yyyy-MM-dd
        assertNull(AuthValidation.startDateError("2026-03-16"))
        assertNull(AuthValidation.endDateError("2026-03-16", "2026-07-20"))

        // Formats with slashes or Peruvian format dd/MM/yyyy
        assertNull(AuthValidation.startDateError("16/03/2026"))
        assertNull(AuthValidation.startDateError("16-03-2026"))
        assertNull(AuthValidation.startDateError("2026/03/16"))

        // End before start error
        assertNotNull(AuthValidation.endDateError("2026-07-20", "2026-03-16"))

        // End more than 1 year later error (e.g. jumping to 2028 from 2026)
        assertNotNull(AuthValidation.endDateError("2026-03-16", "2028-03-16"))

        // Blank or invalid
        assertNotNull(AuthValidation.startDateError(""))
        assertNotNull(AuthValidation.startDateError("fecha_invalida"))
    }
}
