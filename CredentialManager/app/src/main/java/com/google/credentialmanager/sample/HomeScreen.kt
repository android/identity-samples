package com.google.credentialmanager.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.credentialmanager.sample.ui.theme.CredentialManagerSampleTheme

/**
 * Stateful composable that displays the home screen of the application.
 *
 * This screen is shown after a successful sign-in or sign-up. It displays a welcome message
 * indicating whether the user signed in with a passkey or a password and provides a button
 * to sign out.
 *
 * @param navController The navigation controller used for screen navigation.
 */
@Composable
fun HomeScreen(navController: NavController) {
    CredentialManagerSampleTheme {
        val isSignedInThroughPasskeys = DataProvider.isSignedInThroughPasskeys()
        val message = if (isSignedInThroughPasskeys) {
            stringResource(R.string.logged_in_successfully_through_passkeys)
        } else {
            stringResource(R.string.logged_in_successfully_through_password)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = {
                    DataProvider.configureSignedInPref(false)
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                Text(stringResource(R.string.sign_out_and_try_again))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    CredentialManagerSampleTheme {
        HomeScreen(navController = rememberNavController())
    }
}
