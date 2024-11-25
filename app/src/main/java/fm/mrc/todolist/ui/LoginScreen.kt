package fm.mrc.todolist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import fm.mrc.todolist.data.ServerUserManager
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.content.Context

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    userManager: ServerUserManager
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isNewUser by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val savedEmail = prefs.getString("rememberedEmail", "")
        val savedPassword = prefs.getString("rememberedPassword", "")
        
        if (!savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
            email = savedEmail
            password = savedPassword
            rememberMe = true
            scope.launch {
                val result = userManager.signIn(email, password)
                if (result.isSuccess) {
                    onLoginSuccess()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isNewUser) "Create Account" else "Login",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it }
            )
            Text(
                text = "Remember Me",
                modifier = Modifier.clickable { rememberMe = !rememberMe }
            )
        }

        if (showError) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                isLoading = true
                showError = false
                scope.launch {
                    val result = if (isNewUser) {
                        userManager.signUp(email, password)
                    } else {
                        userManager.signIn(email, password)
                    }
                    
                    if (result.isSuccess) {
                        if (rememberMe) {
                            context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("rememberedEmail", email)
                                .putString("rememberedPassword", password)
                                .apply()
                        }
                        onLoginSuccess()
                    } else {
                        showError = true
                        errorMessage = if (isNewUser) {
                            "Failed to create account"
                        } else {
                            "Invalid email or password"
                        }
                    }
                    isLoading = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(if (isNewUser) "Sign Up" else "Sign In")
            }
        }

        TextButton(
            onClick = { isNewUser = !isNewUser }
        ) {
            Text(
                if (isNewUser) "Already have an account? Sign In" 
                else "Don't have an account? Sign Up"
            )
        }
    }
} 