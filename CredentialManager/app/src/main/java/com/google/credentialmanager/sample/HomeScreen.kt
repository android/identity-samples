/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.credentialmanager.sample.ui.theme.AppDimensions.paddingExtraSmall
import com.google.credentialmanager.sample.ui.theme.AppDimensions.paddingMedium
import com.google.credentialmanager.sample.ui.theme.AppDimensions.screenPadding
import com.google.credentialmanager.sample.ui.theme.AppDimensions.screenTitleFontSize
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
                .padding(screenPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(paddingMedium),
                textAlign = TextAlign.Center,
                fontSize = screenTitleFontSize,
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
                shape = RoundedCornerShape(paddingExtraSmall),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = screenPadding)
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
