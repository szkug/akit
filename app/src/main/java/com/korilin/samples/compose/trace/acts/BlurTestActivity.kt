package com.korilin.samples.compose.trace.acts

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.trace
import com.korilin.akit.compose.image.publics.AsyncImageContext
import com.korilin.akit.compose.image.publics.BitmapTranscoder
import com.korilin.akit.compose.image.publics.GlideAsyncImage
import com.korilin.akit.compose.image.publics.rememberAsyncImageContext
import com.korilin.akit.glide.plugin.blur.BlurBitmapConfigOption
import com.korilin.akit.glide.plugin.blur.BlurConfig
import com.korilin.samples.compose.trace.R
import com.korilin.samples.compose.trace.Stores


data class RoomInfo(
    val cover: Any,
    val name: String,
)

class RoomGridListActivity : ComponentActivity() {

    private val list = listOf(
        RoomInfo(
            cover = Stores.urls.random(),
            name = Stores.names.random()
        ),

        RoomInfo(
            cover = Stores.imgIds.random(),
            name = Stores.names.random()
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column {
                RoomGridList(list, Modifier.padding())
            }
        }
    }
}

@Composable
private fun RoomGridList(list: List<RoomInfo>, modifier: Modifier) = trace("Compose:RoomGridList") {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        horizontalArrangement = Arrangement.Absolute.spacedBy(10.dp),
        verticalArrangement = Arrangement.Absolute.spacedBy(10.dp),
        contentPadding = PaddingValues(15.dp)
    ) {
        items(list) {
            RoomGridItem(it)
        }
    }
}

@Composable
private fun RoomGridItem(info: RoomInfo) = trace("Compose:RoomGridItem") {

    Box(
        modifier = Modifier
            .width(160.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {

        GlideAsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = info.cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            extension = rememberAsyncImageContext(
                requestBuilder = {
                    AsyncImageContext.NormalGlideRequestBuilder(it).set(
                        BlurBitmapConfigOption, BlurConfig(15)
                    )
                }
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0f),
                            Color.Black.copy(alpha = 0.5f),
                        )
                    )
                )
                .padding(10.dp)
        ) {

            Text(
                text = info.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
            )
        }
    }
}