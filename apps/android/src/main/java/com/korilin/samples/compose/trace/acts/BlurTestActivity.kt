package com.korilin.samples.compose.trace.acts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.trace
import cn.szkug.renderscript.toolkit.BlurConfig
import com.bumptech.glide.load.engine.DiskCacheStrategy
import cn.szkug.akit.image.AsyncImageContext
import cn.szkug.akit.image.AkitAsyncImage
import cn.szkug.akit.image.glide.GlideRequestEngine
import cn.szkug.akit.image.rememberAsyncImageContext
import com.korilin.akit.glide.extensions.blur.BlurBitmapConfigOption
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

        AkitAsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = info.cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            engine = GlideRequestEngine(
                requestBuilder = {
                    GlideRequestEngine.NormalGlideRequestBuilder(it).skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .set(BlurBitmapConfigOption, BlurConfig(15)
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