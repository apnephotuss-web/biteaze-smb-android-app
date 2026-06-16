package com.example.ui.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.auth.FirebaseAuthManager
import kotlinx.coroutines.launch

enum class AuthMode {
    SIGN_IN, SIGN_UP, FORGOT_PASSWORD
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("F&B") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authManager = remember { FirebaseAuthManager(context) }
    val isFirebaseOnline = remember { authManager.isFirebaseAvailable() }

    LaunchedEffect(Unit) {
        if (authManager.isUserLoggedIn()) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (mode == AuthMode.FORGOT_PASSWORD) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    mode = AuthMode.SIGN_IN
                    errorMessage = null
                    successMessage = null
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Back to Sign In", fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            }
        }

        Surface(
            modifier = Modifier.size(90.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF97316).copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Sync",
                    color = Color(0xFFF97316),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = when (mode) {
                AuthMode.SIGN_IN -> "Welcome Back"
                AuthMode.SIGN_UP -> "Create Account"
                AuthMode.FORGOT_PASSWORD -> "Reset Password"
            },
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1E293B)
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = when (mode) {
                AuthMode.SIGN_IN -> "Sign in with your phone number and password"
                AuthMode.SIGN_UP -> "Set up your SyncPOS workspace"
                AuthMode.FORGOT_PASSWORD -> "Enter your email to receive a reset link"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(visible = !isFirebaseOnline) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFDC2626))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Firebase Project Disconnected",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Authentication is strictly enforced. You must add the google-services.json file.",
                        fontSize = 12.sp,
                        color = Color(0xFF991B1B)
                    )
                }
            }
        }

        AnimatedVisibility(visible = errorMessage != null) {
            Surface(
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = errorMessage ?: "", color = Color(0xFF991B1B), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        AnimatedVisibility(visible = successMessage != null) {
            Surface(
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF22C55E))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = successMessage ?: "", color = Color(0xFF166534), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (mode == AuthMode.SIGN_IN || mode == AuthMode.SIGN_UP) {
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it.filter { char -> char.isDigit() || char == '+' } },
                label = { Text("Mobile Number") },
                placeholder = { Text("+15555555555") },
                enabled = !isLoading,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = "Phone Icon", tint = Color(0xFF64748B))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFF97316),
                    focusedLabelColor = Color(0xFFF97316),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (mode == AuthMode.SIGN_UP || mode == AuthMode.FORGOT_PASSWORD) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                enabled = !isLoading,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = "Email Icon", tint = Color(0xFF64748B))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFF97316),
                    focusedLabelColor = Color(0xFFF97316),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (mode == AuthMode.SIGN_IN || mode == AuthMode.SIGN_UP) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                enabled = !isLoading,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock Icon", tint = Color(0xFF64748B))
                },
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle password visibility")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFF97316),
                    focusedLabelColor = Color(0xFFF97316),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )
        }

        if (mode == AuthMode.SIGN_UP) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Select Business Category (Cannot be changed later)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF475569),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(8.dp))
            val categories = listOf("F&B", "Grocery", "Service-Based")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = category == cat
                    Card(
                        modifier = Modifier
                            .clickable { if (!isLoading) category = cat },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFF97316) else Color.White
                        ),
                        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)) else null
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        } else if (mode == AuthMode.SIGN_IN) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { 
                    mode = AuthMode.FORGOT_PASSWORD 
                    errorMessage = null
                    successMessage = null
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot Password?", color = Color(0xFFF97316), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (!isFirebaseOnline) {
                    errorMessage = "Cannot authenticate. google-services.json configuration is missing."
                    return@Button
                }

                errorMessage = null
                successMessage = null
                
                scope.launch {
                    try {
                        isLoading = true
                        when (mode) {
                            AuthMode.SIGN_IN -> {
                                if (phoneNumber.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter phone and password."
                                    return@launch
                                }
                                if (authManager.signIn(phoneNumber, password)) {
                                    val app = context.applicationContext as com.example.SyncPosApplication
                                    com.example.core.sync.CloudSyncManager(app.repository).restoreDataFromCloud()
                                    onLoginSuccess()
                                }
                            }
                            AuthMode.SIGN_UP -> {
                                if (phoneNumber.isBlank() || email.isBlank() || password.length < 6) {
                                    errorMessage = "Please enter all fields. Password must be 6+ characters."
                                    return@launch
                                }
                                if (authManager.signUp(phoneNumber, email, password, category)) {
                                    val app = context.applicationContext as com.example.SyncPosApplication
                                    com.example.core.sync.CloudSyncManager(app.repository).restoreDataFromCloud()
                                    onLoginSuccess()
                                }
                            }
                            AuthMode.FORGOT_PASSWORD -> {
                                if (email.isBlank() || !email.contains("@")) {
                                    errorMessage = "Please enter a valid email address."
                                    return@launch
                                }
                                if (authManager.resetPassword(email)) {
                                    successMessage = "Password reset email sent to $email"
                                    email = ""
                                }
                            }
                        }
                    } catch (e: Exception) {
                        errorMessage = e.message
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("auth_submit_button"),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text(
                    text = when (mode) {
                        AuthMode.SIGN_IN -> "Sign In"
                        AuthMode.SIGN_UP -> "Sign Up"
                        AuthMode.FORGOT_PASSWORD -> "Send Reset Link"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (mode == AuthMode.SIGN_IN) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account?", color = Color(0xFF64748B))
                TextButton(onClick = { 
                    mode = AuthMode.SIGN_UP 
                    errorMessage = null
                    successMessage = null
                }) {
                    Text("Sign Up", color = Color(0xFFF97316), fontWeight = FontWeight.Bold)
                }
            }
        } else if (mode == AuthMode.SIGN_UP) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account?", color = Color(0xFF64748B))
                TextButton(onClick = { 
                    mode = AuthMode.SIGN_IN
                    errorMessage = null
                    successMessage = null
                }) {
                    Text("Sign In", color = Color(0xFFF97316), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
