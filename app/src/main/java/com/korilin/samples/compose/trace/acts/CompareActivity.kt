package com.korilin.samples.compose.trace.acts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.trace
import coil.compose.AsyncImage
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.korilin.samples.compose.trace.R
import com.korilin.samples.compose.trace.Stores
import com.korilin.samples.compose.trace.glide.GlideAsyncImage

class CompareActivity : ComponentActivity() {

    private var url by mutableStateOf<String?>(null)

    @OptIn(ExperimentalGlideComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {


//                Text("Image", modifier = Modifier.padding(top = 20.dp))
//                val mod = Modifier
//                    .height(50.dp)
//                    .wrapContentWidth()
//                    .background(Color.Red)
//                val painter = if (url == null) painterResource(R.drawable.kotlin_long) else painterResource(R.drawable.kotlin)
//                Row {
//                    Image(
//                        painter = painter,
//                        contentDescription = null,
//                        contentScale = ContentScale.FillHeight,
//                        alignment = Alignment.CenterStart,
//                        modifier = mod
//                    )
//                    Image(
//                        painter = painter,
//                        contentDescription = null,
//                        contentScale = ContentScale.FillHeight,
//                        alignment = Alignment.CenterStart,
//                        modifier = mod
//                    )
//                    Image(
//                        painter = painter,
//                        contentDescription = null,
//                        contentScale = ContentScale.FillHeight,
//                        alignment = Alignment.CenterStart,
//                        modifier = mod
//                    )
//                }


                Text("GlideImage", modifier = Modifier.padding(top = 20.dp))
                Row {
                    GlideImage(
                        model = url,
                        modifier = Modifier
                            .height(50.dp)
                            .wrapContentWidth()
                            .background(Color.Red),
                        contentScale = ContentScale.FillHeight,
                        alignment = Alignment.CenterStart,
                        contentDescription = null
                    )
                    GlideImage(
                        model = url,
                        modifier = Modifier
                            .height(50.dp)
                            .wrapContentWidth()
                            .background(Color.Red)
                            .onSizeChanged { },
                        contentScale = ContentScale.FillHeight,
                        alignment = Alignment.CenterStart,
                        contentDescription = null
                    )
                    GlideImage(
                        model = url,
                        modifier = Modifier
                            .height(50.dp)
                            .wrapContentWidth()
                            .background(Color.Red),
                        contentScale = ContentScale.FillHeight,
                        alignment = Alignment.CenterStart,
                        contentDescription = null
                    )
                }

                Text("AsyncImage", modifier = Modifier.padding(top = 20.dp))
                Row {
                    AsyncImage(
                        model = url,
                        modifier = Modifier
                            .height(50.dp)
                            .wrapContentWidth()
                            .background(Color.Red),
                        contentScale = ContentScale.FillHeight,
                        alignment = Alignment.CenterStart,
                        contentDescription = null
                    )
                    AsyncImage(
                        model = url,
                        modifier = Modifier
                            .height(50.dp)
                            .wrapContentWidth()
                            .background(Color.Red),
                        contentScale = ContentScale.FillHeight,
                        alignment = Alignment.CenterStart,
                        contentDescription = null
                    )
                    AsyncImage(
                        model = url,
                        modifier = Modifier
                            .height(50.dp)
                            .wrapContentWidth()
                            .background(Color.Red),
                        contentScale = ContentScale.FillHeight,
                        alignment = Alignment.CenterStart,
                        contentDescription = null
                    )
                }


                Text("GlideAsyncImage", modifier = Modifier.padding(top = 20.dp))
                Row {
                    GlideAsyncImage(
                        model = url,
                        modifier = Modifier
                            .height(50.dp)
                            .wrapContentWidth()
                            .background(Color.Red),
                        contentScale = ContentScale.FillHeight,
                        alignment = Alignment.CenterStart,
                        contentDescription = null
                    )
                    GlideAsyncImage(
                        model = url,
                        modifier = Modifier
                            .height(50.dp)
                            .wrapContentWidth()
                            .background(Color.Red),
                        contentScale = ContentScale.FillHeight,
                        alignment = Alignment.CenterStart,
                        contentDescription = null
                    )
                    GlideAsyncImage(
                        model = url,
                        modifier = Modifier
                            .height(50.dp)
                            .wrapContentWidth()
                            .background(Color.Red),
                        contentScale = ContentScale.FillHeight,
                        alignment = Alignment.CenterStart,
                        contentDescription = null
                    )
                }

                Button(
                    modifier = Modifier.padding(top = 20.dp),
                    onClick = { url = Stores.urls.first() }
                ) {
                    Text("Load")
                }

//                trace("GlideImage.composition") {
//                    GlideImage(
//                        model = url,
//                        contentDescription = null
//                    )
//                }
//
//                trace("AsyncImage.composition") {
//                    AsyncImage(
//                        model = url,
//                        contentDescription = null
//                    )
//                }
            }
        }
    }
}
