package top.anorak01.wearcord.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.Text
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.anorak01.wearcord.R
import top.anorak01.wearcord.presentation.fetchers.fetchMe
import top.anorak01.wearcord.presentation.me


@OptIn(DelicateCoroutinesApi::class)
@Composable
fun Greeting(navController: NavHostController, greetingName: String, isOpen: Boolean) {
    var welcomeText by remember { mutableStateOf("") }

    welcomeText = stringResource(R.string.hello_world, "")

    SwipeToDismissBox(
        onDismissed = {},
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = welcomeText
            )
            Text(
                text = "Welcome to WearCord",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
            )

        }
    }

    LaunchedEffect(Unit) {
        GlobalScope.launch {
            if (me != null) {
                welcomeText += me!!.global_name ?: ""
                return@launch
            }
            me = fetchMe()
            if (me == null) {
                // if me not fetched, go to login screen
                withContext(Dispatchers.Main) {
                    navController.navigate("qr")
                }
            } else {
                welcomeText += me!!.global_name ?: ""
            }
        }
    }
}