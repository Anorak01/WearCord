package top.anorak01.wearcord.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.ImageDecoderDecoder

private val emoteRegex = "<(a?):([a-zA-Z0-9_]+):(\\d+)>".toRegex()

@Composable
fun RichText(content: String) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(ImageDecoderDecoder.Factory()) }
            .build()
    }
    val parts = remember(content) {
        buildList {
            var lastIndex = 0
            for (match in emoteRegex.findAll(content)) {
                val matchStart = match.range.first
                if (matchStart > lastIndex) {
                    add(MessagePart.Text(content.substring(lastIndex, matchStart)))
                }
                add(
                    MessagePart.Emote(
                        id = match.groupValues[3],
                        name = match.groupValues[2],
                        animated = match.groupValues[1] == "a",
                    ),
                )
                lastIndex = match.range.last + 1
            }
            if (lastIndex < content.length) add(MessagePart.Text(content.substring(lastIndex)))
        }
    }
    val inlineContent = mutableMapOf<String, InlineTextContent>()
    val annotatedText = buildAnnotatedString {
        parts.forEach { part ->
            when (part) {
                is MessagePart.Text -> append(part.text)
                is MessagePart.Emote -> {
                    val key = "<emoji:${part.id}>"
                    inlineContent[key] = InlineTextContent(
                        placeholder = Placeholder(
                            width = 20.sp,
                            height = 20.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        ),
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = "https://cdn.discordapp.com/emojis/${part.id}.${if (part.animated) "gif" else "png"}?size=64",
                                imageLoader = imageLoader,
                            ),
                            contentDescription = ":${part.name}:",
                        )
                    }
                    appendInlineContent(key)
                }
            }
        }
    }

    Text(
        text = annotatedText,
        inlineContent = inlineContent,
        color = Color.Unspecified,
        style = MaterialTheme.typography.body1,
    )
}

private sealed class MessagePart {
    data class Text(val text: String) : MessagePart()
    data class Emote(val id: String, val name: String, val animated: Boolean) : MessagePart()
}
