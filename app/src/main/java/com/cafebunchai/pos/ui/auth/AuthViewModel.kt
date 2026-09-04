package com.cafebunchai.pos.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cafebunchai.pos.data.auth.AuthRepository
import com.cafebunchai.pos.data.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val busy: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(
    private val auth: AuthRepository,
) : ViewModel() {

    val authState: StateFlow<AuthState> = auth.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    private val _form = MutableStateFlow(LoginFormState())
    val form: StateFlow<LoginFormState> = _form

    fun setEmail(value: String) = _form.update { it.copy(email = value, error = null) }
    fun setPassword(value: String) = _form.update { it.copy(password = value, error = null) }

    fun signIn() {
        val f = _form.value
        if (f.email.isBlank() || f.password.isBlank()) {
            _form.update { it.copy(error = "Enter email and password") }
            return
        }
        viewModelScope.launch {
            _form.update { it.copy(busy = true, error = null) }
            runCatching { auth.signIn(f.email, f.password) }
                .onFailure { e ->
                    _form.update { it.copy(busy = false, error = e.message ?: "Login failed") }
                }
                .onSuccess {
                    _form.update { it.copy(busy = false, password = "") }
                }
        }
    }

    fun signOut() = auth.signOut()

    companion object {
        fun factory(auth: AuthRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(auth) as T
        }
    }
}
