package top.anorak01.wearcord.presentation.screens.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.anorak01.wearcord.presentation.fetchers.fetchMe
import top.anorak01.wearcord.presentation.saveToken
import top.anorak01.wearcord.presentation.userToken

@Composable
fun ManualTokenScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var newTokenText by remember { mutableStateOf("") }

    val context = LocalContext.current

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column() {
            Text("Manual token input")


            TextField( // Using OutlinedTextField for better visibility on Wear
                value = newTokenText,
                onValueChange = { newTokenText = it },
                label = { Text("Token") },
                modifier = Modifier.fillMaxWidth(0.75f),

                maxLines = 1 // Allow for multi-line input
            )
            Button(
                onClick = {
                    if (newTokenText.isNotBlank()) {
                        scope.launch(Dispatchers.IO) {
                            if (validateToken(newTokenText)) {
                                withContext(Dispatchers.Main) {
                                    println("success, going home")
                                    userToken = newTokenText
                                    saveToken(context, newTokenText)
                                    navController.clearBackStack("home")
                                    navController.navigate("home")
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(0.25f),
                enabled = newTokenText.isNotBlank() // Disable button if text is empty
            ) {
                Text("Send")
            }
        }
    }

}

fun validateToken(token: String): Boolean {
    return fetchMe(token) != null
}