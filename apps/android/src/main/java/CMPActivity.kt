package munchkin.sample.acts

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import munchkin.apps.cmp.MunchkinCmpApp
import munchkin.apps.cmp.AndroidAppLanguageStore

class CMPActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AndroidAppLanguageStore.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MunchkinCmpApp()
        }
    }
}
