package com.google.credentialmanager.sample

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.credentialmanager.sample.ui.theme.CredentialManagerSampleTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SignInScreen(navController: NavController) {
    CredentialManagerSampleTheme {
        val viewModel: SignInViewModel = viewModel()

        val isLoading by viewModel.isLoading.collectAsState()
        val signInError by viewModel.signInError.collectAsState()

        val context = LocalContext.current
        val activity = context.findActivity()

        LaunchedEffect(Unit) {
            viewModel.navigationEvent.collectLatest {
                event ->
                when (event) {
                    is NavigationEvent.NavigateToHome -> {
                        DataProvider.setSignedInThroughPasskeys(event.signedInWithPasskeys)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Main.route) { inclusive = true }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sign in",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp, bottom = 16.dp)
            )

            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp)) // Consider using theme color: color = MaterialTheme.colorScheme.primary
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("operation in progress...")
                }
            }

            if (signInError != null) {
                Text(
                    signInError!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Button(
                onClick = {
                    if (activity != null) {
                        viewModel.signIn(activity, context)
                    }
                },
                shape = RoundedCornerShape(4.dp),
                enabled = !isLoading && activity != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign in with passkey/saved password")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignInScreenPreview() {
    CredentialManagerSampleTheme {
        SignInScreen(navController = rememberNavController())
    }
}
