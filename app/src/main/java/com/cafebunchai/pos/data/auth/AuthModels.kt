package com.cafebunchai.pos.data.auth

data class StaffSession(
    val uid: String,
    val email: String,
    val isAdmin: Boolean,
)

sealed class AuthState {
    data object Loading : AuthState()
    data object SignedOut : AuthState()
    data class SignedIn(val session: StaffSession) : AuthState()
}
