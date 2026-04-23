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

data class GameSelectionUiState(
    val isLoading: Boolean = false,
    val gameName: String = "",
    val games: List<Game> = emptyList(),
    val selectedGame: Game? = null,
    val message: String? = null,
    val errorMessage: String? = null,
)

interface GameSelectionDataSource {
    suspend fun getGames(): List<Game>
    suspend fun createGame(name: String): Game
}

class SupabaseGameSelectionDataSource(
    private val repositories: TorturaSupabaseRepositories = TorturaSupabaseRepositories(),
) : GameSelectionDataSource {

    override suspend fun getGames(): List<Game> =
        repositories.games.getAll().map { it.toModel() }

    override suspend fun createGame(name: String): Game =
        repositories.games.create(Game(name = name).toInsertDto()).toModel()
}

class GameSelectionViewModel(
    private val dataSource: GameSelectionDataSource = SupabaseGameSelectionDataSource(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameSelectionUiState())
    val uiState: StateFlow<GameSelectionUiState> = _uiState

    fun loadGames() {
        runRepositoryAction {
            val games = dataSource.getGames()
            _uiState.update {
                it.copy(
                    games = games,
                    message = if (games.isEmpty()) null else "Játékok betöltve",
                )
            }
        }
    }

    fun onGameNameChange(value: String) {
        _uiState.update { it.copy(gameName = value, message = null, errorMessage = null) }
    }

    fun selectGame(game: Game) {
        if (game.id == null) {
            _uiState.update { it.copy(errorMessage = "A játék azonosítója hiányzik") }
            return
        }

        _uiState.update { it.copy(selectedGame = game, message = null, errorMessage = null) }
    }

    fun createGame() {
        val name = uiState.value.gameName.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Adj nevet a játéknak") }
            return
        }

        runRepositoryAction {
            val createdGame = dataSource.createGame(name)
            _uiState.update {
                it.copy(
                    gameName = "",
                    games = it.games + createdGame,
                    selectedGame = createdGame,
                    message = "Játék létrehozva",
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedGame = null) }
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
