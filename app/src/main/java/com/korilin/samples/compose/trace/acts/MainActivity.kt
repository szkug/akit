package com.korilin.samples.compose.trace.acts

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.trace
import com.bumptech.glide.Glide
import com.korilin.samples.compose.trace.Stores
import com.korilin.samples.compose.trace.theme.ComposetraceTheme

class MainActivity : ComponentActivity() {


    private fun LazyListScope.spacerItem(content: @Composable () -> Unit) {
        item {
            Box(
                modifier = Modifier.padding(vertical = 5.dp)
            ) {
                content()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        trace("GlideInitWith") {
            Glide.with(this)
        }
        setContent {
            ComposetraceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LazyColumn(modifier = Modifier.padding(innerPadding)) {

                        spacerItem { Text(text = this@MainActivity.packageName) }
                        spacerItem { Text(text = "Compose Trace") }


                        spacerItem {
                            Button(
                                onClick = {
                                    val intent =
                                        Intent(this@MainActivity, RoomGridListActivity::class.java)
                                    startActivity(intent)
                                }
                            ) {
                                Text(text = "RoomGridList")
                            }
                        }
                        spacerItem {
                            Button(
                                onClick = {
                                    val intent =
                                        Intent(this@MainActivity, CompareActivity::class.java)
                                    startActivity(intent)
                                }
                            ) {
                                Text(text = "Compare Glide and Coil")
                            }
                        }

                        spacerItem {
                            Button(
                                onClick = {
                                    val intent =
                                        Intent(this@MainActivity, NinePatchActivity::class.java)
                                    startActivity(intent)
                                }
                            ) {
                                Text(text = "Nine Patch Preview")
                            }
                        }

                        spacerItem {
                            Button(
                                onClick = {
                                    val intent =
                                        Intent(this@MainActivity, LayoutOptActivity::class.java)
                                    startActivity(intent)
                                }
                            ) {
                                Text(text = "Layout Opt")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ComposetraceTheme {
        Greeting("Android")
    }
}