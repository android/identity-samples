package com.google.credentialmanager.sample.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.credentialmanager.sample.R
import com.google.credentialmanager.sample.ui.theme.CredentialManagerTheme

@Composable
fun PasskeyInfo(
    onLearnMoreClicked: () -> Unit,
    ) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF9F2F1),
                shape = RectangleShape
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.passkey_info_heading),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 24.dp)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 20.dp, start = 15.dp, end = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.passkey_info),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp, lineHeight = 16.sp)
                )
                Text(
                    text = stringResource(R.string.learn_more),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 10.dp)
                        .clickable { onLearnMoreClicked() }
                )
            }
            Image(
                painter = painterResource(R.drawable.passkey_image),
                contentDescription = "passkey logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(170.dp)
            )
        }
    }
}


@Preview (showBackground = true)
@Composable
fun PasskeyInfoPreview(){
    CredentialManagerTheme {
        PasskeyInfo(
            onLearnMoreClicked = {->}
        )
    }
}