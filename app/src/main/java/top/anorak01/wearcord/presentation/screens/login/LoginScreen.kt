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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.anorak01.wearcord.presentation.login.sendLogin
import top.anorak01.wearcord.presentation.saveToken

@Composable
fun LoginScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()

    var newLoginText by remember { mutableStateOf("") }

    var newPasswordText by remember { mutableStateOf("") }

    val context = LocalContext.current


    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { // bounding box
        Column(
            modifier = Modifier.fillMaxSize(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { // column for text entries and login button below
            // mail entry
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.background) // Add a background to overlay content
                    .padding(horizontal = 16.dp, vertical = 3.dp)
            ) {
                TextField( // Using OutlinedTextField for better visibility on Wear
                    value = newLoginText,
                    onValueChange = { newLoginText = it },
                    label = { Text("Email") },
                    maxLines = 1 // Allow for multi-line input
                )
            }

            // password entry
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.background) // Add a background to overlay content
                    .padding(horizontal = 16.dp, vertical = 3.dp)
            ) {
                TextField( // Using OutlinedTextField for better visibility on Wear
                    value = newPasswordText,
                    onValueChange = { newPasswordText = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    maxLines = 1 // Allow for multi-line input
                )
            }


            // login button
            Button(
                onClick = {
                    if (newLoginText.isNotBlank() && newPasswordText.isNotBlank()) {
                        scope.launch(Dispatchers.Main) {
                            // Simulate sending and then refresh or update based on actual send result
                            // For now, let's just clear and re-fetch as an example
                            var token: Pair<String?, Boolean> = Pair(null, false)
                            withContext(Dispatchers.IO) {
                                token = sendLogin(
                                    newLoginText,
                                    newPasswordText
                                ) // sendLogin automatically sets the user token if successful
                            }
                            if (!token.second) { // mfa not required
                                newLoginText = ""
                                newPasswordText = ""

                                if (token.first != null && token.first?.isNotBlank() ?: false) {
                                    saveToken(context, token.first!!)
                                }

                                // exit this view and go to home logged in
                                navController.clearBackStack("home")
                            } else {
                                navController.navigate("mfa/${token.first}")
                            }
                        }

                    }
                },
                modifier = Modifier.fillMaxSize(),
                enabled = newLoginText.isNotBlank() && newPasswordText.isNotBlank() // Disable button if text is empty
            ) {
                Text("Log In")
            }
        }
    }
}