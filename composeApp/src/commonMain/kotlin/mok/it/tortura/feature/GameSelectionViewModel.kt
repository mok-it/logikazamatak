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
import mok.it.tortura.model.Location
import mok.it.tortura.model.HealingTask
import mok.it.tortura.model.Item
import mok.it.tortura.model.ItemEffect
import mok.it.tortura.model.Location
import mok.it.tortura.model.Task

data class PendingGameJoin(
    val game: Game,
    val locations: List<Location>,
)

data class CreateLocationDraft(
    val localId: Long,
    val name: String = "",
)

data class CreateTaskDraft(
    val localId: Long,
    val locationLocalId: Long,
    val text: String = "",
    val solution: String = "",
    val isMiniBoss: Boolean = false,
)

data class CreateHealingTaskDraft(
    val localId: Long,
    val text: String = "",
    val solution: String = "",
)

data class CreateShopItemDraft(
    val localId: Long,
    val name: String = "",
    val price: String = "",
    val maxPerTeam: String = "",
    val itemEffectId: String = "",
)

data class CreateGameSetup(
    val name: String,
    val locations: List<CreateLocationDraft>,
    val tasks: List<CreateTaskDraft>,
    val healingTasks: List<CreateHealingTaskDraft>,
    val shopItems: List<CreateShopItemDraft>,
)

data class GameSelectionUiState(
    val isLoading: Boolean = false,
    val gameName: String = "",
    val draftLocations: List<CreateLocationDraft> = emptyList(),
    val draftTasks: List<CreateTaskDraft> = emptyList(),
    val draftHealingTasks: List<CreateHealingTaskDraft> = emptyList(),
    val draftShopItems: List<CreateShopItemDraft> = emptyList(),
    val itemEffects: List<ItemEffect> = emptyList(),
    val games: List<Game> = emptyList(),
    val selectedGame: Game? = null,
    val pendingGameJoin: PendingGameJoin? = null,
    val message: String? = null,
    val errorMessage: String? = null,
)

fun GameSelectionUiState.createGameValidationError(): String? {
    return when {
        gameName.trim().isEmpty() -> "Adj nevet a játéknak"
        draftLocations.any { it.name.trim().isEmpty() } -> "Minden helyszínnek legyen neve"
        draftTasks.any { it.text.trim().isEmpty() || it.solution.trim().isEmpty() } ->
            "Minden feladatnak legyen szövege és megoldása"
        draftHealingTasks.any { it.text.trim().isEmpty() || it.solution.trim().isEmpty() } ->
            "Minden gyógyító feladatnak legyen szövege és megoldása"
        draftShopItems.any { it.name.trim().isEmpty() } -> "Minden bolti tárgynak legyen neve"
        draftShopItems.any { it.price.trim().toIntOrNull()?.let { price -> price >= 0 } != true } ->
            "A bolti tárgy ára legyen nem negatív egész szám"
        draftShopItems.any { it.maxPerTeam.trim().toIntOrNull()?.let { max -> max > 0 } != true } ->
            "A csapatonkénti darabszám legyen pozitív egész szám"
        else -> null
    }
}

interface GameSelectionDataSource {
    suspend fun getGames(): List<Game>
    suspend fun getLocations(gameId: Long): List<Location>
    suspend fun getItemEffects(): List<ItemEffect>
    suspend fun createGame(setup: CreateGameSetup): Game
}

class SupabaseGameSelectionDataSource(
    private val repositories: TorturaSupabaseRepositories = TorturaSupabaseRepositories(),
) : GameSelectionDataSource {

    override suspend fun getGames(): List<Game> =
        repositories.games.getAll().map { it.toModel() }

    override suspend fun getLocations(gameId: Long): List<Location> =
        repositories.locations.getByGameId(gameId).map { it.toModel() }
    override suspend fun getItemEffects(): List<ItemEffect> =
        repositories.itemEffects.getAll().map { it.toModel() }

    override suspend fun createGame(setup: CreateGameSetup): Game {
        val createdGame = repositories.games.create(Game(name = setup.name).toInsertDto()).toModel()
        val gameId = createdGame.id ?: error("A játék azonosítója hiányzik")

        val locationIdsByLocalId = setup.locations.associate { draft ->
            val createdLocation = repositories.locations
                .create(Location(name = draft.name.trim(), gameId = gameId).toInsertDto())
                .toModel()
            draft.localId to (createdLocation.id ?: error("A helyszín azonosítója hiányzik"))
        }

        setup.tasks.forEach { draft ->
            repositories.tasks.create(
                Task(
                    text = draft.text.trim(),
                    solution = draft.solution.trim(),
                    isMiniBoss = draft.isMiniBoss,
                    gameId = gameId,
                    locationId = locationIdsByLocalId[draft.locationLocalId],
                ).toInsertDto(),
            )
        }

        setup.healingTasks.forEach { draft ->
            repositories.healingTasks.create(
                HealingTask(
                    text = draft.text.trim(),
                    solution = draft.solution.trim(),
                    gameId = gameId,
                ).toInsertDto(),
            )
        }

        setup.shopItems.forEach { draft ->
            repositories.items.create(
                Item(
                    name = draft.name.trim(),
                    price = draft.price.trim().toInt(),
                    itemEffectId = draft.itemEffectId.trim().toLongOrNull(),
                    gameId = gameId,
                    maxPerTeam = draft.maxPerTeam.trim().toInt(),
                ).toInsertDto(),
            )
        }

        return createdGame
    }
}

