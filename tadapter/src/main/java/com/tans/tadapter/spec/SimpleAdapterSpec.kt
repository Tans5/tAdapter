package com.tans.tadapter.spec

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.tans.tadapter.DifferHandler
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

class SimpleAdapterSpec<D, Binding : ViewDataBinding>(
    val layoutId: Int,
    val dataUpdater: Observable<List<D>>,
    val bindData: ((D, Binding) -> Unit) = { _, _ -> Unit},
    override val differHandler: DifferHandler<D> = DifferHandler()
) : AdapterSpec<D, Binding> {

    override val dataSubject: Subject<List<D>> = BehaviorSubject.createDefault<List<D>>(emptyList()).toSerialized()

    override fun dataUpdater(): Observable<List<D>> = dataUpdater.doOnNext {
        dataSubject.onNext(it)
    }

    override fun itemType(position: Int, item: D): Int = layoutId

    override fun canHandleTypes(): List<Int> = listOf(layoutId)

    override fun createBinding(context: Context, parent: ViewGroup, viewType: Int)
            : Binding =
        DataBindingUtil.inflate(LayoutInflater.from(context), layoutId, parent, false)

    override fun bindData(data: D, binding: Binding) {
        bindData.invoke(data, binding)
    }
}