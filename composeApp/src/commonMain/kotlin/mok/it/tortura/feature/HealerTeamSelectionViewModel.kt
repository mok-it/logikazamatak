package mok.it.tortura.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mok.it.tortura.data.supabase.mapper.toModel
import mok.it.tortura.data.supabase.repository.TorturaSupabaseRepositories
import mok.it.tortura.model.Team

data class HealerTeamSelectionUiState(
    val isLoading: Boolean = false,
    val teams: List<Team> = emptyList(),
    val message: String? = null,
    val errorMessage: String? = null,
)

interface HealerTeamSelectionDataSource {
    suspend fun getTeams(gameId: Long): List<Team>
}

class SupabaseHealerTeamSelectionDataSource(
    private val repositories: TorturaSupabaseRepositories = TorturaSupabaseRepositories(),
) : HealerTeamSelectionDataSource {

    override suspend fun getTeams(gameId: Long): List<Team> {
        val assignments = repositories.teamAssignments.getByGameId(gameId)
        return assignments
            .mapNotNull { it.id }
            .flatMap { repositories.teams.getByTeamAssignmentId(it) }
            .map { it.toModel() }
            .sortedBy { it.id ?: Long.MAX_VALUE }
    }
}

class HealerTeamSelectionViewModel(
    private val activeGameId: Long,
    private val dataSource: HealerTeamSelectionDataSource = SupabaseHealerTeamSelectionDataSource(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealerTeamSelectionUiState())
    val uiState: StateFlow<HealerTeamSelectionUiState> = _uiState

    fun loadTeams() {
        runRepositoryAction {
            val teams = dataSource.getTeams(activeGameId)
            _uiState.update {
                it.copy(
                    teams = teams,
                    message = if (teams.isEmpty()) null else "Csapatok betöltve",
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