class GameSelectionViewModel(
    private val dataSource: GameSelectionDataSource = SupabaseGameSelectionDataSource(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameSelectionUiState())
    val uiState: StateFlow<GameSelectionUiState> = _uiState
    private var nextDraftId = 1L

    fun loadGames() {
        runRepositoryAction {
            val games = dataSource.getGames()
            val itemEffects = dataSource.getItemEffects()
            _uiState.update {
                it.copy(
                    games = games,
                    itemEffects = itemEffects,
                    message = if (games.isEmpty()) null else "Játékok betöltve",
                )
            }
        }
    }

    fun onGameNameChange(value: String) {
        _uiState.update { it.copy(gameName = value, message = null, errorMessage = null) }
    }

    fun addLocation() {
        _uiState.update {
            it.copy(
                draftLocations = it.draftLocations + CreateLocationDraft(localId = nextDraftId++),
                message = null,
                errorMessage = null,
            )
        }
    }

    fun updateLocationName(localId: Long, value: String) {
        _uiState.update {
            it.copy(
                draftLocations = it.draftLocations.map { draft ->
                    if (draft.localId == localId) draft.copy(name = value) else draft
                },
                message = null,
                errorMessage = null,
            )
        }
    }

    fun removeLocation(localId: Long) {
        _uiState.update {
            it.copy(
                draftLocations = it.draftLocations.filterNot { draft -> draft.localId == localId },
                draftTasks = it.draftTasks.filterNot { draft -> draft.locationLocalId == localId },
                message = null,
                errorMessage = null,
            )
        }
    }

    fun addTask(locationLocalId: Long) {
        _uiState.update {
            it.copy(
                draftTasks = it.draftTasks + CreateTaskDraft(
                    localId = nextDraftId++,
                    locationLocalId = locationLocalId,
                ),
                message = null,
                errorMessage = null,
            )
        }
    }

    fun updateTaskText(localId: Long, value: String) {
        updateTask(localId) { it.copy(text = value) }
    }

    fun updateTaskSolution(localId: Long, value: String) {
        updateTask(localId) { it.copy(solution = value) }
    }

    fun updateTaskMiniBoss(localId: Long, value: Boolean) {
        updateTask(localId) { it.copy(isMiniBoss = value) }
    }

    fun removeTask(localId: Long) {
        _uiState.update {
            it.copy(
                draftTasks = it.draftTasks.filterNot { draft -> draft.localId == localId },
                message = null,
                errorMessage = null,
            )
        }
    }

    fun addHealingTask() {
        _uiState.update {
            it.copy(
                draftHealingTasks = it.draftHealingTasks + CreateHealingTaskDraft(localId = nextDraftId++),
                message = null,
                errorMessage = null,
            )
        }
    }

    fun updateHealingTaskText(localId: Long, value: String) {
        updateHealingTask(localId) { it.copy(text = value) }
    }

    fun updateHealingTaskSolution(localId: Long, value: String) {
        updateHealingTask(localId) { it.copy(solution = value) }
    }

    fun removeHealingTask(localId: Long) {
        _uiState.update {
            it.copy(
                draftHealingTasks = it.draftHealingTasks.filterNot { draft -> draft.localId == localId },
                message = null,
                errorMessage = null,
            )
        }
    }

    fun addShopItem() {
        _uiState.update {
            it.copy(
                draftShopItems = it.draftShopItems + CreateShopItemDraft(localId = nextDraftId++),
                message = null,
                errorMessage = null,
            )
        }
    }

    fun updateShopItemName(localId: Long, value: String) {
        updateShopItem(localId) { it.copy(name = value) }
    }

    fun updateShopItemPrice(localId: Long, value: String) {
        updateShopItem(localId) { it.copy(price = value.filter(Char::isDigit)) }
    }

    fun updateShopItemMaxPerTeam(localId: Long, value: String) {
        updateShopItem(localId) { it.copy(maxPerTeam = value.filter(Char::isDigit)) }
    }

    fun updateShopItemEffectId(localId: Long, value: String) {
        updateShopItem(localId) { it.copy(itemEffectId = value) }
    }

    fun removeShopItem(localId: Long) {
        _uiState.update {
            it.copy(
                draftShopItems = it.draftShopItems.filterNot { draft -> draft.localId == localId },
                message = null,
                errorMessage = null,
            )
        }
    }

    fun selectGame(game: Game) {
        if (game.id == null) {
            _uiState.update { it.copy(errorMessage = "A játék azonosítója hiányzik") }
            return
        }

        runRepositoryAction {
            val locations = dataSource.getLocations(game.id)
            if (locations.isEmpty()) {
                _uiState.update {
                    it.copy(errorMessage = "Ehhez a játékhoz még nincs választható állomás")
                }
                return@runRepositoryAction
            }

            _uiState.update {
                it.copy(
                    pendingGameJoin = PendingGameJoin(
                        game = game,
                        locations = locations,
                    ),
                    message = null,
                    errorMessage = null,
                )
            }
        }
    }

    fun confirmGameJoin(location: Location) {
        val pendingJoin = uiState.value.pendingGameJoin
        if (pendingJoin == null) {
            _uiState.update { it.copy(errorMessage = "Nincs folyamatban lévő csatlakozás") }
            return
        }

        _uiState.update {
            it.copy(
                selectedGame = pendingJoin.game,
                pendingGameJoin = null,
                message = null,
                errorMessage = null,
            )
        }
    }

    fun createGame() {
        val setup = buildSetupOrShowError() ?: return

        runRepositoryAction {
            val createdGame = dataSource.createGame(setup)
            _uiState.update {
                it.copy(
                    gameName = "",
                    draftLocations = emptyList(),
                    draftTasks = emptyList(),
                    draftHealingTasks = emptyList(),
                    draftShopItems = emptyList(),
                    games = it.games + createdGame,
                    selectedGame = createdGame,
                    message = "Játék létrehozva",
                )
            }
        }
    }

    private fun buildSetupOrShowError(): CreateGameSetup? {
        val state = uiState.value
        val name = state.gameName.trim()
        val error = state.createGameValidationError()

        if (error != null) {
            _uiState.update { it.copy(errorMessage = error, message = null) }
            return null
        }

        val locationIds = state.draftLocations.map { it.localId }.toSet()
        if (state.draftTasks.any { it.locationLocalId !in locationIds }) {
            _uiState.update { it.copy(errorMessage = "A feladat helyszíne hiányzik", message = null) }
            return null
        }

        return CreateGameSetup(
            name = name,
            locations = state.draftLocations,
            tasks = state.draftTasks,
            healingTasks = state.draftHealingTasks,
            shopItems = state.draftShopItems,
        )
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedGame = null) }
    }

    fun clearPendingJoin() {
        _uiState.update { it.copy(pendingGameJoin = null) }
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

    private fun updateTask(localId: Long, transform: (CreateTaskDraft) -> CreateTaskDraft) {
        _uiState.update {
            it.copy(
                draftTasks = it.draftTasks.map { draft ->
                    if (draft.localId == localId) transform(draft) else draft
                },
                message = null,
                errorMessage = null,
            )
        }
    }

    private fun updateHealingTask(
        localId: Long,
        transform: (CreateHealingTaskDraft) -> CreateHealingTaskDraft,
    ) {
        _uiState.update {
            it.copy(
                draftHealingTasks = it.draftHealingTasks.map { draft ->
                    if (draft.localId == localId) transform(draft) else draft
                },
                message = null,
                errorMessage = null,
            )
        }
    }

    private fun updateShopItem(
        localId: Long,
        transform: (CreateShopItemDraft) -> CreateShopItemDraft,
    ) {
        _uiState.update {
            it.copy(
                draftShopItems = it.draftShopItems.map { draft ->
                    if (draft.localId == localId) transform(draft) else draft
                },
                message = null,
                errorMessage = null,
            )
        }
    }
}
