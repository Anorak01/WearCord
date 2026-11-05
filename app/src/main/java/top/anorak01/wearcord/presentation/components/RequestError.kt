package top.anorak01.wearcord.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import top.anorak01.wearcord.presentation.network.ApiResult

@Composable
fun RequestError(
    failure: ApiResult.Failure,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier) {
        Text(
            text = "Couldn’t load Discord",
            style = MaterialTheme.typography.title3,
            textAlign = TextAlign.Center,
        )
        Text(
            text = failure.message,
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurfaceVariant,
        )
        ActionChip(
            label = "Try again",
            icon = Icons.Default.Refresh,
            onClick = onRetry,
        )
    }
}
