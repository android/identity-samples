package com.google.credentialmanager.sample.data

import com.google.credentialmanager.sample.R
import com.google.credentialmanager.sample.model.Product

class Datasource() {
    companion object {

        fun loadProducts(): List<Product> {
            return listOf<Product>(
                Product(R.string.lamp, R.drawable.lamp),
                Product(R.string.dishes, R.drawable.dishes),
                Product(R.string.bag, R.drawable.bag),
                Product(R.string.jacket, R.drawable.jacket)
            )
        }
    }
}