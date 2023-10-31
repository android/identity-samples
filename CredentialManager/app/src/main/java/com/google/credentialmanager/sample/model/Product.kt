package com.google.credentialmanager.sample.model

import androidx.annotation.StringRes
import androidx.annotation.DrawableRes

data class Product(
    @StringRes val stringResourceId: Int,
    @DrawableRes val imageResourceId: Int
)