package mok.it.logikazamatak.model

enum class SolutionState {
    CORRECT,
    INCORRECT,
    EMPTY,
    ;

    fun toBoolean(): Boolean = this == CORRECT
}
