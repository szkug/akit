package com.korilin.samples.compose.trace.acts

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
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
import com.korilin.compose.akit.blur.customBlur
import com.korilin.samples.compose.trace.R
import com.korilin.samples.compose.trace.Stores


data class RoomInfo(
    val cover: String,
    val name: String,
) {

    companion object {

        fun create() = RoomInfo(
            cover = Stores.urls.random(),
            name = Stores.names.random()
        )
    }
}

class RoomGridListActivity : ComponentActivity() {

    private val list = List(1) { RoomInfo.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            Column {
                RoomTagsRow_OptCompare()
                RoomGridList(list, Modifier.padding())
            }
        }
    }
}

@Composable
@Preview
private fun RoomGrid_Preview() {
    RoomGridItem(RoomInfo.create())
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

        com.korilin.compose.akit.image.glide.GlideAsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = info.cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
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

            RoomTagsRow_Opt1()

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

@Composable
private fun RoomTagsRow() = trace("Compose:RoomTagsRow") {
    LazyRow {
        item {
            RoomTag(R.drawable.compose)
        }
        item {
            RoomTag(R.drawable.kotlin)
        }
    }
}

@Composable
private fun RoomTagsRow_Opt1() = trace("Compose:RoomTagsRow") {
    Row(
        modifier = Modifier
            .graphicsLayer {
                Log.d("BlurNode", "graphicsLayer")
            }
            .customBlur(15)
    ) {
        RoomTag_Opt1(R.drawable.compose)
        RoomTag_Opt1(R.drawable.kotlin)
    }
}


@Composable
private fun RoomTagsRow_OptCompare() = trace("Compose:RoomTagsRow_OptCompare") {
    Row {

        trace("Compose:RoomTagsRow_OptCompare:Bigger") {
            RoomTag_Opt1(R.drawable.compose)
            RoomTag(R.drawable.compose)
        }

        trace("Compose:RoomTagsRow_OptCompare:Smaller") {
            RoomTag_Opt1(R.drawable.music)
            RoomTag(R.drawable.music)
        }

    }
}



@Composable
private fun RoomTag(@DrawableRes id: Int) = trace("Compose:RoomTag") {
    Image(
        painter = painterResource(id),
        modifier = Modifier.size(20.dp),
        contentDescription = null
    )
}

@Composable
private fun RoomTag_Opt1(@DrawableRes id: Int) = trace("Compose:RoomTag") {
    com.korilin.compose.akit.image.glide.GlideAsyncImage(
        model = id,
        modifier = Modifier.size(20.dp),
        contentDescription = null
    )
}