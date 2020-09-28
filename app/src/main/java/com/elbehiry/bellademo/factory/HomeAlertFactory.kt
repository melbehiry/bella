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

package com.elbehiry.bellademo.factory

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.elbehiry.bella.Bella
import com.elbehiry.bellademo.R

const val BellaViewDuration: Long = 2000

class HomeAlertFactory : Bella.Factory() {
    override fun create(context: Context, lifecycleOwner: LifecycleOwner): Bella {
        return Bella.Builder(context)
            .setButtonVisible(true)
            .setButtonTextColorResource(R.color.action_button_color)
            .setButtonTextSize(12f)
            .setButtonText(context.getString(R.string.retry))
            .setTextResource(R.string.alert_content)
            .setTextSize(12f)
            .setLifecycleOwner(lifecycleOwner)
            .setAutoDismissDuration(BellaViewDuration)
            .setBackgroundColorResource(R.color.error_red)
            .build()
    }
}
