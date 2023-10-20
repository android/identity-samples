package com.google.credentialmanager.sample.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.credentialmanager.sample.ui.theme.CredentialManagerTheme


@Composable
fun ShrineTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .width(300.dp)
            .background(Color(0xFFE9E1E0))// Set background color here
            .padding(16.dp)
    )
}

@Preview()
@Composable
private fun TextFieldPreview() {
    CredentialManagerTheme {
        ShrineTextField(
            TextFieldValue("demo"),
            onValueChange = {},
        )
    }
}