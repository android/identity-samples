package com.authentication.shrine.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.authentication.shrine.ui.theme.ShrineTheme

@Composable
fun ShrineEditText(
    title: String,
    text: String = "",
    isFieldLocked: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title)

        OutlinedTextField(
            value = text,
            onValueChange = { },
            modifier = Modifier
                .fillMaxWidth(),
            readOnly = isFieldLocked,
            shape = RoundedCornerShape(32.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                errorBorderColor = Color.Red,
                focusedContainerColor = Color(0xFFF4F4F4),
                unfocusedContainerColor = Color(0xFFF4F4F4),
                disabledContainerColor = Color(0xFFF4F4F4),
                errorContainerColor = Color(0xFFF4F4F4)
            )
        )
    }
}

@Preview(showSystemUi = true)
@Composable
fun ShrineEditTextPreview() {
    ShrineTheme {
        ShrineEditText(
            "Full Name",
            "ABC XYZ"
        )
    }
}
