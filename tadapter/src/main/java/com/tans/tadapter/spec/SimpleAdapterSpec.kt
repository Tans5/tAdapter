package com.tans.tadapter.spec

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.tans.tadapter.adapter.DifferHandler
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

typealias BindingGetter<Binding> = (context: Context, parent: ViewGroup, layoutId: Int, viewType: Int) -> Binding

class SimpleAdapterSpec<D : Any, Binding : ViewDataBinding>(
    val layoutId: Int,
    override val bindData: ((Int, D, Binding) -> Unit) = { _, _, _ -> Unit },
    override val bindDataPayload: (position: Int, data: D, binding: Binding, payloads: List<Any>) -> Boolean = { _, _, _, _ -> false },
    override val dataUpdater: Observable<List<D>>,
    override val differHandler: DifferHandler<D> = DifferHandler(),
    override val hasStableIds: Boolean = false,
    override val itemId: (position: Int, data: D) -> Long = { _, _ -> RecyclerView.NO_ID },
    override val itemClicks: List<(binding: Binding, type: Int) -> Pair<View, (position: Int, data: D) -> Single<Unit>>> = emptyList(),
    override val swipeRemove: (position: Int, data: D) -> Unit = { _, _ -> },
    val bindingGetter: BindingGetter<Binding> = { context: Context, parent: ViewGroup, layoutIdL: Int, _ -> DataBindingUtil.inflate(LayoutInflater.from(context), layoutIdL, parent, false) }
) : BaseAdapterSpec<D, Binding>() {

    override fun itemType(position: Int, item: D): Int = layoutId

    override fun canHandleTypes(): List<Int> = listOf(layoutId)

    override fun createBinding(context: Context, parent: ViewGroup, viewType: Int)
            : Binding = bindingGetter(context, parent, layoutId, viewType)
}