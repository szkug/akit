package com.korilin.samples.compose.trace.acts

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.korilin.samples.compose.trace.R
import com.korilin.samples.compose.trace.Stores
import com.korilin.samples.compose.trace.draw9Patch
import com.korilin.compose.akit.image.publics.AsyncImageContext
import com.korilin.compose.akit.image.publics.DrawableTranscoder
import com.korilin.compose.akit.image.publics.glideBackground
import com.korilin.compose.akit.image.publics.rememberAsyncImageContext
import com.korilin.samples.compose.trace.ninepatch.NinePatchChunk
import com.korilin.samples.compose.trace.sp
import java.security.MessageDigest

class NinePatchActivity : ComponentActivity() {

    private val url = Stores.ninePatchUrl
    val extension1 = AsyncImageContext(
        this,
        drawableTransformation = listOf(NinePatchDrawableTranscoder),
        ignoreImagePadding = true
    )
    val extension2 = AsyncImageContext(
        this,
        drawableTransformation = listOf(NinePatchDrawableTranscoder),
    )

    inline fun Modifier.background1(
        model: Any?,
        placeholder: Int? = null,
    ) = glideBackground(
        model,
        placeholder,
        contentScale = ContentScale.Crop,
        alignment = Alignment.Center,
        context = extension1
    )
        .padding(5.dp)
        .padding(bottom = 10.dp)

    inline fun Modifier.background2(
        model: Any?,
        placeholder: Int? = null,
    ) = glideBackground(
        model,
        placeholder,
        context = extension2
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {


                Text(
                    text = "Kotlin",
                    color = Color.White,
                    fontSize = 8.dp.sp,
                    modifier = Modifier
                        .draw9Patch(R.drawable.nine_patch_1)
                        .padding(2.dp)
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier
                        .draw9Patch(R.drawable.nine_patch_1)
                        .padding(2.dp)
                )

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier
                        .draw9Patch(R.drawable.nine_patch_1)
                        .padding(2.dp)
                )

                Text("======== Drawable =======")

                Text(
                    text = "Kotlin",
                    color = Color.White,
                    fontSize = 8.dp.sp,
                    modifier = Modifier
                        .background1(
                            model = R.drawable.nine_patch_1,
                        )
                )

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier.background1(
                        model = R.drawable.nine_patch_1,
                    )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Compose & Kotlin & Compose & Kotlin & Compose\"",
                    color = Color.White,
                    modifier = Modifier.background1(
                        model = R.drawable.nine_patch_1,
                    )
                )


                Text("======== NoDpi =======")

                Text(
                    text = "Kotlin",
                    color = Color.White,
                    fontSize = 8.dp.sp,
                    modifier = Modifier
                        .background1(
                            model = R.drawable.nine_patch_1_no,
                        )
                )

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier.background1(
                        model = R.drawable.nine_patch_1_no,
                    )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Compose & Kotlin & Compose & Kotlin & Compose\"",
                    color = Color.White,
                    modifier = Modifier.background1(
                        model = R.drawable.nine_patch_1_no,
                    )
                )


                Text("===============")


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    fontSize = 8.dp.sp,
                    modifier = Modifier
                        .background2(R.drawable.nine_patch_2)
                )

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    fontSize = 8.dp.sp,
                    modifier = Modifier
                        .background2(R.drawable.nine_patch_2)
                )

                Size.Unspecified

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier
                        .background2(R.drawable.nine_patch_2)
                )

                Text("===============")

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    fontSize = 8.dp.sp,
                    modifier = Modifier
                        .background2(
                            model = url,
                        )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier.background2(
                        model = url,
                    )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier.background2(
                        model = url,
                    )
                )
            }
        }
    }
}

@Composable
@Preview
fun Preview() {
    Text(
        text = "Kotlin Kotlin Kotlin Kotlin",
        color = Color.White,
        fontSize = 8.dp.sp,
        modifier = Modifier
            .glideBackground(
                model = R.drawable.nine_patch_2,
                context = rememberAsyncImageContext(
                    drawableTransformation = listOf(NinePatchDrawableTranscoder),
                )
            )
    )
}

object NinePatchDrawableTranscoder : DrawableTranscoder() {

    override fun key(): String {
        return "NinePatchDrawableTranscoder"
    }

    override fun transcode(context: Context, resource: Drawable, width: Int, height: Int): Drawable {
        if (resource !is BitmapDrawable) return resource
        val bitmap = resource.bitmap
        if (!NinePatchChunk.isRawNinePatchBitmap(bitmap)) return resource
        Log.d("NinePatchDrawableTranscoder", "transcode")
        return NinePatchChunk.create9PatchDrawable(context, bitmap, null)
    }
}