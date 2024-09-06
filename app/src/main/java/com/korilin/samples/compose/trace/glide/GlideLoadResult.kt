package com.korilin.samples.compose.trace.glide

import androidx.compose.ui.graphics.painter.Painter

internal sealed interface GlideLoadResult {
    object Error : GlideLoadResult
    data class Success(val painter: Painter) : GlideLoadResult {
        override fun toString(): String {
            return "Success($painter={ painterIntrinsic: ${painter.intrinsicSize}})"
        }
    }
    object Cleared: GlideLoadResult
}