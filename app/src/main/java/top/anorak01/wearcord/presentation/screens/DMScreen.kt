package top.anorak01.wearcord.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import coil.compose.rememberAsyncImagePainter
import top.anorak01.wearcord.presentation.Channel
import top.anorak01.wearcord.presentation.components.ActionChip
import top.anorak01.wearcord.presentation.components.EmptyState
import top.anorak01.wearcord.presentation.components.LoadingState
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.components.ScreenHeader
import top.anorak01.wearcord.presentation.components.WearListScaffold
import top.anorak01.wearcord.presentation.fetchers.fetchDms
import top.anorak01.wearcord.presentation.fetchers.getUserUrl
import top.anorak01.wearcord.presentation.network.ApiResult

@Composable
fun DMScreen(navController: NavHostController) {
    val dms = remember { mutableStateListOf<Channel>() }
    val listState = rememberScalingLazyListState()
    var isLoading by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<ApiResult.Failure?>(null) }
    var refreshCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshCount) {
        isLoading = true
        failure = null
        when (val result = fetchDms()) {
            is ApiResult.Success -> {
                dms.clear()
                dms.addAll(result.data)
            }
            is ApiResult.Failure -> failure = result
        }
        isLoading = false
    }

    WearListScaffold(listState = listState) {
        item { ScreenHeader("Direct messages") }
        if (isLoading && dms.isEmpty()) item { LoadingState("Loading conversations…") }
        failure?.let { error ->
            item(key = "request_error") {
                RequestError(failure = error, onRetry = { refreshCount++ })
            }
        }
        if (!isLoading && dms.isEmpty() && failure == null) {
            item { EmptyState("No conversations", "New direct messages will appear here.") }
        }
        items(dms, key = { it.id }) { channel ->
            val recipient = channel.recipients.firstOrNull()
            Chip(
                onClick = { navController.navigate("messages/${channel.id}") },
                modifier = Modifier.fillMaxWidth(),
                colors = ChipDefaults.primaryChipColors(),
                icon = {
                    if (!recipient?.avatar.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = getUserUrl(recipient!!.id, recipient.avatar!!),
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(ChipDefaults.LargeIconSize).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(channel.name?.take(1)?.uppercase() ?: "?")
                    }
                },
                label = { Text(channel.name ?: "Direct message", maxLines = 2) },
                secondaryLabel = { Text("Open conversation") },
            )
        }
        item {
            ActionChip(
                label = if (isLoading) "Refreshing…" else "Refresh conversations",
                icon = Icons.Default.Refresh,
                enabled = !isLoading,
                onClick = { refreshCount++ },
            )
        }
    }
}
