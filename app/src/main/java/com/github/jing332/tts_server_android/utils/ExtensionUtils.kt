package com.github.jing332.tts_server_android.utils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.DisplayMetrics
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.github.jing332.tts_server_android.constant.KeyConst
import java.lang.reflect.ParameterizedType

@Composable
fun Modifier.simpleVerticalScrollbar(
    state: LazyListState,
    width: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.secondary
): Modifier {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val duration = if (state.isScrollInProgress) 150 else 500

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration), label = ""
    )

    return drawWithContent {
        drawContent()

        val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val needDrawScrollbar = state.isScrollInProgress || alpha > 0.0f

        // Draw scrollbar if scrolling or if the animation is still running and lazy column has content
        if (needDrawScrollbar && firstVisibleElementIndex != null) {
            val elementHeight = this.size.height / state.layoutInfo.totalItemsCount

            val scrollbarOffsetY =
                firstVisibleElementIndex * elementHeight + state.firstVisibleItemScrollOffset / 4

//            val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
            val scrollbarHeight = state.layoutInfo.visibleItemsInfo.size * elementHeight

            drawRect(
                color = color,
                topLeft = Offset(this.size.width - width.toPx(), scrollbarOffsetY),
                size = Size(width.toPx(), scrollbarHeight),
                alpha = alpha
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.clickableRipple(
    enabled: Boolean = true,
    role: Role? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    onClickLabel: String? = null,
    onClick: () -> Unit,
) =
    this.combinedClickable(
        enabled = enabled,
        role = role,
        indication = rememberRipple(),
        interactionSource = remember { MutableInteractionSource() },
        onClickLabel = onClickLabel,
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
    )

fun Spanned.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    val spanned = this@toAnnotatedString
    append(spanned.toString())
    getSpans(0, spanned.length, Any::class.java).forEach { span ->
        val start = getSpanStart(span)
        val end = getSpanEnd(span)
        when (span) {
            is StyleSpan -> when (span.style) {
                Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                Typeface.BOLD_ITALIC -> addStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    ), start, end
                )
            }

            is UnderlineSpan -> addStyle(
                SpanStyle(textDecoration = TextDecoration.Underline),
                start,
                end
            )

            is ForegroundColorSpan -> addStyle(
                SpanStyle(color = Color(span.foregroundColor)),
                start,
                end
            )
        }
    }
}

fun Context.registerGlobalReceiver(
    actions: List<String>,
    receiver: BroadcastReceiver
) {
    ContextCompat.registerReceiver(this, receiver, IntentFilter().apply {
        actions.forEach { addAction(it) }
    }, ContextCompat.RECEIVER_EXPORTED)
}

fun View.performLongPress() {
    this.isHapticFeedbackEnabled = true
    this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}

fun Context.startActivity(clz: Class<*>) {
    startActivity(Intent(this, clz).apply { action = Intent.ACTION_VIEW })
}

fun Uri.grantReadWritePermission(contentResolver: ContentResolver) {
    contentResolver.takePersistableUriPermission(
        this,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    )
}

fun Intent.getBinder(): IBinder? {
    val bundle = getBundleExtra(KeyConst.KEY_BUNDLE)
    return bundle?.getBinder(KeyConst.KEY_LARGE_DATA_BINDER)
}

fun Intent.setBinder(binder: IBinder) {
    putExtra(
        KeyConst.KEY_BUNDLE,
        Bundle().apply {
            putBinder(KeyConst.KEY_LARGE_DATA_BINDER, binder)
        })
}

val Int.dp: Int get() = SizeUtils.dp2px(this.toFloat())

val Int.px: Int get() = SizeUtils.px2dp(this.toFloat())

val Context.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(this)

fun ViewGroup.setMarginMatchParent() {
    this.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
}

/**
 * 重启当前 Activity
 */
fun Activity.restart() {
    finish()
    ContextCompat.startActivity(this, intent, null)
}

