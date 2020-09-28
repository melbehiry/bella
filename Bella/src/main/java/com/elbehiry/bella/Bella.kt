/*
 * Copyright (C) 2020 elbehiry
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.elbehiry.bella

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListenerAdapter
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.elbehiry.bella.annotation.Dp
import com.elbehiry.bella.annotation.Sp
import com.elbehiry.bella.ext.applyTextForm
import com.elbehiry.bella.ext.contextColor
import com.elbehiry.bella.ext.contextDrawable
import com.elbehiry.bella.ext.dimen
import com.elbehiry.bella.ext.displaySize
import com.elbehiry.bella.ext.dp2Px
import com.elbehiry.bella.listeners.OnBellaButtonClickListener
import com.elbehiry.bella.listeners.OnBellaClickListener
import com.elbehiry.bella.listeners.OnBellaDismissListener
import com.elbehiry.bella.listeners.OnBellaShownListener

@DslMarker
annotation class BellaDsl

/** creates an instance of [Bella] by [Bella.Builder] using kotlin dsl. */
@BellaDsl
inline fun createBella(context: Context, block: Bella.Builder.() -> Unit): Bella =
    Bella.Builder(context).apply(block).build()

/** Bella implements showing and dismissing notification top alert with text, action button and animations. */
@Suppress("@MemberVisibilityCanBePrivate")
class Bella(
  private val context: Context,
  private val builder: Builder
) : LifecycleObserver {

    var isShowing = false
        private set
    private var destroyed: Boolean = false

    private var onBellaClickListener: OnBellaClickListener? = null
    private var onBellaButtonClickListener: OnBellaButtonClickListener? = null
    private var onBellaDismissListener: OnBellaDismissListener? = null
    private var onBellaShownListener: OnBellaShownListener? = null

    private val messageDismiss = 1
    private var mShowAndHideTime: Long = 200

    private val fastOutSlowInInterpolator: Interpolator by lazy {
        FastOutSlowInInterpolator()
    }

    private var bellaItemView: BellaItemView? = null

    private val handler: Handler = Handler(Looper.getMainLooper(), Handler.Callback { message ->
        when (message.what) {
            messageDismiss -> {
                hideView()
                true
            }
            else -> false
        }
    })

    /**
     * show bella view with frameLayout as parent viewGroup.
     *
     * @param parent container layout.
     */
    fun show(parent: FrameLayout) {
        if (!isShowing && !destroyed) {
            this.isShowing = true
            bellaItemView = BellaItemView(parent, context)
            initWithBuilder()
            show(parent) {
                addLayoutChangesOnRootView()
                animateRootViewIn()
                if (builder.autoDismissDuration != NO_LONG_VALUE) {
                    dismissWithDelay(builder.autoDismissDuration)
                }
            }
        }
    }

    /** hide the bella view and remove handler call-back. */
    fun dismiss() {
        handler.removeCallbacksAndMessages(bellaItemView)
        hideView()
    }

    private inline fun show(parent: ViewGroup, crossinline block: () -> Unit) {
        parent.addView(getBindingView().root)
        block()
    }

    private fun addLayoutChangesOnRootView() {
        getBindingView().root.addOnAttachStateChangeListener(object :
            View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View?) {
            }

            override fun onViewAttachedToWindow(v: View?) {
                onViewHidden()
            }
        })
    }

    private fun animateRootViewIn() {
        if (ViewCompat.isLaidOut(getBindingView().root)) {
            animateViewIn()
        } else {
            getBindingView().root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                animateViewIn()
            }
        }
    }

    private fun initWithBuilder() {
        initializeBackground()
        initializeBellaContent()
        handleLifeCycleObserver()
        initializeBellaListeners()
        initializeBellaLayout()
    }

    private fun initializeBackground() {
        with(bellaItemView?.binding!!.bellaCard) {
            alpha = builder.alpha
            cardElevation = builder.elevation

            if (builder.backgroundDrawable == null) {
                setBackgroundColor(builder.backgroundColor)
                radius = builder.cornerRadius
            } else {
                background = builder.backgroundDrawable
            }
        }
    }

    private fun initializeBellaContent() {
        with(getBindingView().bellaDetail) {
            if (builder.padding != NO_INT_VALUE) {
                setPadding(builder.padding, builder.padding, builder.padding, builder.padding)
            }
        }
    }

    private fun initializeBellaListeners() {
        this.onBellaClickListener = builder.onBellaClickListener
        this.onBellaButtonClickListener = builder.onBellaButtonClickListener
        this.onBellaDismissListener = builder.onBellaDismissListener
        this.onBellaShownListener = builder.onBellaShownListener

        getBindingView().root.setOnClickListener {
            this.onBellaClickListener?.onBellaClick(it)
        }
    }

    private fun initializeBellaLayout() {
        initializeText()
        initializeButton()
    }

    private fun initializeText() {
        with(getBindingView().bellaText) {
            builder.textForm?.let {
                applyTextForm(it)
            } ?: applyTextForm(textForm(context) {
                setText(builder.text)
                setTextSize(builder.textSize)
                setTextColor(builder.textColor)
                setTextIsHtml(builder.textIsHtml)
                setTextGravity(builder.textGravity)
                setTextTypeface(builder.textTypeface)
                setTextTypeface(builder.textTypefaceObject)
                setMaxLines(builder.textMaxLines)
            })
            val widthSpec =
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            val heightSpec =
                View.MeasureSpec.makeMeasureSpec(
                    context.displaySize().y,
                    View.MeasureSpec.UNSPECIFIED
                )
            measure(widthSpec, heightSpec)
        }
    }

    private fun initializeButton() {
        with(getBindingView().bellaButton) {
            if (builder.buttonVisible) {
                text = builder.buttonText
                if (builder.buttonTextColor != NO_INT_VALUE) {
                    setTextColor(builder.buttonTextColor)
                }
                textSize = builder.buttonTextSize
                if (builder.buttonBackgroundColor != NO_INT_VALUE) {

                    val backgroundTintList: ColorStateList =
                        AppCompatResources.getColorStateList(context, android.R.color.holo_red_dark)
                    ViewCompat.setBackgroundTintList(
                        getBindingView().bellaButton,
                        backgroundTintList
                    )
                }
                if (builder.buttonPadding != 0) {
                    setPadding(
                        builder.buttonPadding,
                        builder.buttonPadding,
                        builder.buttonPadding,
                        builder.buttonPadding
                    )
                }
                initializeButtonClickListener()
            } else {
                getBindingView().bellaButton.visibility = View.GONE
            }
        }
    }

    private fun initializeButtonClickListener() {
        getBindingView().bellaButton.setOnClickListener {
            this.onBellaButtonClickListener?.onBellaButtonClicked(it)
        }
    }

    private fun animateViewIn() {
        getBindingView().root.translationY = (-getBindingView().root.height).toFloat()
        ViewCompat.animate(getBindingView().root)
            .translationY(NO_Float_VALUE)
            .setInterpolator(fastOutSlowInInterpolator)
            .setDuration(mShowAndHideTime)
            .setListener(object : ViewPropertyAnimatorListenerAdapter() {
                override fun onAnimationStart(view: View?) {
                }

                override fun onAnimationEnd(view: View?) {
                    onBellaShownListener?.onBellaShown()
                }
            })
            .start()
    }

    private fun animateViewOut() {
        ViewCompat.animate(getBindingView().root)
            .translationY((-getBindingView().root.height).toFloat())
            .setInterpolator(fastOutSlowInInterpolator)
            .setDuration(mShowAndHideTime)
            .setListener(object : ViewPropertyAnimatorListenerAdapter() {
                override fun onAnimationStart(view: View?) {
                }

                override fun onAnimationEnd(view: View?) {
                    onViewHidden()
                }
            })
            .start()
    }

    private fun hideView() {
        if (this.isShowing) {
            this.isShowing = false
            if (getBindingView().root.visibility != View.VISIBLE) {
                onViewHidden()
            } else {
                animateViewOut()
            }
        }
    }

    private fun onViewHidden() {
        val parent = getBindingView().root.parent
        if (parent is ViewGroup) {
            parent.removeView(getBindingView().root)
        }
        onBellaDismissListener?.onBellaDismiss()
    }

    private fun dismissWithDelay(delay: Long) {
        handler.sendMessageDelayed(
            Message.obtain(
                handler, messageDismiss, bellaItemView
            ), delay
        )
    }

    private fun getBindingView() = bellaItemView?.binding!!

    private fun handleLifeCycleObserver() {
        builder.lifecycleOwner?.lifecycle?.addObserver(this)
    }

    /** dismiss automatically when lifecycle owner is destroyed. */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        destroyed = true
        dismiss()
    }

    /** Builder class for creating [Bella]. */
    @BellaDsl
    class Builder(private val context: Context) {

        @JvmField
        @Dp
        var height: Int = NO_INT_VALUE

        @JvmField
        @Dp
        var padding: Int = NO_INT_VALUE

        @JvmField
        var buttonVisible: Boolean = false

        @JvmField
        var buttonText: CharSequence = ""

        @JvmField
        @ColorInt
        var buttonBackgroundColor: Int = NO_INT_VALUE

        @JvmField
        @ColorInt
        var buttonTextColor: Int = NO_INT_VALUE

        @JvmField
        @Sp
        var buttonTextSize: Float = 16f

        @JvmField
        var buttonPadding: Int = 0

        @JvmField
        @ColorInt
        var backgroundColor: Int = Color.BLACK

        @JvmField
        var backgroundDrawable: Drawable? = null

        @JvmField
        @Dp
        var space: Int = 0

        @JvmField
        @Dp
        var cornerRadius: Float = context.dp2Px(5).toFloat()

        @JvmField
        var text: CharSequence = ""

        @JvmField
        @ColorInt
        var textColor: Int = Color.WHITE

        @JvmField
        var textIsHtml: Boolean = false

        @JvmField
        @Sp
        var textSize: Float = 12f

        @JvmField
        var textTypeface: Int = Typeface.NORMAL

        @JvmField
        var textMaxLines: Int = 2

        @JvmField
        var textTypefaceObject: Typeface? = null

        @JvmField
        var textGravity: Int = Gravity.START

        @JvmField
        var textForm: TextForm? = null

        @JvmField
        @FloatRange(from = 0.0, to = 1.0)
        var alpha: Float = 1f

        @JvmField
        var elevation: Float = context.dp2Px(2f)

        @JvmField
        var onBellaClickListener: OnBellaClickListener? = null

        @JvmField
        var onBellaShownListener: OnBellaShownListener? = null

        @JvmField
        var onBellaButtonClickListener: OnBellaButtonClickListener? = null

        @JvmField
        var onBellaDismissListener: OnBellaDismissListener? = null

        @JvmField
        var autoDismissDuration: Long = NO_LONG_VALUE

        @JvmField
        var lifecycleOwner: LifecycleOwner? = null

        /** sets the height size. */
        fun setHeight(@Dp value: Int): Builder = apply { this.height = context.dp2Px(value) }

        /** sets the height size using dimension resources. */
        fun setHeightResource(@DimenRes value: Int): Builder = apply {
            this.height = context.dimen(value)
        }

        /** sets the padding on all directions. */
        fun setPadding(@Dp value: Int): Builder = apply { this.padding = context.dp2Px(value) }

        /** sets the padding on all directions using dimension resource. */
        fun setPaddingResource(@DimenRes value: Int): Builder = apply {
            this.padding = context.dimen(value)
        }

        /** sets the visibility of the button. */
        fun setButtonVisible(value: Boolean): Builder = apply { this.buttonVisible = value }

        /** sets a color of the button background. */
        fun setButtonBackgroundColor(@ColorInt value: Int): Builder =
            apply { this.buttonBackgroundColor = value }

        /** sets a color of the button background using a resource. */
        fun setButtonBackgroundColorResource(@ColorRes value: Int): Builder = apply {
            this.buttonBackgroundColor = context.contextColor(value)
        }

        /** sets a color of the button text color. */
        fun setButtonTextColor(@ColorInt value: Int): Builder =
            apply { this.buttonTextColor = value }

        /** sets a color of the button text color using a resource. */
        fun setButtonTextColorResource(@ColorRes value: Int): Builder = apply {
            this.buttonTextColor = context.contextColor(value)
        }

        /** sets a button text size. */
        fun setButtonTextSize(@Dp value: Float): Builder =
            apply { this.buttonTextSize = value }

        /** sets a button text size from resource. */
        fun setButtonTextSizeResource(@DimenRes value: Int): Builder = apply {
            this.buttonTextSize = context.dimen(value).toFloat()
        }

        /** sets a space between text and button. */
        fun setSpace(@Dp value: Int): Builder = apply { this.space = context.dp2Px(value) }

        /** sets a space between text and button from resource. */
        fun setSpaceResource(@DimenRes value: Int): Builder = apply {
            this.space = context.dimen(value)
        }

        /** sets the button padding on all directions. */
        fun setButtonPadding(@Dp value: Int): Builder = apply {
            this.buttonPadding = context.dp2Px(value)
        }

        /** sets the content background color. */
        fun setBackgroundColor(@ColorInt value: Int): Builder =
            apply { this.backgroundColor = value }

        /** sets the content background color from resource. */
        fun setBackgroundColorResource(@ColorRes value: Int): Builder = apply {
            this.backgroundColor = context.contextColor(value)
        }

        /** sets the content background drawable. */
        fun setBackgroundDrawable(value: Drawable?): Builder = apply {
            this.backgroundDrawable = value?.mutate()
        }

        /** sets the content background drawable from resource. */
        fun setBackgroundDrawableResource(@DrawableRes value: Int): Builder = apply {
            this.backgroundDrawable = context.contextDrawable(value)?.mutate()
        }

        /** sets the content corner radius. */
        fun setCornerRadius(@Dp value: Float): Builder = apply {
            this.cornerRadius = context.dp2Px(value)
        }

        /** sets the content corner radius from resource. */
        fun setCornerRadiusResource(@DimenRes value: Int): Builder = apply {
            this.cornerRadius = context.dimen(value).toFloat()
        }

        /** sets the button text. */
        fun setButtonText(value: CharSequence): Builder = apply { this.buttonText = value }

        /** sets a text max lines. */
        fun setMaxLines(value: Int): Builder = apply {
            this.textMaxLines = value
        }

        /** sets the main text content of the bella view. */
        fun setText(value: CharSequence): Builder = apply { this.text = value }

        /** sets the main text content of the bella view from resource. */
        fun setTextResource(@StringRes value: Int): Builder = apply {
            this.text = context.getString(value)
        }

        /** sets the main text color. */
        fun setTextColor(@ColorInt value: Int): Builder = apply { this.textColor = value }

        /** sets the main text color from resource. */
        fun setTextColorResource(@ColorRes value: Int): Builder = apply {
            this.textColor = context.contextColor(value)
        }

        /** sets whether the text will be parsed as HTML (using Html.fromHtml(..)) */
        fun setTextIsHtml(value: Boolean): Builder = apply { this.textIsHtml = value }

        /** sets the size of the main text content. */
        fun setTextSize(@Sp value: Float): Builder = apply { this.textSize = value }

        /** sets the typeface of the main text content. */
        fun setTextTypeface(value: Int): Builder = apply { this.textTypeface = value }

        /** sets the typeface of the main text content. */
        fun setTextTypeface(value: Typeface): Builder = apply { this.textTypefaceObject = value }

        /** sets gravity of the text. */
        fun setTextGravity(value: Int): Builder = apply {
            this.textGravity = value
        }

        /** applies [TextForm] attributes to the main text content. */
        fun setTextForm(value: TextForm): Builder = apply { this.textForm = value }

        /** sets the alpha value to the container. */
        fun setAlpha(@FloatRange(from = 0.0, to = 1.0) value: Float): Builder = apply {
            this.alpha = value
        }

        /** sets the elevation to the popup. */
        fun setElevation(@Dp value: Int): Builder = apply {
            this.elevation = context.dp2Px(value).toFloat()
        }

        /** sets the elevation to the popup using dimension resource. */
        fun setElevationResource(@DimenRes value: Int): Builder = apply {
            this.elevation = context.dimen(value).toFloat()
        }

        /**
         * sets the [LifecycleOwner] for dismissing automatically when the [LifecycleOwner] is destroyed.
         * It will prevents memory leak
         */
        fun setLifecycleOwner(value: LifecycleOwner?): Builder =
            apply { this.lifecycleOwner = value }

        /** sets duration value to dismiss the bella view. */
        fun setAutoDismissDuration(value: Long): Builder = apply {
            this.autoDismissDuration = value
        }

        /** sets a [OnBellaClickListener] to the bella view. */
        fun setOnBellaClickListener(value: OnBellaClickListener): Builder = apply {
            this.onBellaClickListener = value
        }

        /** sets a [OnBellaClickListener] to the bella view. */
        fun setOnBellaClickListener(unit: (View) -> Unit): Builder = apply {
            this.onBellaClickListener = object : OnBellaClickListener {
                override fun onBellaClick(view: View) {
                    unit(view)
                }
            }
        }

        /**
         * sets a [OnBellaShownListener] to the bella view.
         * called when the view is appear.
         */
        fun setOnBellaShownListener(value: OnBellaShownListener): Builder = apply {
            this.onBellaShownListener = value
        }

        /**
         * sets a [OnBellaShownListener] to the bella view.
         * called when the view is appear.
         */
        fun setOnBellaShownListener(unit: () -> Unit): Builder = apply {
            this.onBellaShownListener = object : OnBellaShownListener {
                override fun onBellaShown() {
                    unit()
                }
            }
        }

        /**
         * sets a [OnBellaButtonClickListener] to the bella view.
         * called if the button clicked.
         */
        fun setOnBellaButtonClickListener(value: OnBellaButtonClickListener): Builder = apply {
            this.onBellaButtonClickListener = value
        }

        /**
         * sets a [OnBellaButtonClickListener] to the bella view.
         * called if the button clicked.
         */
        fun setOnBellaButtonClickListener(unit: (View) -> Unit): Builder = apply {
            this.onBellaButtonClickListener = object : OnBellaButtonClickListener {
                override fun onBellaButtonClicked(view: View) {
                    unit(view)
                }
            }
        }

        /**
         * sets a [OnBellaDismissListener] to the bella view.
         * called when the view is dismissed.
         */
        fun setOnBellaDismissListener(value: OnBellaDismissListener): Builder = apply {
            this.onBellaDismissListener = value
        }

        /**
         * sets a [OnBellaDismissListener] to the bella view.
         * called when the view is dismissed.
         */
        fun setOnBellaDismissListener(unit: () -> Unit): Builder = apply {
            this.onBellaDismissListener = object : OnBellaDismissListener {
                override fun onBellaDismiss() {
                    unit()
                }
            }
        }

        fun build(): Bella = Bella(context, this@Builder)
    }

    /**
     * An abstract factory class for creating [Bella] instance.
     *
     * A factory implementation class must have a non-argument constructor.
     */
    abstract class Factory {

        /** returns an instance of [Bella]. */
        abstract fun create(context: Context, lifecycleOwner: LifecycleOwner): Bella
    }
}
