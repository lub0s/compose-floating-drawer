package dev.mudrak.modal.drawer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.R
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.semantics.popup
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.UUID

/**
 * Opens a popup with the given content.
 * The popup is visible as long as it is part of the composition hierarchy.
 *
 * Note:
 * This is highly reduced version of the official Popup composable.
 * Adds the view to the decor view of the window, instead of the window itself.
 */
@Composable
public fun FullscreenPopup(
    onDismiss: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val parentComposition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val popupId = rememberSaveable { UUID.randomUUID() }
    val popupView = remember {
        PopupView(
            onDismiss = onDismiss,
            composeView = view,
            popupId = popupId
        ).apply {
            setContent(parentComposition) {
                Box(Modifier.semantics { this.popup() }) {
                    currentContent()
                }
            }
        }
    }

    DisposableEffect(popupView) {
        popupView.show()
        popupView.updateParameters(
            onDismiss = onDismiss
        )
        onDispose {
            popupView.dismiss()
        }
    }
}

@SuppressLint("ViewConstructor")
private class PopupView(
    private var onDismiss: (() -> Unit)?,
    composeView: View,
    popupId: UUID
) : AbstractComposeView(composeView.context), ViewRootForInspector {

    private val decorView = requireNotNull(
        composeView.context.findActivity()?.window?.decorView as ViewGroup
    ) { "Activity doesn't have a decor" }

    override val subCompositionView: AbstractComposeView get() = this

    init {
        id = android.R.id.content
        ViewTreeLifecycleOwner.set(this, ViewTreeLifecycleOwner.get(composeView))
        ViewTreeViewModelStoreOwner.set(this, ViewTreeViewModelStoreOwner.get(composeView))
        setViewTreeSavedStateRegistryOwner(composeView.findViewTreeSavedStateRegistryOwner())
        // Set unique id for AbstractComposeView. This allows state restoration for the state
        // defined inside the Popup via rememberSaveable()
        setTag(R.id.compose_view_saveable_id_tag, "Popup:$popupId")
    }

    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun show() {
        decorView.addView(
            this,
            MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        requestFocus()
    }

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
    }

    @Composable
    override fun Content() {
        content()
    }

    @Suppress("ReturnCount")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (keyDispatcherState == null) {
                return super.dispatchKeyEvent(event)
            }
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val state = keyDispatcherState
                state?.startTracking(event, this)
                return true
            } else if (event.action == KeyEvent.ACTION_UP) {
                val state = keyDispatcherState
                if (state != null && state.isTracking(event) && !event.isCanceled) {
                    onDismiss?.invoke()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    fun updateParameters(
        onDismiss: (() -> Unit)?
    ) {
        this.onDismiss = onDismiss
    }

    fun dismiss() {
        disposeComposition()
        ViewTreeLifecycleOwner.set(this, null)
        decorView.removeView(this)
    }
}

private fun Context.findActivity(): Activity? {
    var innerContext = this
    while (innerContext is ContextWrapper) {
        if (innerContext is Activity) {
            return innerContext
        }
        innerContext = innerContext.baseContext
    }
    return null
}