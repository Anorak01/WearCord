package top.anorak01.wearcord.presentation.screens.login

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import top.anorak01.wearcord.presentation.components.ActionChip
import top.anorak01.wearcord.presentation.components.ScreenHeader
import top.anorak01.wearcord.presentation.components.SurfaceCard
import top.anorak01.wearcord.presentation.components.WearListScaffold

@Composable
fun LoginOptionsScreen(navController: NavHostController) {
    val listState = rememberScalingLazyListState()
    WearListScaffold(listState = listState) {
        item { ScreenHeader("Sign in") }
        item {
            SurfaceCard {
                Text("QR code is easiest", style = MaterialTheme.typography.title3)
                Text(
                    "Scan with Discord on your phone to avoid typing on the watch.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
        }
        item {
            ActionChip(
                label = "Scan QR code",
                secondaryLabel = "Approve on your phone",
                icon = Icons.Default.Person,
                onClick = { navController.navigate("qr") },
            )
        }
        item {
            ActionChip(
                label = "Email and password",
                icon = Icons.Default.Person,
                onClick = { navController.navigate("login") },
            )
        }
        item {
            ActionChip(
                label = "Use an account token",
                secondaryLabel = "Advanced option",
                icon = Icons.Default.Lock,
                onClick = { navController.navigate("manualtoken") },
            )
        }
    }
}
