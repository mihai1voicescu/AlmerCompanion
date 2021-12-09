package io.almer.almercompanion.screen

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.almer.almercompanion.R
import io.almer.almercompanion.composable.background.AlmerLogoBackground
import io.almer.almercompanion.screen.main.*

@Composable
@Preview
fun MainScreen() {
    val homeScreenState = rememberSaveable(
        stateSaver = Saver(
            save = { value ->
                value.nameId
            },
            restore = { nameId ->
                // todo this might crash the app if the id of the string changes
                MainScreenType.screens.firstOrNull {
                    it.nameId == nameId
                }
            }
        )
    ) { mutableStateOf(MainScreenType.StartScreen) }

    AlmerLogoBackground {

        Scaffold(
            topBar = {
                TopAppBar(
                    { Text("Almer Companion") },

//                modifier: Modifier = Modifier,
                    navigationIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.symbol),
                            contentDescription = "Almer"
                        )
                    },
//                actions: @Composable RowScope.() -> Unit = {},
//                backgroundColor: Color = MaterialTheme.colors.primarySurface,
//                contentColor: Color = contentColorFor(backgroundColor),
                    elevation = 10.dp
                )
            },
            bottomBar = {
                BottomNavigationContent(
                    modifier = Modifier.align(Alignment.BottomCenter)
//                    .semantics { contentDescription = bottomNavBarContentDescription },
                    ,
                    homeScreenTypeState = homeScreenState
                )
            }
        ) {
            homeScreenState.value.Screen()
        }
    }
}

val NavController.pathToMainScreen get() = "main"


@Composable
fun BottomNavigationContent(
    modifier: Modifier = Modifier,
    homeScreenTypeState: MutableState<MainScreenType>
) {

    var animateState = remember { mutableStateOf(false) }

    NavigationBar(
        modifier = modifier,
    ) {
        NavBarScope(this, homeScreenTypeState, animateState).apply {
            MainScreenType.screens.map {
                HomeScreenItem(data = it)
            }
        }
    }
}

private class NavBarScope(
    rowScope: RowScope,
    val homeScreenTypeState: MutableState<MainScreenType>,
    animateState: MutableState<Boolean>
) : RowScope by rowScope {
    var animate by animateState
}


@Composable
private fun NavBarScope.HomeScreenItem(
    data: MainScreenType
) {
    NavigationBarItem(
        icon = {
            Icon(
                painter = data.icon(),
                contentDescription = data.name()
            )
        },
        selected = homeScreenTypeState.value == data,
        onClick = {
            homeScreenTypeState.value = data
            animate = false
        },
        label = { Text(data.name()) },
    )
}
