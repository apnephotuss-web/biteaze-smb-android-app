package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.example.ui.screens.login.LoginScreen
import com.example.ui.screens.main.MainScaffold
import com.example.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val context = LocalContext.current
            val authManager = remember { com.example.core.auth.FirebaseAuthManager(context) }
            var isLoggedIn by remember { mutableStateOf(authManager.currentUser != null) }

            if (isLoggedIn) {
                MainScaffold(onSignOut = { isLoggedIn = false })
            } else {
                LoginScreen(onLoginSuccess = { isLoggedIn = true })
            }
        }
      }
    }
  }
}
