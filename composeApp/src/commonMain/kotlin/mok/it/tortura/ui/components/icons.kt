
package mok.it.tortura.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun CorrectIcon(
    selected: Boolean,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = modifier
                .background(color = Color.Green, shape = RoundedCornerShape(iconSize / 2))
                .size(iconSize),
        )
    } else {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = Color.Green,
            modifier = modifier.size(iconSize),
        )
    }
}

@Composable
fun IncorrectIcon(
    selected: Boolean,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Icon(
            Icons.Filled.Close,
            contentDescription = null,
            tint = Color.White,
            modifier = modifier
                .background(color = Color.Red, shape = RoundedCornerShape(iconSize / 2))
                .size(iconSize),
        )
    } else {
        Icon(
            Icons.Filled.Close,
            contentDescription = null,
            tint = Color.Red,
            modifier = modifier.size(iconSize),
        )
    }
}

@Composable
fun DeleteIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.Filled.DeleteForever,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun NavigateBackIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun NavigateForwardIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun HelpIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.AutoMirrored.Filled.Help,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun AddIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.Filled.Add,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun SaveIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.Filled.Save,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun RefreshIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.Filled.Refresh,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun ChangeLocationIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.Filled.MyLocation,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun ShopIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.Filled.ShoppingCart,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun SetupIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.Filled.Construction,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun HealerIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.Filled.MedicalServices,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun GamesIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.Filled.SportsEsports,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun LoginIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.AutoMirrored.Filled.Login,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun LogoutIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.AutoMirrored.Filled.Logout,
        contentDescription = null,
        modifier = modifier,
    )
}
