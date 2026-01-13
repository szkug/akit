package cn.szkug.akit.glide.extensions.blur

import android.graphics.Bitmap
import cn.szkug.renderscript.toolkit.BlurConfig
import cn.szkug.renderscript.toolkit.BlurToolkit
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

class BlurBitmapTransformation(
    private val config: BlurConfig
) : BitmapTransformation() {
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(
            "BlurBitmapTransformation.${config.radius}.${config.repeat}"
                .toByteArray(CHARSET)
        )
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val blurBitmap = BlurToolkit.blur(config, toTransform)
        return blurBitmap
    }

}