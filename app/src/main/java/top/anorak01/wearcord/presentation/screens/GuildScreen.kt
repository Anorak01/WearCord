package top.anorak01.wearcord.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.compose.rememberAsyncImagePainter
import top.anorak01.wearcord.presentation.cache.discordListCache
import top.anorak01.wearcord.presentation.components.ActionChip
import top.anorak01.wearcord.presentation.components.ContextActionDrawer
import top.anorak01.wearcord.presentation.components.EmptyState
import top.anorak01.wearcord.presentation.components.LoadingState
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.components.ScreenHeader
import top.anorak01.wearcord.presentation.components.WearListScaffold
import top.anorak01.wearcord.presentation.fetchers.getIconUrl
import top.anorak01.wearcord.presentation.network.ApiResult

@Composable
fun GuildScreen(navController: NavHostController) {
    val cache = discordListCache
    val guilds = cache.guilds
    val listState = rememberScalingLazyListState()
    var isRefreshing by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<ApiResult.Failure?>(null) }
    var refreshCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshCount) {
        isRefreshing = true
        failure = null
        failure = (cache.refreshGuilds() as? ApiResult.Failure)
        isRefreshing = false
    }

    ContextActionDrawer(
        title = "Server options",
        "Reload servers" to { refreshCount++ },
    ) {
        WearListScaffold(listState = listState) {
            item { ScreenHeader("Servers") }
            if (isRefreshing && guilds.isEmpty()) item { LoadingState("Loading servers…") }
            failure?.let { error ->
                item(key = "request_error") {
                    RequestError(failure = error, onRetry = { refreshCount++ })
                }
            }
            if (!isRefreshing && guilds.isEmpty() && failure == null) {
                item { EmptyState("No servers", "This account hasn’t joined any servers.") }
            }
            items(guilds, key = { it.id }) { guild ->
                Chip(
                    onClick = { navController.navigate("channels/${guild.id}") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.primaryChipColors(),
                    icon = {
                        Box(
                            modifier = Modifier.size(ChipDefaults.LargeIconSize).clip(CircleShape)
                                .background(MaterialTheme.colors.surface),
                            contentAlignment = Alignment.Center,
                        ) {
                            guild.icon?.let { icon ->
                                Image(
                                    painter = rememberAsyncImagePainter(model = getIconUrl(guild.id, icon)),
                                    contentDescription = null,
                                    modifier = Modifier.size(ChipDefaults.LargeIconSize),
                                    contentScale = ContentScale.Crop,
                                )
                            } ?: Text(guild.name.take(1).uppercase())
                        }
                    },
                    label = { Text(guild.name, maxLines = 2) },
                )
            }
            item {
                ActionChip(
                    label = if (isRefreshing) "Refreshing…" else "Refresh servers",
                    icon = Icons.Default.Refresh,
                    enabled = !isRefreshing,
                    onClick = { refreshCount++ },
                )
            }
        }
    }
}
