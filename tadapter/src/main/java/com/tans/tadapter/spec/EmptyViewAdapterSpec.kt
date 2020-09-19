package com.tans.tadapter.spec

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.tans.tadapter.adapter.DifferHandler
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/**
 *
 * author: pengcheng.tan
 * date: 2019-09-24
 */

class EmptyViewAdapterSpec<D, DBinding : ViewDataBinding, EBinding : ViewDataBinding>(
    val emptyLayout: Int,
    val dataAdapterSpec: AdapterSpec<D, DBinding>,
    val initShowEmpty: Boolean = false) : BaseAdapterSpec<SumAdapterDataItem<D, Unit>, ViewDataBinding>() {


    val emptyAdapterSpec: SimpleAdapterSpec<Unit, EBinding> = SimpleAdapterSpec(
            layoutId = emptyLayout,
            dataUpdater = dataAdapterSpec.dataSubject
                    .skip(if (initShowEmpty) 0 else 1)
                    .map { dataList ->
                        if (dataList.isEmpty()) {
                            listOf(Unit)
                        } else {
                            emptyList()
                        }
                    }
    )

    val combineAdapterSpec = dataAdapterSpec + emptyAdapterSpec

    override val itemClicks: List<(binding: ViewDataBinding, type: Int) -> Pair<View, (position: Int, data: SumAdapterDataItem<D, Unit>) -> Single<Unit>>?> = combineAdapterSpec.itemClicks

    override val dataSubject: Subject<List<SumAdapterDataItem<D, Unit>>> = BehaviorSubject.createDefault<List<SumAdapterDataItem<D, Unit>>>(emptyList()).toSerialized()

    override val differHandler: DifferHandler<SumAdapterDataItem<D, Unit>> = combineAdapterSpec.differHandler

    override val dataUpdater: Observable<List<SumAdapterDataItem<D, Unit>>> = combineAdapterSpec.dataSubject

    override val bindData: (position: Int, data: SumAdapterDataItem<D, Unit>, binding: ViewDataBinding) -> Unit = combineAdapterSpec.bindData

    override val bindDataPayload: (position: Int, data: SumAdapterDataItem<D, Unit>, binding: ViewDataBinding, payloads: List<Any>) -> Boolean = combineAdapterSpec.bindDataPayload

    override val hasStableIds: Boolean = dataAdapterSpec.hasStableIds

    override val itemId: (position: Int, data: SumAdapterDataItem<D, Unit>) -> Long = combineAdapterSpec.itemId

    override fun itemType(position: Int, item: SumAdapterDataItem<D, Unit>): Int = combineAdapterSpec.itemType(position, item)

    override fun canHandleTypes(): List<Int> = combineAdapterSpec.canHandleTypes()

    override fun createBinding(
            context: Context,
            parent: ViewGroup,
            viewType: Int
    ): ViewDataBinding = combineAdapterSpec.createBinding(context, parent, viewType)

    override val lifeCompositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun adapterAttachToRecyclerView(recyclerView: RecyclerView) {
        super.adapterAttachToRecyclerView(recyclerView)
        combineAdapterSpec.adapterAttachToRecyclerView(recyclerView)
    }

    override fun adapterDetachToRecyclerView(recyclerView: RecyclerView) {
        super.adapterDetachToRecyclerView(recyclerView)
        combineAdapterSpec.adapterDetachToRecyclerView(recyclerView)
    }

}

fun <D, DBinding : ViewDataBinding, EBinding : ViewDataBinding> AdapterSpec<D, DBinding>.emptyView(emptyLayout: Int, initShowEmpty: Boolean = false)
        : AdapterSpec<SumAdapterDataItem<D, Unit>, ViewDataBinding> = EmptyViewAdapterSpec<D, DBinding, EBinding>(
        emptyLayout = emptyLayout,
        dataAdapterSpec = this,
        initShowEmpty = initShowEmpty)