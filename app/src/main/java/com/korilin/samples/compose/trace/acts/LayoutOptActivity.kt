@file:Suppress("NOTHING_TO_INLINE")

package com.korilin.samples.compose.trace.acts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.trace
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi

class LayoutOptActivity : ComponentActivity() {

    private val bf = SimpleBfCl()
    private val opt = SimpleOptCl()

    private val text = mutableStateOf(StringBuilder().apply {
        repeat(10000) { append("K") }
    }.toString())

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
                bf.Content()
                Spacer(modifier = Modifier.height(10.dp))
                opt.Content()

                Spacer(modifier = Modifier.height(10.dp))
                bf.Content()
                Spacer(modifier = Modifier.height(10.dp))
                opt.Content()

                Spacer(modifier = Modifier.height(10.dp))
                bf.Content()
                Spacer(modifier = Modifier.height(10.dp))
                opt.Content()

            }
        }
    }
}


private val framePd = 5.dp
private val tags = List(3) { "" }

class SimpleBfCl() {

    @Preview
    @Composable
    private fun Preview() = Content()

    @Composable
    fun Content() = trace("SimpleBfCl") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),

            contentAlignment = Alignment.Center
        ) {
            CBFrame(
                modifier = Modifier
                    .matchParentSize()
                    .padding(framePd)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(framePd),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TopSlot_Bulletin()
                    TopSlot_ActTag()
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Box(contentAlignment = Alignment.Center) {
                            InfoSub_Avatar()
                            InfoSub_Wear()
                        }

                        InfoSub_Identify()
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        InfoSub_Name()
                        InfoSub_Notice()
                        InfoSub_Tags()
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        EndArea_Follow()
                        EndArea_Go()
                    }
                }
            }
            CFFrame(
                modifier = Modifier.matchParentSize()
            )
        }
    }

}

class SimpleOptCl() {
    @Preview
    @Composable
    private fun Preview() = Content()

    @Composable
    fun Content() = trace("SimpleOptCl") {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {

            val (cbRef, cfRef, avatarRef, wearRef, idRef, nameRef, noticeRef, tagsRef, bulletinRef, actTagRef, goRef, followRef) = createRefs()

            CBFrame(
                modifier = Modifier
                    .constrainAs(cbRef) {
                        top.linkTo(parent.top, framePd)
                        bottom.linkTo(parent.bottom, framePd)
                        start.linkTo(parent.start, framePd)
                        end.linkTo(parent.end, framePd)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
            )

            TopSlot_Bulletin(
                modifier = Modifier.constrainAs(bulletinRef) {
                    top.linkTo(cbRef.top)
                    start.linkTo(cbRef.start)
                }
            )
            TopSlot_ActTag(
                modifier = Modifier.constrainAs(actTagRef) {
                    top.linkTo(cbRef.top)
                    end.linkTo(cbRef.end)
                }
            )

            InfoSub_Avatar(
                modifier = Modifier.constrainAs(avatarRef) {
                    centerTo(wearRef)
                }
            )
            InfoSub_Wear(
                modifier = Modifier.constrainAs(wearRef) {
                    top.linkTo(bulletinRef.bottom)
                    start.linkTo(cbRef.start)
                }
            )
            InfoSub_Identify(modifier = Modifier.constrainAs(idRef) {
                top.linkTo(wearRef.bottom)
                bottom.linkTo(parent.bottom, framePd)
                centerHorizontallyTo(wearRef)
            })

            InfoSub_Name(
                modifier = Modifier.constrainAs(nameRef) {
                    top.linkTo(bulletinRef.bottom, 4.dp)
                    start.linkTo(wearRef.end, 10.dp)
                }
            )
            InfoSub_Notice(
                modifier = Modifier.constrainAs(noticeRef) {
                    top.linkTo(nameRef.bottom, 4.dp)
                    start.linkTo(nameRef.start)
                }
            )

            InfoSub_Tags(
                modifier = Modifier.constrainAs(tagsRef) {
                    top.linkTo(noticeRef.bottom, 4.dp)
                    start.linkTo(wearRef.end, 10.dp)
                }
            )

            EndArea_Follow(
                modifier = Modifier.constrainAs(followRef) {
                    centerVerticallyTo(cbRef)
                    end.linkTo(goRef.start, 10.dp)
                }
            )
            EndArea_Go(
                modifier = Modifier.constrainAs(goRef) {
                    centerVerticallyTo(cbRef)
                    end.linkTo(cbRef.end)
                })

            CFFrame(
                modifier = Modifier
                    .constrainAs(cfRef) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
            )
        }
    }
}

@Composable
private inline fun CFFrame(
    modifier: Modifier = Modifier
) {
    Spacer(modifier = modifier.border(2.dp, color = Color.Yellow))
}

@Composable
private inline fun CBFrame(
    modifier: Modifier = Modifier
) {
    Spacer(modifier = modifier.background(Color.White))
}

@Composable
private inline fun TopSlot_Bulletin(
    modifier: Modifier = Modifier
) {
    Text(text = "top slot bulletin", modifier = modifier)
}

@Composable
private inline fun TopSlot_ActTag(
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier then Modifier
            .size(40.dp, 20.dp)
            .background(Color.Blue)
    )
}

@Composable
private inline fun InfoSub_Avatar(
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier then Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Color.Black)
    )
}

@Composable
private inline fun InfoSub_Wear(
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier then Modifier
            .size(70.dp)
            .border(width = 2.dp, color = Color.Cyan, shape = CircleShape)
    )
}

@Composable
private inline fun InfoSub_Identify(
    modifier: Modifier = Modifier
) {
    Text(
        text = "InfoSub_Identify",
        modifier = modifier then Modifier.widthIn(max = 70.dp),
        maxLines = 1
    )
}

@Composable
private inline fun InfoSub_Name(
    modifier: Modifier = Modifier
) {
    Text(text = "InfoSub_Name", modifier = modifier)
}

@Composable
private inline fun InfoSub_Notice(
    modifier: Modifier = Modifier
) {
    Text(text = "InfoSub_Notice", modifier = modifier)
}


@Composable
private inline fun InfoSub_Tags(
    modifier: Modifier = Modifier
) {

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        tags.fastForEach {
            Row(modifier, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                Spacer(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color.Red)
                )
                Text(text = "tag")
            }
        }
    }
}

@Composable
private inline fun EndArea_Go(
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier then Modifier
            .size(20.dp)
            .background(Color.Green)
    )
}

@Composable
private inline fun EndArea_Follow(
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier then Modifier
            .size(20.dp)
            .background(Color.Green)
    )
}