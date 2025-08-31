package top.anorak01.wearcord.presentation.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.anorak01.wearcord.presentation.login.send2FA
import top.anorak01.wearcord.presentation.saveToken

@Composable
fun MFAScreen(navController: NavHostController, ticket: String) {
    val scope = rememberCoroutineScope()
    var new2FAText by remember { mutableStateOf("") }


    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { // bounding box
        Column(
            modifier = Modifier.fillMaxSize(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { // column for text entries and login button below
            // code entry
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.background) // Add a background to overlay content
                    .padding(horizontal = 16.dp, vertical = 3.dp)
            ) {
                TextField( // Using OutlinedTextField for better visibility on Wear
                    value = new2FAText,
                    onValueChange = { new2FAText = it },
                    label = { Text("2FA Code") },
                    maxLines = 1 // Allow for multi-line input
                )
            }

            // login button
            Button(
                onClick = {
                    if (new2FAText.isNotBlank()) {
                        scope.launch(Dispatchers.Main) {
                            var token: String? = null
                            withContext(Dispatchers.IO) {
                                token = send2FA(
                                    ticket,
                                    new2FAText
                                ) // sendLogin automatically sets the user token if successful
                            }
                            if (token != null && token.isNotBlank()) {
                                saveToken(context, token)
                                navController.clearBackStack("home")
                                new2FAText = ""
                            }

                        }

                    }
                },
                modifier = Modifier.fillMaxSize(),
                enabled = new2FAText.isNotBlank() // Disable button if text is empty
            ) {
                Text("Confirm")
            }
        }
    }
}