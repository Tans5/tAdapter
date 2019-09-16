package com.example.tadapter.service

import com.example.tadapter.model.Product
import io.reactivex.Single
import java.util.concurrent.TimeUnit

/**
 *
 * author: pengcheng.tan
 * date: 2019-09-16
 */

class ProductService {

    fun getProducts(page: Int = 1): Single<List<Product>> = Single.fromCallable {
        List(20) { index ->
            Product(
                id = page * 1000 + index,
                name = "Product Name",
                price = 99.99)
        }
    }.delay(1000, TimeUnit.MILLISECONDS)

}