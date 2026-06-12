package com.example.ui.screens.login

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.core.auth.FirebaseAuthManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authManager = remember { FirebaseAuthManager(context) }

    // WARNING: Replace this string with a dummy or use string resources,
    // actually we will instruct the user to configure Web Client ID in strings.xml 
    // or as a Secret in AI Studio later.
    val webClientId = if (com.example.BuildConfig.WEB_CLIENT_ID.isNotBlank() && com.example.BuildConfig.WEB_CLIENT_ID != "placeholder_client_id") {
        com.example.BuildConfig.WEB_CLIENT_ID
    } else {
        "your_web_client_id_here"
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    scope.launch {
                        try {
                            val user = authManager.signInWithGoogle(idToken)
                            if (user != null) {
                                // Initialize sync manager and restore data
                                val app = context.applicationContext as com.example.SyncPosApplication
                                val syncManager = com.example.core.sync.CloudSyncManager(app.repository)
                                syncManager.restoreDataFromCloud()
                                onLoginSuccess()
                            } else {
                                errorMessage = "Firebase Authentication is missing or not configured. Please ensure google-services.json is added."
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            errorMessage = e.localizedMessage ?: "Firebase auth failed"
                        } finally {
                            isLoading = false
                        }
                    }
                } else {
                    errorMessage = "No ID token found"
                    isLoading = false
                }
            } catch (e: ApiException) {
                e.printStackTrace()
                errorMessage = "Google sign in failed: ${e.statusCode}"
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    // Auto-login check if already logged in
    LaunchedEffect(Unit) {
        try {
            if (authManager.currentUser != null) {
                onLoginSuccess()
            }
        } catch (e: Exception) {
             // Firebase not initialized
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF97316).copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Sync",
                    color = Color(0xFFF97316),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome to SyncPOS",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1E293B)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Sign in to manage your inventory, sales, and customers seamlessly.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        if (errorMessage != null) {
            Surface(
                color = Color.Red.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color.Red
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = errorMessage!!, color = Color.Red)
                }
            }
        }

        Button(
            onClick = {
                try {
                     isLoading = true
                    val gso = authManager.getGoogleSignInOptions(webClientId)
                     val googleSignInClient = GoogleSignIn.getClient(context, gso)
                     launcher.launch(googleSignInClient.signInIntent)
                } catch(e: Exception) {
                     errorMessage = "Missing google-services.json or Firebase is not configured properly yet."
                     isLoading = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF1E293B)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF1E293B)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("G", fontWeight = FontWeight.Bold, color = Color.Blue, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Continue with Google", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
