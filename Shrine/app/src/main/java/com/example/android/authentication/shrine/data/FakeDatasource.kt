/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.authentication.shrine.data

import com.example.android.authentication.shrine.R
import com.example.android.authentication.shrine.model.Product

/**
 * A fake data source that provides a list of products.
 */
class FakeDatasource {

    companion object {

        /**
         * Loads a list of products.
         *
         * @return A list of [Product] objects.
         */
        fun loadProducts(): List<Product> {
            return listOf(
                Product(R.string.lamp, R.drawable.lamp),
                Product(R.string.dishes, R.drawable.dishes),
                Product(R.string.bag, R.drawable.bag),
                Product(R.string.jacket, R.drawable.jacket),
            )
        }
    }
}
