package com.example.tadapter

import com.example.tadapter.core.BaseViewModel
import com.example.tadapter.core.InputOwner
import com.example.tadapter.model.Product
import com.example.tadapter.service.ProductService
import com.example.tadapter.utils.switchThread
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom

/**
 *
 * author: pengcheng.tan
 * date: 2019-09-16
 */

class MainViewModel : BaseViewModel<MainOutputState, MainInput>(defaultState = MainOutputState()) {

    val productService = ProductService()

    override fun inputUpdate(input: MainInput?, inputOwner: InputOwner) {
        with(inputOwner) {
            input?.type1ProductsNext
                ?.withLatestFrom(bindOutputState().map { it.type1Products.first })
                ?.filter { !it.second.finished }
                ?.switchMapSingle { (_, params) ->
                    productService.getProducts(page = params.page)
                        .switchThread()
                        .flatMap { products ->
                            updateState { state ->
                                val (param, list) = state.type1Products
                                val nexPageParam = if (param.page >= 10) {
                                    param.copy(finished = true)
                                } else {
                                    param.copy(page = param.page + 1)
                                }
                                state.copy(type1Products = nexPageParam to list + products)
                            }.toSingleDefault(Unit)
                        }
                }?.bindInputLife()

            input?.type1Remove
                ?.withLatestFrom(bindOutputState().map { it.type1Products.second })
                ?.flatMapCompletable { (requestRemoveProduct, list) ->
                    val newList = list.filter { requestRemoveProduct != it }
                    updateState {
                        val type1Products = it.type1Products.first to newList
                        it.copy(type1Products = type1Products)
                    }
                }
                ?.bindInputLife()

            input?.type2ProductsNext
                ?.withLatestFrom(bindOutputState().map { it.type2Products.first })
                ?.filter { !it.second.finished }
                ?.flatMapSingle { (_, params) ->
                    productService.getProducts(page = params.page)
                        .switchThread()
                        .flatMap { products ->
                            updateState { state ->
                                val (param, list) = state.type2Products
                                state.copy(type2Products = param.copy(param.page + 1) to list + products)
                            }.toSingleDefault(Unit)
                        }
                }?.bindInputLife()

            input?.type3ProductsNext
                ?.withLatestFrom(bindOutputState().map { it.type3Products.first })
                ?.filter { !it.second.finished }
                ?.flatMapSingle { (_, params) ->
                    productService.getProducts(page = params.page)
                        .switchThread()
                        .flatMap { products ->
                            updateState { state ->
                                val (param, list) = state.type3Products
                                state.copy(type3Products = param.copy(param.page + 1) to list + products)
                            }.toSingleDefault(Unit)
                        }
                }?.bindInputLife()

            input?.type1ItemChanged
                ?.withLatestFrom(bindOutputState().map { it.type1Products.second })
                ?.flatMapSingle { (newItem, oldList) ->
                    val newList = oldList.map { item ->
                        if (item.id == newItem.id) {
                            newItem
                        } else {
                            item
                        }
                    }
                    updateState { state ->
                        state.copy(type1Products = state.type1Products.copy(second = newList))
                    }.toSingleDefault(Unit)
                }
                ?.bindInputLife()
        }
    }

    override fun init() {
        bindOutputState()
            .firstElement()
            .map { it.type1Products }
            .flatMapCompletable { (param, oldList) ->
                productService.getProducts(page = param.page)
                    .switchThread()
                    .flatMapCompletable { products ->
                        updateState {
                            it.copy(type1Products = param.copy(page = param.page + 1) to (oldList + products))
                        }
                    }
            }
            .bindLife()

        bindOutputState()
            .firstElement()
            .map { it.type2Products }
            .flatMapCompletable { (param, oldList) ->
                productService.getProducts(page = param.page)
                    .switchThread()
                    .flatMapCompletable { products ->
                        updateState {
                            it.copy(type2Products = param.copy(page = param.page + 1) to (oldList + products))
                        }
                    }
            }
            .bindLife()

        bindOutputState()
            .firstElement()
            .map { it.type3Products }
            .flatMapCompletable { (param, oldList) ->
                productService.getProducts(page = param.page)
                    .switchThread()
                    .flatMapCompletable { products ->
                        updateState {
                            it.copy(type3Products = param.copy(page = param.page + 1) to (oldList + products))
                        }
                    }
            }
            .bindLife()
    }

}

data class ProductsServiceParams(val page: Int = 1,
                                 val finished: Boolean = false,
                                 val isError: Boolean = false)

data class MainOutputState(
    val type1Products: Pair<ProductsServiceParams, List<Product>> = ProductsServiceParams() to emptyList(),
    val type2Products: Pair<ProductsServiceParams, List<Product>> = ProductsServiceParams() to emptyList(),
    val type3Products: Pair<ProductsServiceParams, List<Product>> = ProductsServiceParams() to emptyList())

data class MainInput(
    val type1ProductsNext: Observable<Unit>,
    val type1Remove: Observable<Product>,
    val type1ItemChanged: Observable<Product>,
    val type2ProductsNext: Observable<Unit>,
    val type3ProductsNext: Observable<Unit>
)