@Suppress("UNCHECKED_CAST")
fun <T : ViewBinding> Any.inflateBinding(
    inflater: LayoutInflater,
    root: ViewGroup? = null,
    attachToParent: Boolean = false
): T {
    return (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments
        .filterIsInstance<Class<T>>()
        .first()
        .getDeclaredMethod(
            "inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java
        )
        .also { it.isAccessible = true }
        .invoke(null, inflater, root, attachToParent) as T
}

val WindowManager.windowSize: DisplayMetrics
    get() {
        val displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics: WindowMetrics = currentWindowMetrics
            val insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            displayMetrics.widthPixels = windowMetrics.bounds.width() - insets.left - insets.right
            displayMetrics.heightPixels = windowMetrics.bounds.height() - insets.top - insets.bottom
        } else {
            @Suppress("DEPRECATION")
            defaultDisplay.getMetrics(displayMetrics)
        }
        return displayMetrics
    }

@Suppress("DEPRECATION")
val Activity.displayHeight: Int
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            windowMetrics.bounds.height() - insets.bottom - insets.top
        } else
            windowManager.defaultDisplay.height
    }

/**
 * 点击防抖动
 */
fun View.clickWithThrottle(throttleTime: Long = 600L, action: (v: View) -> Unit) {
    this.setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0

        override fun onClick(v: View) {
            if (SystemClock.elapsedRealtime() - lastClickTime < throttleTime) return
            else action(v)

            lastClickTime = SystemClock.elapsedRealtime()
        }
    })
}

/**
 * View 是否在屏幕上可见
 */
fun View.isVisibleOnScreen(): Boolean {
    if (!isShown) {
        return false
    }
    val actualPosition = Rect()
    val isGlobalVisible = getGlobalVisibleRect(actualPosition)
    val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    val screenHeight = Resources.getSystem().displayMetrics.heightPixels
    val screen = Rect(0, 0, screenWidth, screenHeight)
    return isGlobalVisible && Rect.intersects(actualPosition, screen)
}

fun ViewPager2.reduceDragSensitivity(f: Int = 4) {
    val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
    recyclerViewField.isAccessible = true
    val recyclerView = recyclerViewField.get(this) as RecyclerView

    val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
    touchSlopField.isAccessible = true
    val touchSlop = touchSlopField.get(recyclerView) as Int
    touchSlopField.set(recyclerView, touchSlop * f)       // "8" was obtained experimentally
}

/**
 * 绑定返回键回调（建议使用该方法）
 * @param owner Receive callbacks to a new OnBackPressedCallback when the given LifecycleOwner is at least started.
 * This will automatically call addCallback(OnBackPressedCallback) and remove the callback as the lifecycle state changes. As a corollary, if your lifecycle is already at least started, calling this method will result in an immediate call to addCallback(OnBackPressedCallback).
 * When the LifecycleOwner is destroyed, it will automatically be removed from the list of callbacks. The only time you would need to manually call OnBackPressedCallback.remove() is if you'd like to remove the callback prior to destruction of the associated lifecycle.
 * @param onBackPressed 回调方法；返回true则表示消耗了按键事件，事件不会继续往下传递，相反返回false则表示没有消耗，事件继续往下传递
 * @return 注册的回调对象，如果想要移除注册的回调，直接通过调用[OnBackPressedCallback.remove]方法即可。
 */
fun androidx.activity.ComponentActivity.addOnBackPressed(
    owner: LifecycleOwner,
    onBackPressed: () -> Boolean
): OnBackPressedCallback {
    return backPressedCallback(onBackPressed).also {
        onBackPressedDispatcher.addCallback(owner, it)
    }
}

/**
 * 绑定返回键回调，未关联生命周期，建议使用关联生命周期的办法（尤其在fragment中使用，应该关联fragment的生命周期）
 */
fun androidx.activity.ComponentActivity.addOnBackPressed(onBackPressed: () -> Boolean): OnBackPressedCallback {
    return backPressedCallback(onBackPressed).also {
        onBackPressedDispatcher.addCallback(it)
    }
}

private fun androidx.activity.ComponentActivity.backPressedCallback(onBackPressed: () -> Boolean): OnBackPressedCallback {
    return object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!onBackPressed()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }
}