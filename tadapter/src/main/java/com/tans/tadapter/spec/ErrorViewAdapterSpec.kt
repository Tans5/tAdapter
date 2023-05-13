package com.tans.tadapter.spec

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.tans.tadapter.adapter.DifferHandler
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.PublishSubject

/**
 *
 * author: pengcheng.tan
 * date: 2019-09-24
 */

class ErrorViewAdapterSpec<D : Any, DBinding : ViewDataBinding, EBinding : ViewDataBinding>(
    val errorLayout: Int,
    val dataAdapterSpec: AdapterSpec<D, DBinding>,
    val errorChecker: Observable<Throwable>,
    val bindDataError: (Int, Throwable, EBinding) -> Unit = { _, _, _ -> Unit }
) : BaseAdapterSpec<SumAdapterDataItem<D, Throwable>, ViewDataBinding>() {

    val othersDataSubject = PublishSubject.create<List<D>>().toSerialized()

    val errorDataSpec: SimpleAdapterSpec<Throwable, EBinding> = SimpleAdapterSpec(
            layoutId = errorLayout,
            dataUpdater = errorChecker.distinctUntilChanged().map {
                othersDataSubject.onNext(emptyList())
                listOf(it)
            },
            bindData = bindDataError
    )

    val combineAdapterSpec = DataProxyAdapterSpec(dataAdapterSpec, othersDataSubject) + errorDataSpec

    override val differHandler: DifferHandler<SumAdapterDataItem<D, Throwable>> =
            combineAdapterSpec.differHandler

    override val dataUpdater: Observable<List<SumAdapterDataItem<D, Throwable>>> =
        combineAdapterSpec.dataUpdater

    override val bindData: (position: Int, data: SumAdapterDataItem<D, Throwable>, binding: ViewDataBinding) -> Unit =
            combineAdapterSpec.bindData

    override val swipeRemove: (position: Int, data: SumAdapterDataItem<D, Throwable>) -> Unit = combineAdapterSpec.swipeRemove

    override val bindDataPayload: (position: Int, data: SumAdapterDataItem<D, Throwable>, binding: ViewDataBinding, payloads: List<Any>) -> Boolean =
            combineAdapterSpec.bindDataPayload

    override fun itemType(position: Int, item: SumAdapterDataItem<D, Throwable>): Int =
            combineAdapterSpec.itemType(position, item)

    override val hasStableIds: Boolean = combineAdapterSpec.hasStableIds

    override val itemId: (position: Int, data: SumAdapterDataItem<D, Throwable>) -> Long = combineAdapterSpec.itemId

    override fun canHandleTypes(): List<Int> = combineAdapterSpec.canHandleTypes()

    override fun createBinding(
            context: Context,
            parent: ViewGroup,
            viewType: Int
    ): ViewDataBinding = combineAdapterSpec.createBinding(context, parent, viewType)

    override fun adapterAttachToRecyclerView(recyclerView: RecyclerView) {
        super.adapterAttachToRecyclerView(recyclerView)
        combineAdapterSpec.adapterAttachToRecyclerView(recyclerView)
    }

    override fun adapterDetachToRecyclerView(recyclerView: RecyclerView) {
        super.adapterDetachToRecyclerView(recyclerView)
        combineAdapterSpec.adapterDetachToRecyclerView(recyclerView)
    }

    override val itemClicks: List<(binding: ViewDataBinding, type: Int) -> Pair<View, (position: Int, data: SumAdapterDataItem<D, Throwable>) -> Single<Unit>>?>
        = combineAdapterSpec.itemClicks

}

fun <D : Any, DBinding : ViewDataBinding, EBinding : ViewDataBinding> AdapterSpec<D, DBinding>.errorView(
        errorLayout: Int,
        errorChecker: Observable<Throwable>,
        bindDataError: (Int, Throwable, EBinding) -> Unit = { _, _, _ -> Unit }
): ErrorViewAdapterSpec<D, DBinding, EBinding> = ErrorViewAdapterSpec(
        errorLayout = errorLayout,
        dataAdapterSpec = this,
        errorChecker = errorChecker,
        bindDataError = bindDataError)