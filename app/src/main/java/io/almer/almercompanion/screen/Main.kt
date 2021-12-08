package io.almer.almercompanion.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

import io.almer.almercompanion.R
import io.almer.almercompanion.composable.background.AlmerLogoBackground

@Composable
@Preview
fun MainScreen() {
    val homeScreenState = rememberSaveable { mutableStateOf(BottomNavType.Home) }

    AlmerLogoBackground {
        BottomNavigationContent(
            modifier = Modifier.align(Alignment.BottomCenter)
//                    .semantics { contentDescription = bottomNavBarContentDescription },
            ,
            homeScreenState = homeScreenState
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {


    }

}

@Composable
fun BottomNavigationContent(
    modifier: Modifier = Modifier,
    homeScreenState: MutableState<BottomNavType>
) {

    var animateState = remember { mutableStateOf(false) }

    NavigationBar(
        modifier = modifier,
    ) {
        NavBarScope(this, homeScreenState, animateState).apply {
            HomeScreenItem(BottomNavType.Home)
            HomeScreenItem(BottomNavType.Info)
        }
    }
}

class NavBarScope(
    rowScope: RowScope,
    val homeScreenState: MutableState<BottomNavType>,
    animateState: MutableState<Boolean>
) : RowScope by rowScope {
    var animate by animateState
}

enum class BottomNavType(
    val iconId: Int,
    val nameId: Int
) {
    Home(R.drawable.ic_round_home_24, R.string.navigation_item_home),
    Info(R.drawable.ic_round_info_24, R.string.navigation_item_info)
}

@Composable
inline fun BottomNavType.icon() = painterResource(iconId)

@Composable
inline fun BottomNavType.name() = stringResource(nameId)

@Composable
private fun NavBarScope.HomeScreenItem(
    data: BottomNavType
) {
    NavigationBarItem(
        icon = {
            Icon(
                painter = data.icon(),
                contentDescription = data.name
            )
        },
        selected = homeScreenState.value == data,
        onClick = {
            homeScreenState.value = data
            animate = false
        },
        label = { Text(data.name) },
    )
}
