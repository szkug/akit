package cn.szkug.samples.compose.trace.acts

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import cn.szkug.akit.apps.cmp.AkitCmpApp
import cn.szkug.akit.apps.cmp.AndroidAppLanguageStore

class CMPActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AndroidAppLanguageStore.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AkitCmpApp()
        }
    }
}
