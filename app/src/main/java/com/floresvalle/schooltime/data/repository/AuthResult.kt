package com.floresvalle.schooltime.data.repository

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val canRetryOffline: Boolean = false, val isNetworkError: Boolean = false) : AuthResult<Nothing>()
}
