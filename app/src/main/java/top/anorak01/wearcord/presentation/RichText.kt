package top.anorak01.wearcord.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.ImageDecoderDecoder

@Composable
public fun RichText(content: String) {
    // parse the message content, namely the emotes
    // https://cdn.discordapp.com/emojis/{emote_id}.{ext} emote link

    val emoteRegex = "<(a?):([a-zA-Z0-9_]+):(\\d+)>".toRegex()

    val parts = mutableListOf<MessagePart>()

    var lastIndex = 0

    for (match in emoteRegex.findAll(content)) {
        val matchStart = match.range.first
        val matchEnd = match.range.last + 1

        // Add preceding text, if any
        if (matchStart > lastIndex) {
            parts.add(MessagePart.Text(content.substring(lastIndex, matchStart)))
        }

        val animated = match.groupValues[1] == "a"
        val emoteId = match.groupValues[3]

        parts.add(MessagePart.Emote(id = emoteId, animated = animated))

        lastIndex = matchEnd
    }

    // Add trailing text, if any
    if (lastIndex < content.length) {
        parts.add(MessagePart.Text(content.substring(lastIndex)))
    }

    val inlineContent = mutableMapOf<String, InlineTextContent>()

    val annotatedText = buildAnnotatedString {
        for (part in parts) {
            when (part) {
                is MessagePart.Text -> {
                    append(part.text)
                }
                is MessagePart.Emote -> {
                    inlineContent.put("<emoji:${part.id}>",
                        InlineTextContent(
                                placeholder = Placeholder(
                                    width = 20.sp,
                                    height = 20.sp,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                )
                            ) {
                                val imageLoader = ImageLoader.Builder(LocalContext.current) // here for GIF support
                                    .components {
                                        add(ImageDecoderDecoder.Factory())
                                    }
                                    .build()

                                Image(
                                    painter = rememberAsyncImagePainter("https://cdn.discordapp.com/emojis/${part.id}.${if (part.animated) "gif" else "png"}?size=64", imageLoader = imageLoader),
                                    contentDescription = "Emoji"
                                )
                            }
                        )

                    appendInlineContent("<emoji:${part.id}>")
                }
            }
        }


    }

    Text(
        text = annotatedText,
        inlineContent = inlineContent,
        color = Color.White,
        modifier = Modifier
            //.align(Alignment.CenterStart)
            .padding(start = 0.dp), // Explicitly no padding
        textAlign = TextAlign.Start,
        fontSize = TextUnit(14.0f, TextUnitType.Sp),
        )


        /*when (part) {
            is MessagePart.Text -> {
                Text(
                    text = part.text,
                    color = Color.White,
                    modifier = Modifier
                        //.align(Alignment.CenterStart)
                        .padding(start = 0.dp), // Explicitly no padding
                    textAlign = TextAlign.Start,
                    fontSize = TextUnit(14.0f, TextUnitType.Sp),
                )
            }

            is MessagePart.Emote -> {

            }
        }*/


}

sealed class MessagePart {
    data class Text(val text: String) : MessagePart()
    data class Emote(val id: String, val animated: Boolean) : MessagePart()
}