package com.korilin.samples.compose.trace.stash

import android.os.Build
import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.annotations.ApiStatus.Experimental
import kotlin.math.max
import kotlin.math.min

@Experimental
val LocalImeInsetHolder = compositionLocalOf<ImeInsetHolder> {
    throw IllegalStateException("No provide ImeInsetHolder")
}

@Composable
@Experimental
fun rememberCurrentImeInsetHolder(
    remember: IMEHolderRemember?
): ImeInsetHolder {
    val view = LocalView.current

    return remember(view, remember) {
        val holder = ImeInsetHolder(remember)
        holder.init(view)
        holder
    }
}

interface IMEHolderRemember {
    fun getHeight(): Int

    fun saveHeight(height: Int)
}

class ImeInsetHolder(
    private val remember: IMEHolderRemember?
) : SoftKeyboardInsetUpdater {

    val height = MutableStateFlow(0)
    val visible = MutableStateFlow(false)
    val prepareAnimation = MutableStateFlow(false to 0L)

    var rememberMaxHeight: Int = 0
        private set

    private val listener = SoftKeyboardInsetListener(this)

    fun animating() = prepareAnimation.value.second > 0L

    // use RootView is recommended
    fun init(view: View) {

        rememberMaxHeight = remember?.getHeight() ?: 0

        // add listeners
        ViewCompat.setOnApplyWindowInsetsListener(view, listener)

        if (view.isAttachedToWindow) {
            view.requestApplyInsets()
        }
        view.addOnAttachStateChangeListener(listener)

        ViewCompat.setWindowInsetsAnimationCallback(view, listener)
    }


    private fun WindowInsetsCompat.imeInset() =
        getInsets(WindowInsetsCompat.Type.ime())

    private fun WindowInsetsCompat.imeVisible() =
        isVisible(WindowInsetsCompat.Type.ime())

    override fun prepare(duration: Long) {
        prepareAnimation.value = !visible.value to duration
    }

    override fun update(compat: WindowInsetsCompat, end: Boolean) {
        val inset = compat.imeInset()
        // 有些软键盘高度和动画回调高度不一致，可能会回弹，所以要控制这个高度
        height.value = if (rememberMaxHeight <= 0) inset.bottom
        else min(inset.bottom, rememberMaxHeight)
        if (end) {
            prepareAnimation.value = visible.value to 0

            // end 的情况确保正确宽度
            height.value = inset.bottom
            visible.value = compat.imeVisible()
            val remember = max(height.value, rememberMaxHeight)
            if (remember != rememberMaxHeight) {
                rememberMaxHeight = remember
                this.remember?.saveHeight(remember)
            }
        }
    }
}

private interface SoftKeyboardInsetUpdater {

    fun prepare(duration: Long)

    fun update(compat: WindowInsetsCompat, end: Boolean)

}

/**
 * @see androidx.compose.foundation.layout.WindowInsetsHolder
 * @see androidx.compose.foundation.layout.InsetsListener
 */
private class SoftKeyboardInsetListener(
    private val updater: SoftKeyboardInsetUpdater,
) : Runnable,
    OnApplyWindowInsetsListener,
    OnAttachStateChangeListener,
    WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {


    /**
     * When [android.view.WindowInsetsController.controlWindowInsetsAnimation] is called,
     * the [onApplyWindowInsets] is called after [onPrepare] with the target size. We
     * don't want to report the target size, we want to always report the current size,
     * so we must ignore those calls. However, the animation may be canceled before it
     * progresses. On R, it won't make any callbacks, so we have to figure out whether
     * the [onApplyWindowInsets] is from a canceled animation or if it is from the
     * controlled animation. When [prepared] is `true` on R, we post a callback to
     * set the [onApplyWindowInsets] insets value.
     */
    var prepared = false

    /**
     * `true` if there is an animation in progress.
     */
    var runningAnimation = false

    var savedInsets: WindowInsetsCompat? = null


    // OnAttachStateChangeListener
    override fun onViewAttachedToWindow(view: View) {
        view.requestApplyInsets()
    }

    override fun onViewDetachedFromWindow(view: View) {

    }

    // OnApplyWindowInsetsListener
    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        // Keep track of the most recent insets we've seen, to ensure onEnd will always use the
        // most recently acquired insets
        savedInsets = insets
        // updater.updateImeAnimationTarget(insets)
        if (prepared) {
            // There may be no callback on R if the animation is canceled after onPrepare(),
            // so we won't know if the onPrepare() was canceled or if this is an
            // onApplyWindowInsets() after the cancellation. We'll just post the value
            // and if it is still preparing then we just use the value.
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) view.post(this)
        } else if (!runningAnimation) {
            // If an animation is running, rely on onProgress() to update the insets
            // On APIs less than 30 where the IME animation is backported, this avoids reporting
            // the final insets for a frame while the animation is running.
            // updater.updateImeAnimationSource(insets)
            updater.update(insets, false)
        }
        return insets
    }

    // WindowInsetsAnimationCompat
    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        prepared = true
        runningAnimation = true

        updater.prepare(animation.durationMillis)
        super.onPrepare(animation)
    }

    override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: WindowInsetsAnimationCompat.BoundsCompat
    ): WindowInsetsAnimationCompat.BoundsCompat {
        prepared = false
        return super.onStart(animation, bounds)
    }

    override fun onProgress(
        insets: WindowInsetsCompat,
        runningAnimations: MutableList<WindowInsetsAnimationCompat>
    ): WindowInsetsCompat {
        updater.update(insets, false)
        return insets
    }

    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        prepared = false
        runningAnimation = false
        val insets = savedInsets
        if (animation.durationMillis != 0L && insets != null) {
            // updater.updateImeAnimationSource(insets)
            // updater.updateImeAnimationTarget(insets)
            updater.update(insets, true)
        }
        savedInsets = null
        super.onEnd(animation)
    }

    /**
     * On [R], we don't receive the [onEnd] call when an animation is canceled, so we post
     * the value received in [onApplyWindowInsets] immediately after [onPrepare]. If [onProgress]
     * or [onEnd] is received before the runnable executes then the value won't be used. Otherwise,
     * the [onApplyWindowInsets] value will be used. It may have a janky frame, but it is the best
     * we can do.
     */
    override fun run() {
        if (prepared) {
            prepared = false
            runningAnimation = false
            savedInsets?.let {
                // updater.updateImeAnimationSource(it)
                updater.update(it, true)
                savedInsets = null
            }
        }
    }
}
