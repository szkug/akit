package com.korilin.samples.compose.trace.acts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.korilin.samples.compose.trace.R
import com.korilin.samples.compose.trace.glide.glideBackground

class NicePatchActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            Column {

                Text(
                    text = "Kotlin",
                    modifier = Modifier.glideBackground(
                        model = null,
                        placeholder = R.drawable.chat_msg_bg
                    )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    modifier = Modifier.glideBackground(
                        model = null,
                        placeholder = R.drawable.chat_msg_bg
                    )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose",
                    modifier = Modifier.glideBackground(
                        model = null,
                        placeholder = R.drawable.chat_msg_bg
                    )
                )
            }
        }
    }
}
