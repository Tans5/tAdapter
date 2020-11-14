package com.tans.tadapter.spec

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.tans.tadapter.adapter.DifferHandler
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

typealias BindingGetter<Binding> = (context: Context, parent: ViewGroup, layoutId: Int, viewType: Int) -> Binding

class SimpleAdapterSpec<D, Binding : ViewDataBinding>(
    val layoutId: Int,
    override val bindData: ((Int, D, Binding) -> Unit) = { _, _, _ -> Unit },
    override val bindDataPayload: (position: Int, data: D, binding: Binding, payloads: List<Any>) -> Boolean = { _, _, _, _ -> false },
    override val dataUpdater: Observable<List<D>>,
    override val differHandler: DifferHandler<D> = DifferHandler(),
    override val hasStableIds: Boolean = false,
    override val itemId: (position: Int, data: D) -> Long = { _, _ -> RecyclerView.NO_ID },
    override val itemClicks: List<(binding: Binding, type: Int) -> Pair<View, (position: Int, data: D) -> Single<Unit>>> = emptyList(),
    val bindingGetter: BindingGetter<Binding> = { context: Context, parent: ViewGroup, layoutIdL: Int, _ -> DataBindingUtil.inflate(LayoutInflater.from(context), layoutIdL, parent, false) }
) : BaseAdapterSpec<D, Binding>() {

    override fun itemType(position: Int, item: D): Int = layoutId

    override fun canHandleTypes(): List<Int> = listOf(layoutId)

    override fun createBinding(context: Context, parent: ViewGroup, viewType: Int)
            : Binding = bindingGetter(context, parent, layoutId, viewType)
}