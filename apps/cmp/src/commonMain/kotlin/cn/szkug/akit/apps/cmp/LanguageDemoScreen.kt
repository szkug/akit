package cn.szkug.akit.apps.cmp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cn.szkug.akit.resources.runtime.ResourceLocaleManager
import cn.szkug.akit.resources.runtime.painterResource
import cn.szkug.akit.resources.runtime.stringResource

@Composable
fun LanguageDemoPage(onBack: () -> Unit) {
    val languageCode = ResourceLocaleManager.locale.languageCode
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(title = stringResource(Res.strings.language_demo_title), onBack = onBack)
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(Res.strings.language_demo_desc))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = { ResourceLocaleManager.update(null) }) {
                    Text(text = stringResource(Res.strings.language_demo_button_system))
                }
                Button(onClick = { ResourceLocaleManager.update("en") }) {
                    Text(text = stringResource(Res.strings.language_demo_button_en))
                }
                Button(onClick = { ResourceLocaleManager.update("ar") }) {
                    Text(text = stringResource(Res.strings.language_demo_button_ar))
                }
            }

            val currentLabel = when (languageCode) {
                null -> stringResource(Res.strings.language_demo_button_system)
                "en" -> stringResource(Res.strings.language_demo_button_en)
                "ar" -> stringResource(Res.strings.language_demo_button_ar)
                else -> languageCode
            }
            Text(text = stringResource(Res.strings.language_demo_current, currentLabel))
            Text(text = stringResource(Res.strings.language_demo_sample))

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = stringResource(Res.strings.language_demo_image_label))
            Image(
                painter = painterResource(Res.drawable.language_demo),
                contentDescription = "localized-image",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.size(120.dp),
            )
        }
    }
}
