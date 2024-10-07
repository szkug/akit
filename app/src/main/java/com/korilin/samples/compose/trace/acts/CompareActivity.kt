package com.korilin.samples.compose.trace.acts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.trace
import coil.compose.AsyncImage
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.korilin.samples.compose.trace.Stores

class CompareActivity : ComponentActivity() {

    private val url = Stores.urls.first()

    @OptIn(ExperimentalGlideComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            Column {

                trace("GlideImage.composition") {
                    GlideImage(
                        model = url,
                        contentDescription = null
                    )
                }

                Button(onClick = { finish() }) {
                    Text(
                        "Back",
                    )
                }

                trace("AsyncImage.composition") {
                    AsyncImage(
                        model = url,
                        contentDescription = null
                    )
                }
            }
        }
    }
}
