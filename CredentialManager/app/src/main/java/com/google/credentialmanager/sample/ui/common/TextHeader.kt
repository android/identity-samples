package com.google.credentialmanager.sample.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.credentialmanager.sample.ui.theme.CredentialManagerTheme

@Composable
fun TextHeader(
    text: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(start = 5.dp, bottom = 15.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 30.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight(400),
            )
        )
    }
}


@Preview(showBackground = true)
@Composable
fun TextHeaderPreview(){
    CredentialManagerTheme {
        TextHeader(text = "Heading Text")
    }
}