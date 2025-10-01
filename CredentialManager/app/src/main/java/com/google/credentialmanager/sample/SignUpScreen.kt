package com.google.credentialmanager.sample

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.credentialmanager.sample.ui.theme.CredentialManagerSampleTheme
import kotlinx.coroutines.flow.collectLatest

// Helper extension function to find the activity from LocalContext
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    CredentialManagerSampleTheme {
        val viewModel: SignUpViewModel = viewModel()

        val username by viewModel.username.collectAsState()
        val password by viewModel.password.collectAsState()
        val isPasswordInputVisible by viewModel.isPasswordInputVisible.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val usernameError by viewModel.usernameError.collectAsState()
        val passwordError by viewModel.passwordError.collectAsState()
        val passkeyCreationError by viewModel.passkeyCreationError.collectAsState()
        val passwordCreationError by viewModel.passwordCreationError.collectAsState()

        val context = LocalContext.current
        val activity = context.findActivity()

        LaunchedEffect(Unit) {
            viewModel.navigationEvent.collectLatest { event ->
                when (event) {
                    is NavigationEvent.NavigateToHome -> {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Create New Account",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Enter Username") },
                singleLine = true,
                isError = usernameError != null,
                modifier = Modifier.fillMaxWidth()
            )
            if (usernameError != null) {
                Text(usernameError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            if (isPasswordInputVisible) {
                OutlinedTextField(
                    value = password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Enter Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = passwordError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (passwordError != null) {
                    Text(passwordError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }

            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("operation in progress...")
                }
            }
            if (passkeyCreationError != null) {
                Text(
                    passkeyCreationError!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            if (passwordCreationError != null) {
                Text(
                    passwordCreationError!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(0.dp))

            Text(
                text = "Sign in to your account easily and securely with a passkey. Note: Your biometric data is only stored on your devices and will never be shared with anyone.",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Button(
                onClick = {
                    if (activity != null) {
                        viewModel.signUpWithPasskey(activity, context)
                    }
                },
                shape = RoundedCornerShape(4.dp),
                enabled = !isLoading && activity != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Up with passkey")
            }

            Text(
                text = "-------------------- OR --------------------",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = "Sign up to your account with a password. Your password will be saved securely with your password provider.",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Button(
                onClick = {
                    if (activity != null) {
                        viewModel.signUpWithPassword(activity)
                    }
                },
                shape = RoundedCornerShape(4.dp),
                enabled = !isLoading && activity != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isPasswordInputVisible) "Sign up with Password" else "Sign up with a password instead")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    CredentialManagerSampleTheme {
        SignUpScreen(navController = rememberNavController())
    }
}
