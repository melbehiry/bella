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

    }
}