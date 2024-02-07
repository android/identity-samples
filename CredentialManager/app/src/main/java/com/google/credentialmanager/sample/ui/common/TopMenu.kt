package com.google.credentialmanager.sample.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.credentialmanager.sample.R
import com.google.credentialmanager.sample.ui.theme.CredentialManagerTheme

@Composable
fun TopMenu(
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(R.drawable.ic_menu_24px),
            contentDescription = "shrine app menu mockup",
            modifier = Modifier.size(150.dp)
        )
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "shrine app menu mockup",
            modifier = Modifier.size(150.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TopMenuPreview(){
    CredentialManagerTheme {
        TopMenu()
    }
}