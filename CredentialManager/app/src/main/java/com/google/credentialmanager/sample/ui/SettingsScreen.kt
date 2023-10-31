package com.google.credentialmanager.sample.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.credentialmanager.sample.R
import com.google.credentialmanager.sample.ui.common.PasskeyInfo
import com.google.credentialmanager.sample.ui.common.ShrineButton
import com.google.credentialmanager.sample.ui.common.TextHeader
import com.google.credentialmanager.sample.ui.theme.CredentialManagerTheme

private const val TAG = "SettingsScreen"

@Composable
fun SettingsScreen(
    onCreatePasskeyClicked: () -> Unit,
    onChangePasswordClicked: () -> Unit,
    onHelpClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxHeight()
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextHeader(
            text = stringResource(R.string.account),
        )
        Image(
            painter = painterResource(R.drawable.person_24px),
            contentDescription = "password",
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(50.dp)
        )
        Column(){
            Text(
                text = stringResource(R.string.username),
                style = TextStyle(textAlign = TextAlign.Start),
                modifier = Modifier
                    .padding(start = 10.dp, end = 10.dp)
                    .fillMaxWidth()
            )
            OutlinedTextField(
                value = stringResource(R.string.username),
                onValueChange = {},
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(color = Color(0xFFF9F2F1)),
            )
        }
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.security),
                style = TextStyle(textAlign = TextAlign.Start),
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .fillMaxWidth()
            )
            PasskeyInfo {}

            Spacer(modifier = Modifier.padding(top = 5.dp))

            ShrineButton(onClick = { onCreatePasskeyClicked() }) {
                Text(text = stringResource(R.string.create_passkey))
            }

            Spacer(modifier = Modifier.padding(top = 5.dp))

            Row(
                Modifier
                    .height(70.dp)
                    .background(color = Color(0xFFF9F2F1)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Image(
                        painter = painterResource(R.drawable.clip_path_group),
                        contentDescription = "password",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(10.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.password),
                        style = TextStyle(
                            fontSize = 10.sp,
                            lineHeight = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.padding(2.dp))
                    Text(
                        text = "Last changed April 13, 2023",
                        style = TextStyle(
                            fontSize = 8.sp,
                            lineHeight = 20.sp
                        )
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(R.string.change),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 10.sp,
                            lineHeight = 16.sp
                        ),
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clickable { onChangePasswordClicked() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.padding(10.dp))

        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.contact),
                style = TextStyle(textAlign = TextAlign.Start),
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .fillMaxWidth()
            )
            ShrineButton(onClick = { onHelpClicked() }) {
                Text(text = "Help")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview(){
    CredentialManagerTheme {
        SettingsScreen(
            onCreatePasskeyClicked = {->},
            onChangePasswordClicked = {->},
            onHelpClicked = {->}

        )
    }
}