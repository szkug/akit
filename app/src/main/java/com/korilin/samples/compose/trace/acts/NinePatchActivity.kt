package com.korilin.samples.compose.trace.acts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.korilin.samples.compose.trace.R
import com.korilin.samples.compose.trace.glide.glideBackground

class NinePatchActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {

                Text(
                    text = "Kotlin",
                    color = Color.White,
                    modifier = Modifier.glideBackground(
                        model = R.drawable.chat_msg_bg,
                    )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier.glideBackground(
                        model = R.drawable.chat_msg_bg,
                    )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier.glideBackground(
                        model = R.drawable.chat_msg_bg,
                    )
                )
            }
        }
    }
}
