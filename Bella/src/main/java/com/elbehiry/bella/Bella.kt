package com.elbehiry.bella

import android.content.Context
import androidx.lifecycle.LifecycleObserver

class Bella(
    private val context: Context,
    private val builder: Builder
) : LifecycleObserver {

    class Builder(private val context: Context) {

    }
}