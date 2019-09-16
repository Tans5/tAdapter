package com.example.tadapter.core

import androidx.lifecycle.ViewModel

import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable

abstract class BaseViewModel<OutputState, Input>(defaultState: OutputState) : ViewModel(),
        BindLife, Output<OutputState> {

    override val outputSubject = Output.defaultOutputSubject(defaultState)

    override val lifeCompositeDisposable: CompositeDisposable = CompositeDisposable()

    fun setInput(input: Input?, inputOwner: InputOwner) {
        inputOwner.inputCompositeDisposable.clear()
        inputUpdate(input, inputOwner)
    }
    abstract fun inputUpdate(input: Input?, inputOwner: InputOwner)

    open fun init() {
    }

    open fun initWithCompletable(): Completable = Completable.complete()

    override fun onCleared() {
        super.onCleared()
        outputSubject.onComplete()
        lifeCompositeDisposable.clear()
    }

}