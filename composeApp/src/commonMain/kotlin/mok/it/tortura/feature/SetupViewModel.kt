package mok.it.tortura.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mok.it.tortura.data.supabase.mapper.toInsertDto
import mok.it.tortura.data.supabase.mapper.toModel
import mok.it.tortura.data.supabase.repository.TorturaSupabaseRepositories
import mok.it.tortura.model.Game
import mok.it.tortura.model.TeamAssignment

data class SetupUiState(
    val isLoading: Boolean = false,
    val gameName: String = "",
    val baseTeamCounter: String = "",
    val games: List<Game> = emptyList(),
    val teamAssignments: List<TeamAssignment> = emptyList(),
    val message: String? = null,
    val errorMessage: String? = null,
)

class SetupViewModel(
    private val repositories: TorturaSupabaseRepositories = TorturaSupabaseRepositories(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState

    fun loadSetupData() {
        runRepositoryAction {
            val games = repositories.games.getAll().map { it.toModel() }
            val teamAssignments = repositories.teamAssignments.getAll().map { it.toModel() }

            _uiState.update {
                it.copy(
                    games = games,
                    teamAssignments = teamAssignments,
                    message = "Adatok betöltve",
                )
            }
        }
    }

    fun onGameNameChange(value: String) {
        _uiState.update { it.copy(gameName = value, message = null, errorMessage = null) }
    }

    fun onBaseTeamCounterChange(value: String) {
        _uiState.update { it.copy(baseTeamCounter = value, message = null, errorMessage = null) }
    }

    fun createGame() {
        val name = uiState.value.gameName.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Adj nevet a feladatsornak") }
            return
        }

        runRepositoryAction {
            val createdGame = repositories.games.create(Game(name = name).toInsertDto()).toModel()
            _uiState.update {
                it.copy(
                    gameName = "",
                    games = it.games + createdGame,
                    message = "Feladatsor létrehozva",
                )
            }
        }
    }

    fun createTeamAssignment() {
        val baseTeamCounter = uiState.value.baseTeamCounter.trim().toLongOrNull()
        if (baseTeamCounter == null) {
            _uiState.update { it.copy(errorMessage = "A csapatszám legyen egész szám") }
            return
        }

        runRepositoryAction {
            val createdAssignment = repositories.teamAssignments
                .create(TeamAssignment(baseTeamCounter = baseTeamCounter).toInsertDto())
                .toModel()
            _uiState.update {
                it.copy(
                    baseTeamCounter = "",
                    teamAssignments = it.teamAssignments + createdAssignment,
                    message = "Csapatbeosztás létrehozva",
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    private fun runRepositoryAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, errorMessage = null) }
            try {
                action()
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: "Supabase művelet sikertelen")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
