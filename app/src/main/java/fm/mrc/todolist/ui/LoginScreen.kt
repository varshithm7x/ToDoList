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
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isNewUser) "Create Account" else "Sign In",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (showError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    showError = false
                    try {
                        val result = if (isNewUser) {
                            userManager.signUp(email, password)
                        } else {
                            userManager.signIn(email, password)
                        }
                        
                        result.fold(
                            onSuccess = { onLoginSuccess() },
                            onFailure = { 
                                showError = true
                                errorMessage = it.message ?: "Authentication failed"
                            }
                        )
                    } catch (e: Exception) {
                        showError = true
                        errorMessage = e.message ?: "An error occurred"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isNewUser) "Already have an account? Sign In" else "Need an account? Sign Up",
            modifier = Modifier.clickable { isNewUser = !isNewUser },
            color = MaterialTheme.colorScheme.primary
        )
    }
} 