package cn.szkug.akit.glide.extensions.lottie

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import cn.szkug.akit.lottie.LottieResource
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.Option
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.load.resource.SimpleResource
import com.bumptech.glide.module.LibraryGlideModule
import com.bumptech.glide.signature.ObjectKey
import java.io.InputStream

object LottieDecodeOptions {
    val Enabled: Option<Boolean> = Option.memory(
        "cn.szkug.akit.lottie.Enabled",
        false
    )
    val Iterations: Option<Int> = Option.memory(
        "cn.szkug.akit.lottie.Iterations",
        LottieDrawable.INFINITE
    )
}

@GlideModule
class LottieLibraryGlideModule : LibraryGlideModule() {
    companion object {
        var registerCount: Int = 0
    }

    override fun registerComponents(
        context: Context,
        glide: Glide,
        registry: Registry
    ) {
        registerCount++
        registry
            .append(
                LottieResource::class.java,
                InputStream::class.java,
                LottieResourceModelLoader.Factory(context)
            )
            .prepend(
                InputStream::class.java,
                Drawable::class.java,
                LottieDrawableDecoder()
            )
    }
}

private class LottieResourceModelLoader(
    private val resources: Resources,
) : ModelLoader<LottieResource, InputStream> {

    override fun buildLoadData(
        model: LottieResource,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream>? {
        val resId = model.resource as? Int ?: return null
        return ModelLoader.LoadData(
            ObjectKey(resId),
            LottieResourceDataFetcher(resources, resId)
        )
    }

    override fun handles(model: LottieResource): Boolean = model.resource is Int

    class Factory(
        private val context: Context,
    ) : ModelLoaderFactory<LottieResource, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<LottieResource, InputStream> {
            return LottieResourceModelLoader(context.resources)
        }

        override fun teardown() {}
    }
}

private class LottieResourceDataFetcher(
    private val resources: Resources,
    private val resId: Int,
) : DataFetcher<InputStream> {

    private var stream: InputStream? = null

    override fun loadData(
        priority: com.bumptech.glide.Priority,
        callback: DataFetcher.DataCallback<in InputStream>
    ) {
        try {
            stream = resources.openRawResource(resId)
            callback.onDataReady(stream)
        } catch (exception: Exception) {
            callback.onLoadFailed(exception)
        }
    }

    override fun cleanup() {
        stream?.close()
        stream = null
    }

    override fun cancel() {}

    override fun getDataClass(): Class<InputStream> = InputStream::class.java

    override fun getDataSource(): com.bumptech.glide.load.DataSource =
        com.bumptech.glide.load.DataSource.LOCAL
}

private class LottieDrawableDecoder : ResourceDecoder<InputStream, Drawable> {
    override fun handles(source: InputStream, options: Options): Boolean {
        return options.get(LottieDecodeOptions.Enabled)!!
    }

    override fun decode(
        source: InputStream,
        width: Int,
        height: Int,
        options: Options
    ): com.bumptech.glide.load.engine.Resource<Drawable>? {
        val result = LottieCompositionFactory.fromJsonInputStreamSync(source, null)
        val composition = result.value ?: return null
        val iterations = options.get(LottieDecodeOptions.Iterations)!!
        val drawable = LottieDrawable().apply {
            setComposition(composition)
            repeatMode = LottieDrawable.RESTART
            repeatCount = if (iterations < 0) LottieDrawable.INFINITE else iterations
        }
        return SimpleResource(drawable)
    }
}
