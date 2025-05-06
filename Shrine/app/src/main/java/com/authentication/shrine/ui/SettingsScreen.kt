package com.authentication.shrine.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.authentication.shrine.R
import com.authentication.shrine.ui.common.ShrineButton
import com.authentication.shrine.ui.common.ShrineEditText
import com.authentication.shrine.ui.common.ShrineLoader
import com.authentication.shrine.ui.common.ShrineTextHeader
import com.authentication.shrine.ui.common.ShrineToolbar
import com.authentication.shrine.ui.theme.ShrineTheme
import com.authentication.shrine.ui.viewmodel.SettingsUiState
import com.authentication.shrine.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onCreatePasskeyClicked: () -> Unit,
    onChangePasswordClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    onManagePasskeysClicked: () -> Unit,
) {
    val uiState = viewModel.uiState.collectAsState().value

    SettingsScreen(
        onLearnMoreClicked = onLearnMoreClicked,
        onCreatePasskeyClicked = onCreatePasskeyClicked,
        onChangePasswordClicked = onChangePasswordClicked,
        onManagePasskeysClicked = onManagePasskeysClicked,
        uiState = uiState
    )
}

@Composable
fun SettingsScreen(
    onCreatePasskeyClicked: () -> Unit,
    onChangePasswordClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    onManagePasskeysClicked: () -> Unit,
    uiState: SettingsUiState,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShrineToolbar(true)

            ShrineTextHeader(stringResource(R.string.account))

            ShrineEditText(
                title = stringResource(R.string.full_name),
                isFieldLocked = true
            )

            ShrineEditText(
                title = stringResource(R.string.username),
                text = uiState.username,
                isFieldLocked = true
            )

            SecuritySection(
                onLearnMoreClicked = onLearnMoreClicked,
                onCreatePasskeyClicked = onCreatePasskeyClicked,
                onChangePasswordClicked = onChangePasswordClicked,
                onManagePasskeysClicked = onManagePasskeysClicked,
                uiState = uiState
            )
        }

        if (!uiState.isLoading) {
            ShrineLoader()
        }

        if (uiState.errorMessage.isNotBlank()) {
            LaunchedEffect(uiState) {
                snackbarHostState.showSnackbar(message = uiState.errorMessage)
            }
        }
    }
}

@Composable
fun SecuritySection(
    onLearnMoreClicked: () -> Unit,
    onCreatePasskeyClicked: () -> Unit,
    onChangePasswordClicked: () -> Unit,
    onManagePasskeysClicked: () -> Unit,
    uiState: SettingsUiState,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.security))

        if (uiState.userHasPasskeys) {
            PasskeysManagementTab(
                onManageClicked = onManagePasskeysClicked,
                noOfPasskeys = uiState.passkeysList.size,
                isButtonEnabled = !uiState.isLoading
            )
        } else {
            CreatePasskeyTab(
                onLearnMoreClicked = onLearnMoreClicked,
                onCreatePasskeyClicked = onCreatePasskeyClicked,
                isButtonEnabled = !uiState.isLoading
            )
        }

        PasswordManagementTab(
            onChangePasswordClicked = onChangePasswordClicked,
            lastPasswordChange = uiState.passwordChanged,
            isButtonEnabled = !uiState.isLoading
        )
    }
}

@Composable
fun PasskeysManagementTab(
    onManageClicked: () -> Unit,
    noOfPasskeys: Int,
    isButtonEnabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFF4F4F4))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_passkey),
            contentDescription = stringResource(R.string.icon_passkeys)
        )

        Column(
            modifier = Modifier.weight(1F),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.passkeys),
                style = MaterialTheme.typography.displayMedium
            )

            Text(
                text = "$noOfPasskeys passkey" + if (noOfPasskeys == 1) {
                    ""
                } else {
                    "s"
                },
                style = MaterialTheme.typography.bodySmall
            )
        }

        TextButton(
            onClick = onManageClicked,
            enabled = isButtonEnabled
        ) {
            Text(
                stringResource(R.string.manage),
                color = Companion.Black
            )
        }
    }
}

@Composable
fun CreatePasskeyTab(
    onLearnMoreClicked: () -> Unit,
    onCreatePasskeyClicked: () -> Unit,
    isButtonEnabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFF4F4F4))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(0.6F),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.sign_in_faster_next_time),
                    style = MaterialTheme.typography.bodyLarge
                )

                val annotatedText = buildAnnotatedString {
                    append(stringResource(R.string.create_passkey_text))
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        pushStringAnnotation("url", "url")
                        append(stringResource(R.string.how_passkeys_work))
                    }
                }

                ClickableText(
                    text = annotatedText,
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(tag = "url", start = offset, end = offset).firstOrNull()?.let {
                            onLearnMoreClicked()
                        }
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Image(
                modifier = Modifier.weight(0.4F),
                painter = painterResource(R.drawable.ic_passkeys_info),
                contentDescription = ""
            )
        }

        ShrineButton(
            onClick = onCreatePasskeyClicked,
            buttonText = stringResource(R.string.create_passkey),
            isButtonEnabled = isButtonEnabled,
        )
    }
}

@Composable
fun PasswordManagementTab(
    onChangePasswordClicked: () -> Unit,
    lastPasswordChange: String,
    isButtonEnabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFF4F4F4))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.clip_path_group),
            contentDescription = ""
        )

        Column(
            modifier = Modifier.weight(1F),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.password),
                style = MaterialTheme.typography.displayMedium
            )

            Text(
                text = stringResource(R.string.last_changed, lastPasswordChange),
                style = MaterialTheme.typography.bodySmall
            )
        }

        TextButton(
            onClick = onChangePasswordClicked,
            enabled = isButtonEnabled
        ) {
            Text(stringResource(R.string.change), color = Companion.Black)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingPreview() {
    ShrineTheme {
        SettingsScreen(
            onCreatePasskeyClicked = { },
            onChangePasswordClicked = { },
            onLearnMoreClicked = { },
            onManagePasskeysClicked = { },
            uiState = SettingsUiState()
        )
    }
}
