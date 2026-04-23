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

interface SetupDataSource {
    suspend fun getGames(): List<Game>
    suspend fun getTeamAssignments(): List<TeamAssignment>
    suspend fun createGame(name: String): Game
    suspend fun createTeamAssignment(baseTeamCounter: Long): TeamAssignment
}

class SupabaseSetupDataSource(
    private val repositories: TorturaSupabaseRepositories = TorturaSupabaseRepositories(),
) : SetupDataSource {

    override suspend fun getGames(): List<Game> =
        repositories.games.getAll().map { it.toModel() }

    override suspend fun getTeamAssignments(): List<TeamAssignment> =
        repositories.teamAssignments.getAll().map { it.toModel() }

    override suspend fun createGame(name: String): Game =
        repositories.games.create(Game(name = name).toInsertDto()).toModel()

    override suspend fun createTeamAssignment(baseTeamCounter: Long): TeamAssignment =
        repositories.teamAssignments
            .create(TeamAssignment(baseTeamCounter = baseTeamCounter).toInsertDto())
            .toModel()
}

class SetupViewModel(
    private val dataSource: SetupDataSource = SupabaseSetupDataSource(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState

    fun loadSetupData() {
        runRepositoryAction {
            val games = dataSource.getGames()
            val teamAssignments = dataSource.getTeamAssignments()

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
            val createdGame = dataSource.createGame(name)
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
            val createdAssignment = dataSource.createTeamAssignment(baseTeamCounter)
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
