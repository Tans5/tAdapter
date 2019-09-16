package com.example.tadapter.core

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable

interface InputOwner {

    val inputCompositeDisposable: CompositeDisposable

    fun <T> Observable<T>.bindInputLife() {
        inputCompositeDisposable.add(this.subscribe({
            Log.d("Rx", "Next: ${it.toString()}")
        }, {
            Log.e("Rx", it.toString())
        }, {
            Log.d("Rx","Complete")
        }))
    }

    fun Completable.bindInputLife() {
        inputCompositeDisposable.add(this.subscribe({
            Log.d("Rx","Complete")
        }, {
            Log.e("Rx", it.toString())
        }))
    }

    fun <T> Single<T>.bindInputLife() {
        inputCompositeDisposable.add(this.subscribe({
            Log.d("Rx", it.toString())
        }, {
            Log.e("Rx", it.toString())
        }))
    }

    fun <T> Maybe<T>.bindInputLife() {
        inputCompositeDisposable.add(this.subscribe ({
            Log.d("Rx","Success: $it")
        }, {
            Log.e("Rx", it.toString())
        }, {
            Log.d("Rx", "Complete")
        }))
    }

    fun clear() {
        inputCompositeDisposable.clear()
    }

}