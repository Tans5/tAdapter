package com.tans.tadapter.spec

import android.content.Context
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import com.tans.tadapter.DifferHandler
import com.tans.tadapter.core.BindLife
import com.tans.tadapter.core.Output
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/**
 *
 * author: pengcheng.tan
 * date: 2019-09-17
 */

class PagingWithFootViewAdapterSpec<D, DBinding : ViewDataBinding,
        LBinding : ViewDataBinding,
        EBinding : ViewDataBinding>(
    val contentLayoutId: Int,
    val loadingLayoutId: Int,
    val errorLayoutId: Int,
    val bindData: (D, DBinding) -> Unit = { _, _ -> Unit },
    val bindDataError: (Throwable, EBinding) -> Unit = { _, _ -> Unit },
    differHandler: DifferHandler<D> = DifferHandler()
) : AdapterSpec<SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>, ViewDataBinding>, BindLife, Output<PagingWithFootViewState> {

    override val lifeCompositeDisposable: CompositeDisposable = CompositeDisposable()

    override val outputSubject: Subject<PagingWithFootViewState> =
        Output.defaultOutputSubject(PagingWithFootViewState.LoadingMore)

    val dataAdapterSpec = SimpleAdapterSpec<D, DBinding>(
        layoutId = contentLayoutId,
        bindData = bindData,
        dataUpdater = Observable.just(emptyList()))

    val loadingAdapterSpec = SimpleAdapterSpec<PagingWithFootViewState.LoadingMore, LBinding>(
        layoutId = loadingLayoutId,
        bindData = { _, _ -> Unit },
        dataUpdater = Observable.just(emptyList()))

    val errorAdapterSpec = SimpleAdapterSpec<PagingWithFootViewState.Error, EBinding>(
        layoutId = errorLayoutId,
        bindData = { errorState, binding -> bindDataError(errorState.e, binding) },
        dataUpdater = Observable.just(emptyList()))

    val combineAdapterSpec = dataAdapterSpec + loadingAdapterSpec + errorAdapterSpec

    override val dataSubject: Subject<List<SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>,
            PagingWithFootViewState.Error>>> = BehaviorSubject.createDefault<List<SumAdapterDataItem<SumAdapterDataItem<D,
            PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>>>(emptyList()).toSerialized()

    override val differHandler = object
        : DifferHandler<SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>,
            PagingWithFootViewState.Error>>() {

        override fun areItemsTheSame(
            oldItem: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>,
            newItem: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>
        ): Boolean {
            return if (oldItem.left?.left != null && newItem.left?.left != null) {
                differHandler.areItemsTheSame(oldItem = oldItem.left!!.left!!, newItem = newItem.left!!.left!!)
            } else {
                false
            }
        }

        override fun areContentsTheSame(
            oldItem: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>,
            newItem: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>
        ): Boolean {
            return if (oldItem.left?.left != null && newItem.left?.left != null) {
                differHandler.areContentsTheSame(oldItem = oldItem.left!!.left!!, newItem = newItem.left!!.left!!)
            } else {
                false
            }
        }

    }

    override fun itemType(
        position: Int,
        item: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>
    ): Int {
        return when (item) {
            is SumAdapterDataItem.Left -> {
                when (item.left) {
                    is SumAdapterDataItem.Left -> {
                        contentLayoutId
                    }
                    is SumAdapterDataItem.Right -> {
                        loadingLayoutId
                    }
                }
            }
            is SumAdapterDataItem.Right -> {
                errorLayoutId
            }
        }
    }

    override fun canHandleTypes(): List<Int> = listOf(contentLayoutId, loadingLayoutId, errorLayoutId)

    override fun dataUpdater(): Observable<List<SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>>>
            = combineAdapterSpec.dataUpdater()

    override fun createBinding(context: Context, parent: ViewGroup, viewType: Int)
            : ViewDataBinding = combineAdapterSpec.createBinding(context, parent, viewType)

    override fun bindData(
        data: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>,
        binding: ViewDataBinding
    ) {
        combineAdapterSpec.bindData(data, binding)
    }


}


sealed class PagingWithFootViewState {
    object LoadingMore : PagingWithFootViewState()
    object Finish : PagingWithFootViewState()
    class Error(val e: Throwable) : PagingWithFootViewState()
}