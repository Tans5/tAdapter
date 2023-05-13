package com.tans.tadapter.core

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

interface BindLife {
    val lifeCompositeDisposable: CompositeDisposable

    fun <T : Any> Observable<T>.bindLife() {
        lifeCompositeDisposable.add(this.subscribe({
        }, {
            it.printStackTrace()
        }, {
        }))
    }

    fun Completable.bindLife() {
        lifeCompositeDisposable.add(this.subscribe({

        }, {
            it.printStackTrace()
        }))
    }

    fun <T : Any> Single<T>.bindLife() {
        lifeCompositeDisposable.add(this.subscribe({

        }, {
            it.printStackTrace()
        }))
    }

    fun <T : Any> Maybe<T>.bindLife() {
        lifeCompositeDisposable.add(this.subscribe ({
        }, {
            it.printStackTrace()
        }, {

        }))
    }
}

fun BindLife(): BindLife = object : BindLife {
    override val lifeCompositeDisposable: CompositeDisposable = CompositeDisposable()
}