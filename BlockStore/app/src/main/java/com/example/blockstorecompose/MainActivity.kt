package com.example.blockstorecompose

import android.os.Bundle
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

import com.example.blockstorecompose.ui.theme.BlockstoreComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockstoreComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BlockStoreScreen(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun BlockStoreScreen(name: String,
                     modifier: Modifier = Modifier,
                     viewModel: MainViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsState()

    Column() {
        // Update to grab the text
        var text by rememberSaveable { mutableStateOf("Text") }


        if(state.value.areBytesStored) {
            Button(onClick = { viewModel.clearBytes() }) { Text("Sign Out") }
        } else {
            TextField(
                value = text,
                onValueChange = {
                    text = it
                },
                label = { Text("Label") }
            )
            Button(onClick = { viewModel.storeBytes(text) }) { Text("Sign In") }
        }
    }
}