package com.korilin.samples.compose.trace

import android.os.Bundle
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.trace
import com.bumptech.glide.Glide
import com.korilin.samples.compose.trace.theme.ComposetraceTheme
import com.korilin.samples.compose.trace.glide.GlideAsyncImage


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

    private val list = List(10) { RoomInfo.create() }

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

        GlideAsyncImage(
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
                fontWeight = FontWeight.Bold
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
    Row {
        RoomTag_Opt1(R.drawable.compose)
        RoomTag_Opt1(R.drawable.kotlin)
    }
}


@Composable
private fun RoomTagsRow_OptCompare() = trace("Compose:RoomTagsRow_OptCompare") {
    Row {
        RoomTag_Opt1(R.drawable.compose)
        RoomTag(R.drawable.compose)
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
    GlideAsyncImage(
        model = id,
        modifier = Modifier.size(20.dp),
        contentDescription = null
    )
}