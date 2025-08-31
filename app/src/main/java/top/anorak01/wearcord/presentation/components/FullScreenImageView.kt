package top.anorak01.wearcord.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import kotlin.math.roundToInt

@Composable
fun FullScreenImageView(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current.density


    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Fill screen
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)) // Semi-transparent background
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ -> // rotation gesture ignored
                        val oldScale = scale
                        scale = (scale * zoom).coerceIn(1f, 5f) // Clamp scale between 1x and 5x

                        // Calculate the offset adjustment to keep the zoom centered
                        offsetX += (centroid.x - size.width / 2) * (scale - oldScale) / oldScale
                        offsetY += (centroid.y - size.height / 2) * (scale - oldScale) / oldScale

                        // Apply pan considering the current scale
                        offsetX += pan.x * scale
                        offsetY += pan.y * scale
                    }
                }
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = imageUrl),
                contentDescription = "Full screen image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        // Center the transformation origin
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                    )
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close full screen image view",
                    tint = Color.White
                )
            }
        }
    }
}
