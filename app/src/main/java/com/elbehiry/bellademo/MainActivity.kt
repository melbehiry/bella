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

package com.elbehiry.bellademo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.elbehiry.bella.Bella
import com.elbehiry.bella.Duration
import com.elbehiry.bella.ext.bella
import com.elbehiry.bellademo.factory.HomeAlertFactory
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val bellaView by bella(HomeAlertFactory::class)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showAlertView.setOnClickListener {
            Bella.make(showAlert, "something went wrong", Duration.LENGTH_LONG).show()
        }
        showAlert.setOnClickListener {
            bellaView.showAlignedWithParent(bellaAlertContent)
        }
    }
}
