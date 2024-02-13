package com.google.credentialmanager.sample.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.credentialmanager.sample.R
import com.google.credentialmanager.sample.ui.theme.CredentialManagerTheme

@Composable
fun TopMenu(
) {
    Row(
        Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Image(
            painterResource(R.drawable.ic_menu_24px),
            contentDescription = "shrine app menu mockup",
            contentScale = ContentScale.Fit,
            modifier = Modifier
        )
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "shrine app menu mockup",
            contentScale = ContentScale.Fit,
            modifier = Modifier
        )

        Spacer(modifier = Modifier.fillMaxWidth(.5f))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(5.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_search_24px),
                contentDescription = "shrine app menu mockup",
                contentScale = ContentScale.Fit,
                modifier = Modifier.padding(5.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Image(
                painter = painterResource(R.drawable.image),
                contentDescription = "shrine app menu mockup",
                contentScale = ContentScale.Fit,
                modifier = Modifier.padding(5.dp)
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun TopMenuPreview(){
    CredentialManagerTheme {
        TopMenu()
    }
}