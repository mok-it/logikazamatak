package mok.it.tortura.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mok.it.tortura.data.supabase.mapper.toModel
import mok.it.tortura.data.supabase.repository.TorturaSupabaseRepositories
import mok.it.tortura.model.Game
import mok.it.tortura.model.Location

data class LocationSelectionUiState(
    val isLoading: Boolean = false,
    val game: Game? = null,
    val locations: List<Location> = emptyList(),
    val errorMessage: String? = null,
)

interface LocationSelectionDataSource {
    suspend fun getGame(gameId: Long): Game?
    suspend fun getLocations(gameId: Long): List<Location>
}

class SupabaseLocationSelectionDataSource(
    private val repositories: TorturaSupabaseRepositories = TorturaSupabaseRepositories(),
) : LocationSelectionDataSource {

    override suspend fun getGame(gameId: Long): Game? =
        repositories.games.getById(gameId)?.toModel()

    override suspend fun getLocations(gameId: Long): List<Location> =
        repositories.locations.getByGameId(gameId).map { it.toModel() }
}

class LocationSelectionViewModel(
    private val gameId: Long,
    private val dataSource: LocationSelectionDataSource = SupabaseLocationSelectionDataSource(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationSelectionUiState())
    val uiState: StateFlow<LocationSelectionUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val game = dataSource.getGame(gameId) ?: error("A játék nem található")
                val locations = dataSource.getLocations(gameId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        game = game,
                        locations = locations,
                        errorMessage = if (locations.isEmpty()) {
                            "Ehhez a játékhoz még nincs választható állomás"
                        } else {
                            null
                        },
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Supabase művelet sikertelen",
                    )
                }
            }
        }
    }
}
