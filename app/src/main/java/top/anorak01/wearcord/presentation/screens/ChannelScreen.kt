package top.anorak01.wearcord.presentation.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import top.anorak01.wearcord.presentation.ChannelGroup
import top.anorak01.wearcord.presentation.components.ActionChip
import top.anorak01.wearcord.presentation.components.EmptyState
import top.anorak01.wearcord.presentation.components.LoadingState
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.components.ScreenHeader
import top.anorak01.wearcord.presentation.components.WearListScaffold
import top.anorak01.wearcord.presentation.fetchers.fetchGuildChannels
import top.anorak01.wearcord.presentation.network.ApiResult

@Composable
fun ChannelScreen(navController: NavHostController, guildId: String) {
    val groups = remember { mutableStateListOf<ChannelGroup>() }
    val listState = rememberScalingLazyListState()
    var isLoading by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<ApiResult.Failure?>(null) }
    var refreshCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(guildId, refreshCount) {
        isLoading = true
        failure = null
        when (val result = fetchGuildChannels(guildId)) {
            is ApiResult.Success -> {
                groups.clear()
                groups.addAll(result.data)
            }
            is ApiResult.Failure -> failure = result
        }
        isLoading = false
    }

    WearListScaffold(listState = listState) {
        item { ScreenHeader("Channels") }
        if (isLoading && groups.isEmpty()) item { LoadingState("Loading channels…") }
        failure?.let { error ->
            item(key = "request_error") {
                RequestError(failure = error, onRetry = { refreshCount++ })
            }
        }
        if (!isLoading && groups.isEmpty() && failure == null) {
            item { EmptyState("No channels", "No channels are visible to this account.") }
        }
        groups.forEach { group ->
            group.name?.let { name ->
                item(key = "category_${group.id}") {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurfaceVariant,
                    )
                }
            }
            group.channels.forEach { channel ->
                item(key = channel.id) {
                    Chip(
                        onClick = { navController.navigate("messages/${channel.id}") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ChipDefaults.primaryChipColors(),
                        label = { Text(channel.name ?: "Channel", maxLines = 2) },
                        secondaryLabel = { Text("Open channel") },
                    )
                }
            }
        }
        item {
            ActionChip(
                label = if (isLoading) "Refreshing…" else "Refresh channels",
                icon = Icons.Default.Refresh,
                enabled = !isLoading,
                onClick = { refreshCount++ },
            )
        }
    }
}
