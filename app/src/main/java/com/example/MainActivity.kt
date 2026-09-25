package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.model.User
import com.example.ui.ChatViewModel
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
    LOGIN,
    HOME,
    CHAT,
    PROFILE
}

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                K118App(viewModel = chatViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        chatViewModel.setOnlinePresence(true)
    }

    override fun onPause() {
        super.onPause()
        chatViewModel.setOnlinePresence(false)
    }
}

@Composable
fun K118App(viewModel: ChatViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val activeChatUser by viewModel.activeChatUser.collectAsState()
    val uiMessage by viewModel.uiStateMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by remember {
        mutableStateOf(if (currentUser != null) Screen.HOME else Screen.LOGIN)
    }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            currentScreen = Screen.LOGIN
        } else if (currentScreen == Screen.LOGIN) {
            currentScreen = Screen.HOME
        }
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "screen_navigation"
        ) { screen ->
            when (screen) {
                Screen.LOGIN -> {
                    LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = { currentScreen = Screen.HOME }
                    )
                }

                Screen.HOME -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenChat = { user ->
                            viewModel.openChatWith(user)
                            currentScreen = Screen.CHAT
                        },
                        onOpenProfile = {
                            currentScreen = Screen.PROFILE
                        },
                        onNavigateToLogin = {
                            currentScreen = Screen.LOGIN
                        }
                    )
                }

                Screen.CHAT -> {
                    val targetUser = activeChatUser
                    if (targetUser != null) {
                        BackHandler {
                            viewModel.closeActiveChat()
                            currentScreen = Screen.HOME
                        }
                        ChatScreen(
                            recipientUser = targetUser,
                            viewModel = viewModel,
                            onBack = {
                                viewModel.closeActiveChat()
                                currentScreen = Screen.HOME
                            }
                        )
                    } else {
                        currentScreen = Screen.HOME
                    }
                }

                Screen.PROFILE -> {
                    BackHandler {
                        currentScreen = Screen.HOME
                    }
                    ProfileScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.HOME },
                        onLoggedOut = { currentScreen = Screen.LOGIN }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
