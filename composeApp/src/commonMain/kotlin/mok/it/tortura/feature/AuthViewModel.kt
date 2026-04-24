package mok.it.tortura.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mok.it.tortura.SupabaseClient

data class AuthUiState(
    val isInitializing: Boolean = true,
    val isBusy: Boolean = false,
    val isAuthenticated: Boolean = false,
    val email: String? = null,
    val errorMessage: String? = null,
)

class AuthViewModel : ViewModel() {

    private val auth = SupabaseClient.client.auth

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        viewModelScope.launch {
            auth.sessionStatus.collect { status ->
                _uiState.update {
                    when (status) {
                        is SessionStatus.Authenticated -> it.copy(
                            isInitializing = false,
                            isAuthenticated = true,
                            email = status.session.user?.email,
                            errorMessage = null,
                        )

                        SessionStatus.Initializing -> it.copy(
                            isInitializing = true,
                            isAuthenticated = false,
                        )

                        is SessionStatus.NotAuthenticated -> it.copy(
                            isInitializing = false,
                            isAuthenticated = false,
                            email = null,
                        )

                        is SessionStatus.RefreshFailure -> it.copy(
                            isInitializing = false,
                            isAuthenticated = false,
                            email = null,
                            errorMessage = "A bejelentkezés frissítése sikertelen",
                        )
                    }
                }
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            try {
                auth.signInWith(Google) {
                    scopes.add("email")
                    scopes.add("profile")
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: "Google bejelentkezés sikertelen")
                }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            try {
                auth.signOut()
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: "Kijelentkezés sikertelen")
                }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
