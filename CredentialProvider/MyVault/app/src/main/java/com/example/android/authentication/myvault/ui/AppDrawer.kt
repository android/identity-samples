/*
 * Copyright 2024 The Android Open Source Project
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
package com.example.android.authentication.myvault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.android.authentication.myvault.Dimensions
import com.example.android.authentication.myvault.R

/**
 * The AppDrawer composable function creates a drawer for the MyVault app.
 *
 * @param currentRoute The current route of the app.
 * @param navigateToHome A lambda function to navigate to the home screen.
 * @param navigateToSettings A lambda function to navigate to the settings screen.
 * @param closeDrawer A lambda function to close the drawer.
 */
@Composable
fun AppDrawer(
    currentRoute: String,
    navigateToHome: () -> Unit,
    navigateToSettings: () -> Unit,
    closeDrawer: () -> Unit,
) {
    MyVaultContent()
    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .2f))
    DrawerButton(
        image = Icons.Filled.Home,
        label = (stringResource(R.string.credentials)),
        isSelected = currentRoute == MyVaultDestinations.HOME_ROUTE,
        action = {
            navigateToHome()
            closeDrawer()
        },
    )
    DrawerButton(
        image = Icons.Filled.Settings,
        label = (stringResource(R.string.settings)),
        isSelected = currentRoute == MyVaultDestinations.SETTINGS_ROUTE,
        action = {
            navigateToSettings()
            closeDrawer()
        },
    )
}

/**
 * The MyVaultContent composable function displays the MyVault logo and app name.
 *
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
private fun MyVaultContent(modifier: Modifier = Modifier) {
    Row(
        modifier.padding(top = Dimensions.padding_medium, bottom = Dimensions.padding_medium),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier.width(Dimensions.padding_large))
        Icon(
            painter = painterResource(R.drawable.android_secure),
            contentDescription = stringResource(id = R.string.app_name),
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(40.dp),
        )
        Text(
            color = MaterialTheme.colorScheme.primary,
            text = "MyVault",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            modifier = modifier.align(Alignment.CenterVertically),
        )
    }
}

/**
 * The DrawerButton composable function creates a button for the MyVault app's navigation drawer.
 *
 * @param image The image to display on the button.
 * @param label The text to display on the button.
 * @param isSelected Whether the button is currently selected.
 * @param action The action to perform when the button is clicked.
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
private fun DrawerButton(
    image: ImageVector,
    label: String,
    isSelected: Boolean,
    action: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NavigationDrawerItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.background,
        selectedTextColor = MaterialTheme.colorScheme.background,
        unselectedIconColor = MaterialTheme.colorScheme.primary,
        unselectedTextColor = MaterialTheme.colorScheme.primary,
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        unselectedContainerColor = MaterialTheme.colorScheme.background,
    )

    Spacer(modifier.width(Dimensions.padding_large))
    NavigationDrawerItem(
        onClick = action,
        icon = { Icon(image, null) },
        label = {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        selected = isSelected,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 1.dp,
                top = Dimensions.padding_medium,
                end = 1.dp,
            ),
        colors = colors,
    )
}
