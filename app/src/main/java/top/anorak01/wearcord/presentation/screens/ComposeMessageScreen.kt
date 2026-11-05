package top.anorak01.wearcord.presentation.screens

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import kotlinx.coroutines.launch
import top.anorak01.wearcord.presentation.components.ActionChip
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.components.ScreenHeader
import top.anorak01.wearcord.presentation.components.WearListScaffold
import top.anorak01.wearcord.presentation.components.WearTextField
import top.anorak01.wearcord.presentation.message.sendMessage
import top.anorak01.wearcord.presentation.network.ApiResult

@Composable
fun ComposeMessageScreen(navController: NavHostController, channelId: String) {
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<ApiResult.Failure?>(null) }

    fun send() {
        if (isSending || message.isBlank()) return
        scope.launch {
            isSending = true
            failure = null
            when (val result = sendMessage(channelId, message)) {
                is ApiResult.Success -> {
                    message = ""
                    navController.popBackStack()
                }
                is ApiResult.Failure -> failure = result
            }
            isSending = false
        }
    }

    WearListScaffold(listState = listState) {
        item { ScreenHeader("New message") }
        item {
            WearTextField(
                value = message,
                onValueChange = {
                    if (it.length <= 2000) {
                        message = it
                        failure = null
                    }
                },
                label = "Message",
                placeholder = "Type a message",
                enabled = !isSending,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(onSend = { send() }),
            )
        }
        failure?.let { error ->
            item { RequestError(failure = error, onRetry = ::send) }
        }
        item {
            ActionChip(
                label = if (isSending) "Sending…" else "Send message",
                secondaryLabel = if (message.length >= 1900) "${message.length}/2000" else null,
                icon = Icons.AutoMirrored.Filled.Send,
                enabled = message.isNotBlank() && !isSending,
                onClick = ::send,
            )
        }
    }
}
