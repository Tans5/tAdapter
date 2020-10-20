package com.tans.tadapter.spec

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import com.tans.tadapter.adapter.DifferHandler
import io.reactivex.Observable
import io.reactivex.Single


class DataProxyAdapterSpec<D, Binding : ViewDataBinding>(
    val realAdapterSpec: AdapterSpec<D, Binding>,
    othersDataUpdater: Observable<List<D>>) : BaseAdapterSpec<D, Binding>() {

    override val differHandler: DifferHandler<D> = realAdapterSpec.differHandler

    override val dataUpdater: Observable<List<D>> = Observable.merge(realAdapterSpec.dataUpdater, othersDataUpdater)

    override val bindData: (position: Int, data: D, binding: Binding) -> Unit = realAdapterSpec.bindData

    override fun itemType(position: Int, item: D): Int = realAdapterSpec.itemType(position, item)

    override fun canHandleTypes(): List<Int> = realAdapterSpec.canHandleTypes()

    override fun createBinding(context: Context, parent: ViewGroup, viewType: Int): Binding = realAdapterSpec.createBinding(context, parent, viewType)

    override val bindDataPayload: (position: Int, data: D, binding: Binding, payloads: List<Any>) -> Boolean = realAdapterSpec.bindDataPayload

    override val itemId: (position: Int, data: D) -> Long = realAdapterSpec.itemId

    override val hasStableIds: Boolean = realAdapterSpec.hasStableIds

    override val itemClicks: List<(binding: Binding, type: Int) -> Pair<View, (position: Int, data: D) -> Single<Unit>>?> = realAdapterSpec.itemClicks


}