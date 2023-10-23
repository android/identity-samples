package com.google.credentialmanager.sample.ui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.credentialmanager.sample.R
import com.google.credentialmanager.sample.ui.common.ClickableLearnMore
import com.google.credentialmanager.sample.ui.common.ShrineButton
import com.google.credentialmanager.sample.ui.theme.CredentialManagerTheme


private const val TAG = "LearnMoreScreen"

@Composable
fun LearnMoreScreen(){
    val context = (LocalContext.current as? ComponentActivity)?.applicationContext

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.passkey_image),
            contentDescription = "passkey logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(170.dp)
        )
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.learn_more_heading),
                style = TextStyle(fontSize = 36.sp, textAlign = TextAlign.Start),
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = stringResource(R.string.learn_more_line1),
                style = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Start),
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = stringResource(R.string.learn_more_line2),
                style = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Start),
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = stringResource(R.string.learn_more_line3),
                style = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Start),
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = stringResource(R.string.learn_more_line4),
                style = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Start),
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = stringResource(R.string.learn_more_line5),
                style = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Start),
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = stringResource(R.string.learn_more_line6),
                style = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Start),
                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
            )
            ClickableLearnMore()
        }
        ShrineButton(onClick = { /*TODO*/ }) {
            Text(stringResource(R.string.back_button))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LearnMoreScreenPreview() {
    CredentialManagerTheme{
        LearnMoreScreen()
    }
}