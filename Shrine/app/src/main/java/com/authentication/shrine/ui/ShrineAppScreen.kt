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
package com.authentication.shrine.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.authentication.shrine.R
import com.authentication.shrine.data.FakeDatasource
import com.authentication.shrine.model.Product
import com.authentication.shrine.ui.common.ShrineTopMenu
import com.authentication.shrine.ui.theme.ShrineTheme

/**
 * The main screen of the Shrine app.
 *
 * This composable displays the top menu and a list of products.
 *
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun ShrineAppScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.background)
            .padding(dimensionResource(R.dimen.dimen_standard))
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ShrineTopMenu()

        ProductList(
            productList = FakeDatasource.loadProducts(),
        )
    }
}

/**
 * Displays a list of products in a lazy column.
 *
 * @param modifier The modifier to be applied to the composable.
 * @param productList The list of products to display.
 */
@Composable
fun ProductList(
    productList: List<Product>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.onSecondary)
            .padding(dimensionResource(R.dimen.padding_small))
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(productList) { product ->
            ProductCard(
                product = product,
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_extra_small)),
            )
        }
    }
}

/**
 * Displays a product card with an image and a title.
 *
 * @param product The product to display.
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(product.imageResourceId),
            contentDescription = stringResource(product.stringResourceId),
            modifier.width(100.dp),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = LocalContext.current.getString(product.stringResourceId),
            modifier.padding(dimensionResource(R.dimen.dimen_standard)),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * A preview of the Shrine app screen.
 */
@Preview(showBackground = true)
@Composable
fun ShrineAppScreenPreview() {
    ShrineTheme {
        ShrineAppScreen()
    }
}
