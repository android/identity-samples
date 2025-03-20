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
package com.authentication.shrine.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * An [Interceptor] that adds the `User-Agent` and `X-Requested-With` headers to every request.
 *
 * @param userAgent The user agent string to use.
 */
internal class AddHeaderInterceptor(private val userAgent: String) : Interceptor {

    /**
     * Intercepts the request and adds the headers.
     *
     * @param chain The interceptor chain.
     * @return The response with the added headers.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", userAgent)
                .header("X-Requested-With", "XMLHttpRequest")
                .build(),
        )
    }
}
