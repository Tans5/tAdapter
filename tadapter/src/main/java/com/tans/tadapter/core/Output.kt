package com.tans.tadapter.core

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

interface Output<State> {

    val outputSubject: Subject<State>

    fun bindOutputState(): Observable<State> = outputSubject

    fun updateState(newState: (State) -> State): Completable = Completable.fromAction {
        val oldState = outputSubject.blockingFirst()
        outputSubject.onNext(newState(oldState))
    }

    companion object {
        fun <State> defaultOutputSubject(defaultState: State) = BehaviorSubject.createDefault(defaultState).toSerialized()
    }
}