package com.tans.tadapter

import android.content.Context
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.Subject

interface AdapterSpec<D, Binding: ViewDataBinding> {

    val dataSubject: Subject<List<D>>

    val differHandler: DifferHandler<D>

    fun itemType(position: Int): Int

    fun canHandleTypes(): List<Int>

    fun createBinding(context: Context, parent: ViewGroup, viewType: Int): Binding

    fun bindData(data: D, binding: Binding)

    fun dataUpdater(): Observable<List<D>>

}

fun <D, Binding: ViewDataBinding> AdapterSpec<D, Binding>.toAdapter()
        : BaseAdapter<D, Binding> = object : BaseAdapter<D, Binding>(adapterSpec = this) {
    override val lifeCompositeDisposable: CompositeDisposable = CompositeDisposable()
}