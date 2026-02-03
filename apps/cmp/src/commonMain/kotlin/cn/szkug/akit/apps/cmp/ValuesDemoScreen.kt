package cn.szkug.akit.apps.cmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.szkug.akit.resources.runtime.colorResource
import cn.szkug.akit.resources.runtime.toDp
import cn.szkug.akit.resources.runtime.pluralStringResource
import cn.szkug.akit.resources.runtime.toSp
import cn.szkug.akit.resources.runtime.stringResource

@Composable
fun ValuesDemoPage(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(title = stringResource(Res.labels.values_demo_title), onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(text = stringResource(Res.labels.values_demo_color_label))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorSwatch(colorResource(Res.colors.color_primary_gradient_session_0))
                    ColorSwatch(colorResource(Res.colors2.color_primary_gradient_session_1))
                    ColorSwatch(colorResource(Res.colors.color_accent))
                }
            }

            item {
                Text(text = stringResource(Res.labels.values_demo_array_label))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Res.colors.common_primaryGradient.forEach { id ->
                        ColorSwatch(colorResource(id))
                    }
                }
            }

            item {
                Text(text = stringResource(Res.labels.values_demo_plural_label))
                Text(text = pluralStringResource(Res.strings.common_hours, 1, "1"))
                Text(text = pluralStringResource(Res.strings.common_hours, 5, "5"))
            }

            item {
                Text(text = stringResource(Res.labels.values_demo_dimen_label))
                Box(
                    modifier = Modifier
                        .size(Res.dimens.button_width.toDp, Res.dimens.button_height.toDp)
                        .background(colorResource(Res.colors.color_accent))
                )
            }

            item {
                Text(text = stringResource(Res.labels.values_demo_unit_label))
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

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color)
    )
}
