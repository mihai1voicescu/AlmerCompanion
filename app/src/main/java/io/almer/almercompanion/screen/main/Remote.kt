//package io.almer.almercompanion.screen.main
//
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.tooling.preview.Preview
//import io.almer.almercompanion.R
//
//object Remote : MainScreenType(R.drawable.ic_round_keyboard_24, R.string.navigation_item_home) {
//    @Composable
//    override fun Screen() {
//        Box(Modifier.fillMaxSize()) {
//            Image(
//                painter = painterResource(id = R.drawable.ic_round_arrow_circle_up_24),
//                alignment = Alignment.TopCenter,
//                contentDescription = "Up",
//            )
//            Image(
//                painter = painterResource(id = R.drawable.ic_round_arrow_circle_up_24),
//                alignment = Alignment.BottomCenter,
//                contentDescription = "Down",
//            )
//        }
//    }
//}
//
//@Composable
//@Preview
//private fun ScreenPreview() {
//    Remote.Screen()
//}
