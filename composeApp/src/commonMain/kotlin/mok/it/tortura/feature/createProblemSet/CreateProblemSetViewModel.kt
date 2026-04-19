package mok.it.tortura.feature.createProblemSet

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import mok.it.tortura.loadProblemSetFromExcel
import mok.it.tortura.model.Location
import mok.it.tortura.model.ProblemSet
import mok.it.tortura.model.Task

class CreateProblemSetViewModel : ViewModel() {
    val problemSet = mutableStateOf(ProblemSet(listOf()))
    val popup = mutableStateOf(CreateProblemSetPopupType.NONE)

    private fun modifyAllBlocks(newValue: List<Location>) {
        problemSet.value = problemSet.value.copy(blocks = newValue)
    }

    private fun modifySingleBlock(
        block: Location,
        newValue: Location,
    ) {
        val blockIndex = problemSet.value.blocks.indexOf(block)
        if (blockIndex == -1) {
            return
        }
        val newBlocks = problemSet.value.blocks.toMutableList()
        newBlocks[blockIndex] = newValue.copy(minCorrectToProgress = (newValue.tasks.size + 2) / 2)
        modifyAllBlocks(newBlocks)
    }

    private fun modifyTask(
        block: Location,
        task: Task,
        newValue: Task,
    ) {
        val taskIndex = block.tasks.indexOf(task)
        if (taskIndex == -1) {
            return
        }
        val newTasks = block.tasks.toMutableList()
        newTasks[taskIndex] = newValue
        modifySingleBlock(block, block.copy(tasks = newTasks))
    }

    fun onEvent(event: CreateProblemSetEvent) {
        when (event) {
            is CreateProblemSetEvent.AddTask -> {
                modifySingleBlock(event.block, event.block.copy(tasks = event.block.tasks + Task()))
            }

            is CreateProblemSetEvent.AddBlock -> {
                modifyAllBlocks(problemSet.value.blocks + Location())
            }

            is CreateProblemSetEvent.DeleteTask -> {
                modifySingleBlock(
                    event.block,
                    event.block.copy(
                        tasks = event.block.tasks.filter {
                            it !=
                                event.task
                        },
                    ),
                )
            }

            is CreateProblemSetEvent.DeleteBlock -> {
                modifyAllBlocks(problemSet.value.blocks.filter { it != event.block })
            }

            is CreateProblemSetEvent.ChangeTaskText -> {
                modifyTask(event.block, event.task, event.task.copy(text = event.text))
            }

            is CreateProblemSetEvent.ChangeTaskSolution -> {
                modifyTask(event.block, event.task, event.task.copy(solution = event.text))
            }

            is CreateProblemSetEvent.ImportProblemSetFromJson -> {
                viewModelScope.launch {
                    try {
                        val newProblemSet = Json.decodeFromString<ProblemSet>(
                            event.file.readString(),
                        )
                        problemSet.value = newProblemSet
                    } catch (_: SerializationException) {
                        popup.value = CreateProblemSetPopupType.PARSE_ERROR
                    } catch (_: IllegalArgumentException) {
                        popup.value = CreateProblemSetPopupType.TYPE_ERROR
                    }
                }
            }

            is CreateProblemSetEvent.ImportProblemSetFromExcel -> {
                val newProblemSet = loadProblemSetFromExcel(event.file)
                if (newProblemSet == null) {
                    popup.value = CreateProblemSetPopupType.EXCEL_ERROR
                } else {
                    problemSet.value = newProblemSet
                }
            }

            is CreateProblemSetEvent.DismissPopup -> {
                popup.value = CreateProblemSetPopupType.NONE
            }

            is CreateProblemSetEvent.ShowHelp -> {
                popup.value = CreateProblemSetPopupType.HELP
            }
        }
    }
}

sealed class CreateProblemSetEvent {
    data object AddBlock : CreateProblemSetEvent()
    data class AddTask(val block: Location) : CreateProblemSetEvent()
    data class DeleteBlock(val block: Location) : CreateProblemSetEvent()
    data class DeleteTask(val task: Task, val block: Location) : CreateProblemSetEvent()
    data class ChangeTaskText(
        val block: Location,
        val task: Task,
        val text: String,
    ) : CreateProblemSetEvent()
    data class ChangeTaskSolution(
        val block: Location,
        val task: Task,
        val text: String,
    ) : CreateProblemSetEvent()
    data class ImportProblemSetFromJson(val file: PlatformFile) : CreateProblemSetEvent()
    data class ImportProblemSetFromExcel(val file: PlatformFile) : CreateProblemSetEvent()
    data object DismissPopup : CreateProblemSetEvent()
    data object ShowHelp : CreateProblemSetEvent()
}

enum class CreateProblemSetPopupType {
    PARSE_ERROR,
    TYPE_ERROR,
    EXCEL_ERROR,
    HELP,
    NONE,
}
