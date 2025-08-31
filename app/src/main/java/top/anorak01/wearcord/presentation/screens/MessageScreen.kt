package top.anorak01.wearcord.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.anorak01.wearcord.presentation.Message
import top.anorak01.wearcord.presentation.RichText
import top.anorak01.wearcord.presentation.components.FullScreenImageView
import top.anorak01.wearcord.presentation.fetchers.fetchMessages
import top.anorak01.wearcord.presentation.message.jumpToMessage
import top.anorak01.wearcord.presentation.message.sendMessage

@Composable
fun MessageScreen(navController: NavHostController, channelId: String) {
    val scope = rememberCoroutineScope()

    val messages = remember { mutableStateListOf<Message>() }

    val listState = rememberScalingLazyListState()

    val isAtTop by remember {
        derivedStateOf { listState.centerItemIndex >= messages.size - 1 }
    }

    var newMessageText by remember { mutableStateOf("") }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val result = fetchMessages(channelId = channelId)
        messages.clear()
        messages.addAll(result)
        println("fetched messages")
        listState.animateScrollToItem(0, 1)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            LaunchedEffect(isAtTop) {
                if (isAtTop && messages.isNotEmpty()) {
                    println("reached the top")
                    val result = fetchMessages(
                        channelId = channelId,
                        messagesBeforeId = messages[messages.size - 1].id
                    )
                    messages.addAll(result)
                    println("fetched more messages")
                    // can load more messages here
                }
            }

            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                reverseLayout = true, // To show newest message at bottom
                scalingParams = ScalingLazyColumnDefaults.scalingParams(
                    edgeScale = 0.5f,
                    edgeAlpha = 0.5f,
                    minTransitionArea = 0.4f
                ),
                contentPadding = PaddingValues(vertical = 70.dp)
            ) {
                items(messages) { message ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        println("long pressed")
                                    }
                                )
                            }
                            .padding(horizontal = 4.dp),
                        //.padding(vertical = 0.dp),  // Only vertical spacing, no horizontal padding
                        contentAlignment = Alignment.CenterStart,


                        ) {
                        Column {
                            Text(
                                text = message.author.username,
                                color = Color.Black,
                                modifier = Modifier
                                    //.align(Alignment.CenterStart)
                                    .padding(start = 0.dp), // Explicitly no padding
                                textAlign = TextAlign.Start,
                                fontSize = TextUnit(14.0f, TextUnitType.Sp),
                            )
                            /*Text(
                                text = message.content,
                                color = Color.White,
                                modifier = Modifier
                                    //.align(Alignment.CenterStart)
                                    .padding(start = 0.dp), // Explicitly no padding
                                textAlign = TextAlign.Start,
                                fontSize = TextUnit(14.0f, TextUnitType.Sp),
                            )*/
                            RichText(message.content)
                            if (message.attachments.isNotEmpty()) {
                                message.attachments.forEach { attachment ->
                                    // For now, let's assume all attachments are images
                                    // You might want to check attachment.contentType
                                    if (attachment.content_type.startsWith("image/")) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Image(
                                            painter = rememberAsyncImagePainter(
                                                model = attachment.url,
                                                // Optional: Add a placeholder and error drawable
                                                // placeholder = painterResource(id = R.drawable.placeholder),
                                                // error = painterResource(id = R.drawable.error_image)
                                            ),
                                            contentDescription = "Attachment: ${attachment.filename}",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 150.dp) // Max height for image
                                                .clip(RoundedCornerShape(CornerSize(6.dp)))
                                                .clickable {
                                                    fullScreenImageUrl = attachment.url
                                                }, // Open image in full screen
                                            contentScale = ContentScale.Fit // Or ContentScale.Crop
                                        )
                                        Text( // Optional: display filename
                                            text = attachment.filename,
                                            fontSize = TextUnit(10f, TextUnitType.Sp),
                                            color = Color.LightGray,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    } else {
                                        // Handle other attachment types (e.g., show filename or icon)
                                        Text(
                                            text = "Attachment: ${attachment.filename} (${attachment.content_type ?: "unknown type"})",
                                            color = Color.Cyan,
                                            fontSize = TextUnit(10f, TextUnitType.Sp),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (message.type == 19) {
                                Text(
                                    text = "Reply: Jump to message",
                                    color = Color.Black,
                                    modifier = Modifier
                                        //.align(Alignment.CenterStart)
                                        .clickable {
                                            scope.launch {
                                                if (message.message_reference != null) {
                                                    jumpToMessage(
                                                        channelId,
                                                        message.message_reference.message_id,
                                                        listState,
                                                        messages
                                                    )
                                                }
                                            }
                                        }
                                        .padding(start = 0.dp), // Explicitly no padding
                                    textAlign = TextAlign.Start,
                                    fontSize = TextUnit(8.0f, TextUnitType.Sp),
                                )
                                // add actual reply handling
                                // 1. check if message is already in message list, if yes jump to it and return
                                // 2. check if message really exists /channels/<id>/messages/<id>, if no, return with error
                                // 3. fetch next 100 messages, check if id in them
                                // 4. add to message list
                                // 5. if message in newly fetched, jump to it and return, else jump to 3.
                            } else if (message.type != 0 || message.flags != 0) {
                                Text(
                                    text = "Part of this message is not supported",
                                    color = Color.Red,
                                    modifier = Modifier
                                        //.align(Alignment.CenterStart)
                                        .padding(start = 0.dp), // Explicitly no padding
                                    textAlign = TextAlign.Start,
                                    fontSize = TextUnit(8.0f, TextUnitType.Sp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize()) {
        Row {
            var reloadEnabled by remember { mutableStateOf(true) }
            Button(
                onClick = {
                    println("clicked")
                    scope.launch {
                        reloadEnabled = false
                        val result = fetchMessages(channelId = channelId)
                        messages.clear()
                        messages.addAll(result)
                        println("fetched messages")
                        reloadEnabled = true
                    }

                },
                content = { Text("R") },
                modifier = Modifier.fillMaxSize(0.15f),
                enabled = reloadEnabled
            )


            Button(
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0, 1)
                    }

                },
                content = { Text("D") },
                modifier = Modifier.fillMaxSize(0.15f)

            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
// Input field and Send Button - Bottom
        var buttonEnabled by remember { mutableStateOf(true) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.background) // Add a background to overlay content
                .padding(horizontal = 16.dp, vertical = 3.dp)
        ) {
            TextField( // Using OutlinedTextField for better visibility on Wear
                value = newMessageText,
                onValueChange = { newMessageText = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth(0.75f),
                /*colors = TextFieldDefaults.colors( // Customize colors if needed
                textColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.Gray
            ),*/
                maxLines = 2 // Allow for multi-line input
            )
            Button(
                onClick = {
                    if (newMessageText.isNotBlank()) {
                        scope.launch(Dispatchers.Main) {
                            buttonEnabled = false
                            // Call your function to send the message
                            // sendMessage(channelId, newMessageText) // Assuming you have a suspend function for this
                            println("Sending message: $newMessageText to channel $channelId")
                            // Optionally, clear the text field and refresh messages
                            // For optimistic updates, you can add the message to the list immediately
                            // val sentMessage = Message( /* ... create a temporary message object ... */)
                            // messages.add(0, sentMessage) // Add to the top for reverseLayout
                            // listState.animateScrollToItem(0)

                            // Simulate sending and then refresh or update based on actual send result
                            // For now, let's just clear and re-fetch as an example
                            var successfullySent = false
                            withContext(Dispatchers.IO) {
                                successfullySent = sendMessage(
                                    channelId,
                                    newMessageText
                                ) // Make sure this function exists
                            }
                            if (successfullySent) {
                                newMessageText = ""
                                val result =
                                    fetchMessages(channelId = channelId) // Re-fetch messages
                                messages.clear()
                                messages.addAll(result)
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(0, 1)
                                }
                                buttonEnabled = true
                            } else {
                                // Handle send failure (e.g., show a toast)
                                println("Failed to send message")
                                buttonEnabled = true
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize(0.25f)
                    .align(Alignment.CenterEnd),
                enabled = newMessageText.isNotBlank() && buttonEnabled // Disable button if text is empty
            ) {
                Text("Send")
            }
        }
    }

    // Full Screen Image Viewer
    fullScreenImageUrl?.let {
        FullScreenImageView(imageUrl = it) {
            fullScreenImageUrl = null // Dismiss the dialog
        }
    }
}