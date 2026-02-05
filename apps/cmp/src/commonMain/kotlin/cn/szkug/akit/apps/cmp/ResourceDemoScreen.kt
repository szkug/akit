package cn.szkug.akit.apps.cmp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.szkug.akit.resources.runtime.colorResource
import cn.szkug.akit.resources.runtime.painterResource
import cn.szkug.akit.resources.runtime.pluralStringResource
import cn.szkug.akit.resources.runtime.stringResource
import cn.szkug.akit.resources.runtime.toDp
import cn.szkug.akit.resources.runtime.toSp

@Composable
fun ResourceDemoPage(onBack: () -> Unit) {
    val languageManager = rememberAppLanguageManager()
    var languageCode by rememberSaveable { mutableStateOf(languageManager.getAppLanguage()) }
    val languageOptions = listOf(
        LanguageOption(null, Res.strings.language_demo_button_system),
        LanguageOption("en", Res.strings.language_demo_button_en),
        LanguageOption("ar", Res.strings.language_demo_button_ar),
        LanguageOption("ru", Res.strings.language_demo_button_ru),
        LanguageOption("sl", Res.strings.language_demo_button_sl),
        LanguageOption("cy", Res.strings.language_demo_button_cy),
    )
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(title = stringResource(Res.labels.resource_demo_title), onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SectionTitle(text = stringResource(Res.strings.language_demo_title))
                Text(text = stringResource(Res.strings.language_demo_desc))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    languageOptions.chunked(3).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            rowOptions.forEach { option ->
                                Button(onClick = {
                                    languageManager.setAppLanguage(option.code)
                                    languageCode = languageManager.getAppLanguage()
                                }) {
                                    Text(text = stringResource(option.labelId))
                                }
                            }
                        }
                    }
                }

                val currentLabel = languageOptions.firstOrNull { it.code == languageCode }?.let {
                    stringResource(it.labelId)
                } ?: (languageCode ?: "")
                Text(text = stringResource(Res.strings.language_demo_current, currentLabel))
                Text(text = stringResource(Res.strings.language_demo_sample))

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = stringResource(Res.strings.language_demo_image_label))
                Image(
                    painter = painterResource(Res.drawable.language_demo),
                    contentDescription = "localized-image",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.size(120.dp),
                )
            }

            item {
                SectionTitle(text = stringResource(Res.labels.values_demo_color_label))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorSwatch(colorResource(Res.colors.color_primary_gradient_session_0))
                    ColorSwatch(colorResource(Res.colors2.color_primary_gradient_session_1))
                    ColorSwatch(colorResource(Res.colors.color_accent))
                }
            }

            item {
                SectionTitle(text = stringResource(Res.labels.values_demo_array_label))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Res.colors.common_primaryGradient.forEach { id ->
                        ColorSwatch(colorResource(id))
                    }
                }
            }

            item {
                SectionTitle(text = stringResource(Res.labels.values_demo_vector_label))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(
                        painter = painterResource(Res.drawable.vector_square),
                        contentDescription = "vector-square",
                        modifier = Modifier.size(48.dp),
                    )
                    Image(
                        painter = painterResource(Res.drawable.vector_circle),
                        contentDescription = "vector-circle",
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            item {
                SectionTitle(text = stringResource(Res.labels.values_demo_plural_label))
                Text(text = stringResource(Res.strings.num_text, 2))
                Text(text = pluralStringResource(Res.strings.common_hours, 1, "1"))
                Text(text = pluralStringResource(Res.strings.common_hours, 5, "5"))
                val userName = "Alex"
                val pluralSamples = listOf(0, 1, 2, 3, 6, 11)
                pluralSamples.forEach { count ->
                    Text(text = pluralStringResource(Res.strings.common_files, count, count, userName))
                }
            }

            item {
                SectionTitle(text = stringResource(Res.labels.values_demo_dimen_label))
                Box(
                    modifier = Modifier
                        .size(Res.dimens.button_width.toDp, Res.dimens.button_height.toDp)
                        .background(colorResource(Res.colors.color_accent))
                )
            }

            item {
                SectionTitle(text = stringResource(Res.labels.values_demo_unit_label))
                Text(text = "button_width.dp = ${Res.dimens.button_width.toDp}")
                Text(text = "button_width.sp = ${Res.dimens.button_width.toSp}")
                Text(text = "sp_text_size.sp = ${Res.dimens.sp_text_size.toSp}")
                Text(text = "pt_text_size.sp = ${Res.dimens.pt_text_size.toSp}")
                Text(text = "in_width.dp = ${Res.dimens.in_width.toDp}")
                Text(text = "mm_height.dp = ${Res.dimens.mm_height.toDp}")
                Text(
                    text = "Sample SP text",
                    fontSize = Res.dimens.sp_text_size.toSp,
                )
                Text(
                    text = "Sample PT line height",
                    fontSize = 16.sp,
                    lineHeight = Res.dimens.pt_text_size.toSp,
                )
            }

            item {
                SectionTitle(text = stringResource(Res.labels.values_demo_title))
                Text(text = stringResource(Res.strings.sample_short))
                Text(text = stringResource(Res.strings.sample_medium))
                Text(text = stringResource(Res.strings.sample_long))
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text)
}

@Composable
private fun ColorSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color)
    )
}

private data class LanguageOption(
    val code: String?,
    val labelId: cn.szkug.akit.resources.runtime.StringResourceId,
)
