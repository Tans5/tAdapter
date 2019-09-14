package com.tans.tadapter

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable

interface BindLife {

    val lifeCompositeDisposable: CompositeDisposable

    fun <T> Observable<T>.bindLife() {
        lifeCompositeDisposable.add(this.subscribe({
            Log.d("tAdapter", "Next: ${it.toString()}")
        }, {
            Log.e("tAdapter", it.toString())
        }, {
            Log.d("tAdapter","Complete")
        }))
    }

    fun Completable.bindLife() {
        lifeCompositeDisposable.add(this.subscribe({
            Log.d("tAdapter","Complete")
        }, {
            Log.d("tAdapter", it.toString())
        }))
    }

    fun <T> Single<T>.bindLife() {
        lifeCompositeDisposable.add(this.subscribe({
            Log.d("tAdapter", it.toString())
        }, {
            Log.d("tAdapter", it.toString())
        }))
    }

    fun <T> Maybe<T>.bindLife() {
        lifeCompositeDisposable.add(this.subscribe ({
            Log.d("tAdapter","Success: $it")
        }, {
            Log.d("tAdapter", it.toString())
        }, {
            Log.d("tAdapter","Complete")
        }))
    }

}