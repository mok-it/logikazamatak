package mok.it.tortura.ui

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import mok.it.tortura.util.ColorAsStringSerializer

@Serializable
data class CategoryColors(
    @Serializable(with = ColorAsStringSerializer::class)
    val textColor: Color,
    @Serializable(with = ColorAsStringSerializer::class)
    val backgroundColor: Color,
    val name: String = "Egyedi",
) {

    companion object {
        val BOCS = CategoryColors(
            backgroundColor = Color(171, 32, 17),
            textColor = Color.White,
            name = "Medvebocs",
        )
        val KIS = CategoryColors(
            backgroundColor = Color(207, 198, 35),
            textColor = Color.White,
            name = "Kismedve",
        )
        val NAGY = CategoryColors(
            backgroundColor = Color(12, 135, 47),
            textColor = Color.White,
            name = "Nagymedve",
        )
        val JEGES = CategoryColors(
            backgroundColor = Color(37, 195, 219),
            textColor = Color.White,
            name = "Jegesmedve",
        )
        val UNDIFINED = CategoryColors(
            backgroundColor = Color.Unspecified,
            textColor = Color.Unspecified,
            name = "Válasz színt!",
        )
    }
}
