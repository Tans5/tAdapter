package com.tans.tadapter.spec

import android.content.Context
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import com.tans.tadapter.BaseAdapter
import com.tans.tadapter.DifferHandler
import com.tans.tadapter.core.BindLife
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.Subject

interface AdapterSpec<D, Binding: ViewDataBinding> : BindLife {

    val dataSubject: Subject<List<D>>

    val differHandler: DifferHandler<D>

    val dataUpdater: Observable<List<D>>

    val bindData: (position: Int, data: D, binding: Binding) -> Unit

    fun itemType(position: Int, item: D): Int

    fun canHandleTypes(): List<Int>

    fun createBinding(context: Context, parent: ViewGroup, viewType: Int): Binding

    fun adapterAttachToRecyclerView() {
        dataUpdater
            .doOnNext {
                dataSubject.onNext(it)
            }
            .bindLife()
    }

    fun adapterDetachToRecyclerView() {
        lifeCompositeDisposable.clear()
    }

}

fun <D, Binding: ViewDataBinding> AdapterSpec<D, Binding>.toAdapter()
        : BaseAdapter<D, Binding> = object : BaseAdapter<D, Binding>(adapterSpec = this) {
    override val lifeCompositeDisposable: CompositeDisposable = CompositeDisposable()
}