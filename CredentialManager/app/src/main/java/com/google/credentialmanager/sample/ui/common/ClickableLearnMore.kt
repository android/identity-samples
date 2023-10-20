package com.google.credentialmanager.sample.ui.common

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.credentialmanager.sample.R

@Composable
fun ClickableLearnMore() {
    val context = LocalContext.current
    val intent = remember { Intent(Intent.ACTION_VIEW, Uri.parse("https://developers.google.com/identity/passkeys/")) }
        Text(
            text = stringResource(R.string.learn_more),
                    modifier = Modifier.clickable(
                    onClick = { context.startActivity(intent) })
        )
    }