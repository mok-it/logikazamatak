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
import mok.it.tortura.model.TeamAssignment

data class SetupUiState(
    val isLoading: Boolean = false,
    val baseTeamCounter: String = "",
    val teamAssignments: List<TeamAssignment> = emptyList(),
    val message: String? = null,
    val errorMessage: String? = null,
)

interface SetupDataSource {
    suspend fun getTeamAssignments(gameId: Long): List<TeamAssignment>
    suspend fun createTeamAssignment(
        gameId: Long,
        baseTeamCounter: Long,
    ): TeamAssignment
}

class SupabaseSetupDataSource(
    private val repositories: TorturaSupabaseRepositories = TorturaSupabaseRepositories(),
) : SetupDataSource {

    override suspend fun getTeamAssignments(gameId: Long): List<TeamAssignment> =
        repositories.teamAssignments.getByGameId(gameId).map { it.toModel() }

    override suspend fun createTeamAssignment(
        gameId: Long,
        baseTeamCounter: Long,
    ): TeamAssignment = repositories.teamAssignments
        .create(
            TeamAssignment(
                baseTeamCounter = baseTeamCounter,
                gameId = gameId,
            ).toInsertDto(),
        )
        .toModel()
}

class SetupViewModel(
    private val activeGameId: Long,
    private val dataSource: SetupDataSource = SupabaseSetupDataSource(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState

    fun loadSetupData() {
        runRepositoryAction {
            val teamAssignments = dataSource.getTeamAssignments(activeGameId)

            _uiState.update {
                it.copy(
                    teamAssignments = teamAssignments,
                    message = "Adatok betöltve",
                )
            }
        }
    }

    fun onBaseTeamCounterChange(value: String) {
        _uiState.update { it.copy(baseTeamCounter = value, message = null, errorMessage = null) }
    }

    fun createTeamAssignment() {
        val baseTeamCounter = uiState.value.baseTeamCounter.trim().toLongOrNull()
        if (baseTeamCounter == null) {
            _uiState.update { it.copy(errorMessage = "A csapatszám legyen egész szám") }
            return
        }

        runRepositoryAction {
            val createdAssignment = dataSource.createTeamAssignment(activeGameId, baseTeamCounter)
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
