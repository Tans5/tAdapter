package com.tans.tadapter.spec

import android.content.Context
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.tans.tadapter.adapter.DifferHandler
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/**
 *
 * author: pengcheng.tan
 * date: 2019-09-24
 */

class ErrorViewAdapterSpec<D, DBinding : ViewDataBinding, EBinding : ViewDataBinding>(
        val errorLayout: Int,
        val dataAdapterSpec: AdapterSpec<D, DBinding>,
        val errorChecker: Observable<Throwable>,
        val bindDataError: (Int, Throwable, EBinding) -> Unit = { _, _, _ -> Unit }
) : AdapterSpec<SumAdapterDataItem<D, Throwable>, ViewDataBinding> {

    val errorDataSpec: SimpleAdapterSpec<Throwable, EBinding> = SimpleAdapterSpec(
            layoutId = errorLayout,
            dataUpdater = errorChecker.distinctUntilChanged().map {
                dataAdapterSpec.dataSubject.onNext(
                        emptyList()
                ); listOf(it)
            },
            bindData = bindDataError
    )

    val combineAdapterSpec = dataAdapterSpec + errorDataSpec

    override val dataSubject: Subject<List<SumAdapterDataItem<D, Throwable>>> =
            BehaviorSubject.createDefault<List<SumAdapterDataItem<D, Throwable>>>(emptyList())
                    .toSerialized()

    override val differHandler: DifferHandler<SumAdapterDataItem<D, Throwable>> =
            combineAdapterSpec.differHandler

    override val dataUpdater: Observable<List<SumAdapterDataItem<D, Throwable>>> =
            combineAdapterSpec.dataSubject

    override val bindData: (position: Int, data: SumAdapterDataItem<D, Throwable>, binding: ViewDataBinding) -> Unit =
            combineAdapterSpec.bindData

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

fun <D, DBinding : ViewDataBinding, EBinding : ViewDataBinding> AdapterSpec<D, DBinding>.errorView(
        errorLayout: Int,
        errorChecker: Observable<Throwable>,
        bindDataError: (Int, Throwable, EBinding) -> Unit = { _, _, _ -> Unit }
): ErrorViewAdapterSpec<D, DBinding, EBinding> = ErrorViewAdapterSpec(
        errorLayout = errorLayout,
        dataAdapterSpec = this,
        errorChecker = errorChecker,
        bindDataError = bindDataError)