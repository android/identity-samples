package com.google.credentialmanager.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.credentialmanager.sample.R
import com.google.credentialmanager.sample.ui.common.LogoHeading
import com.google.credentialmanager.sample.ui.theme.CredentialManagerTheme

private const val TAG = "PlaceholderScreen"

@Composable
fun PlaceholderScreen() {
    Column(
    modifier = Modifier
        .padding(20.dp)
        .fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
    ){
        LogoHeading()
        Text(
            text = stringResource(R.string.todo),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlaceholderScreenPreview() {
    CredentialManagerTheme {
        PlaceholderScreen()
    }
}