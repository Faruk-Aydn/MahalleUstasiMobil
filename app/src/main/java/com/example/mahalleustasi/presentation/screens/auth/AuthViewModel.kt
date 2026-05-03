package com.example.mahalleustasi.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.User
import com.example.mahalleustasi.domain.usecase.auth.GetCurrentUserUseCase
import com.example.mahalleustasi.domain.usecase.auth.LoginUseCase
import com.example.mahalleustasi.domain.usecase.auth.LogoutUseCase
import com.example.mahalleustasi.domain.usecase.auth.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase:          LoginUseCase,
    private val registerUseCase:       RegisterUseCase,
    private val logoutUseCase:         LogoutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    // ── Auth State ──────────────────────────────────────────────────────────
    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState = _authState.asStateFlow()

    // ── Mevcut kullanıcı (Firebase Auth Flow) ──────────────────────────────
    val currentUser = getCurrentUserUseCase()
        .stateIn(
            scope         = viewModelScope,
            started       = SharingStarted.WhileSubscribed(5_000),
            initialValue  = null
        )

    // ── Giriş Yap ──────────────────────────────────────────────────────────
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            _authState.value = when (val result = loginUseCase(email, password)) {
                is Resource.Success -> AuthUiState.Success(result.data)
                is Resource.Error   -> AuthUiState.Error(result.message)
                is Resource.Loading -> AuthUiState.Loading
                is Resource.Idle    -> AuthUiState.Idle
            }
        }
    }

    // ── Kayıt Ol ───────────────────────────────────────────────────────────
    fun register(name: String, email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            _authState.value = when (
                val result = registerUseCase(name, email, password, confirmPassword)
            ) {
                is Resource.Success -> AuthUiState.Success(result.data)
                is Resource.Error   -> AuthUiState.Error(result.message)
                is Resource.Loading -> AuthUiState.Loading
                is Resource.Idle    -> AuthUiState.Idle
            }
        }
    }

    // ── Çıkış Yap ──────────────────────────────────────────────────────────
    fun logout() {
        viewModelScope.launch { logoutUseCase() }
    }

    // ── Hata temizle ───────────────────────────────────────────────────────
    fun clearState() {
        _authState.value = AuthUiState.Idle
    }
}

// ── UI State ─────────────────────────────────────────────────────────────────
sealed class AuthUiState {
    data object Idle    : AuthUiState()
    data object Loading : AuthUiState()
    data class  Success(val user: User) : AuthUiState()
    data class  Error(val message: String) : AuthUiState()
}
