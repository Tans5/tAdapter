package com.tans.tadapter.spec

import android.content.Context
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import com.tans.tadapter.DifferHandler
import com.tans.tadapter.core.BindLife
import com.tans.tadapter.core.Output
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
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
    val bindDataContent: (Int, D, DBinding) -> Unit = { _, _, _ -> Unit  },
    val bindDataError: (Int, Throwable, EBinding) -> Unit = { _, _, _ -> Unit },
    val dataGetter: PagingWithFootViewAdapterSpec<D, DBinding, LBinding, EBinding>.() -> Observable<List<D>> = { Observable.empty<List<D>>() },
    val loadNextPage: PagingWithFootViewAdapterSpec<D, DBinding, LBinding, EBinding>.() -> Unit = { },
    differHandler: DifferHandler<D> = DifferHandler()
) : AdapterSpec<SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>, ViewDataBinding>,
    Output<PagingWithFootViewState> {

    override val lifeCompositeDisposable: CompositeDisposable = CompositeDisposable()

    override val outputSubject: Subject<PagingWithFootViewState> =
        Output.defaultOutputSubject(PagingWithFootViewState.LoadingMore)

    val dataAdapterSpec = SimpleAdapterSpec<D, DBinding>(
        layoutId = contentLayoutId,
        bindData = { position, item, binding ->
            bindDataContent(position, item, binding)
            isLastData(item)
                .flatMap { isLastData ->
                    if (isLastData) {
                        bindOutputState()
                            .firstOrError()
                            .map { state ->
                                if (state is PagingWithFootViewState.LoadingMore) {
                                    loadNextPage
                                }
                                Unit
                            }
                    } else {
                        Single.just(Unit)
                    }
                }
                .bindLife()

        },
        dataUpdater = dataGetter()
    )

    val loadingAdapterSpec = SimpleAdapterSpec<PagingWithFootViewState.LoadingMore, LBinding>(
        layoutId = loadingLayoutId,
        bindData = { _, _, _ -> Unit },
        dataUpdater = bindOutputState().map { state ->
            if (state is PagingWithFootViewState.LoadingMore) {
                listOf(state)
            } else {
                emptyList()
            }
        })

    val errorAdapterSpec = SimpleAdapterSpec<PagingWithFootViewState.Error, EBinding>(
        layoutId = errorLayoutId,
        bindData = { position, errorState, binding ->
            bindDataError(
                position,
                errorState.e,
                binding
            )
        },
        dataUpdater = bindOutputState().map { state ->
            if (state is PagingWithFootViewState.Error) {
                listOf(state)
            } else {
                emptyList()
            }
        }
    )

    val combineAdapterSpec = dataAdapterSpec + loadingAdapterSpec + errorAdapterSpec

    override val dataUpdater: Observable<List<SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>>> =
        combineAdapterSpec.dataSubject

    override val bindData: (position: Int, data: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>, binding: ViewDataBinding) -> Unit = combineAdapterSpec.bindData

    override val dataSubject: Subject<List<SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>,
            PagingWithFootViewState.Error>>> = combineAdapterSpec.dataSubject

    override val differHandler = object
        :
        DifferHandler<SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>,
                PagingWithFootViewState.Error>>() {

        override fun areItemsTheSame(
            oldItem: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>,
            newItem: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>
        ): Boolean {
            return if (oldItem.left?.left != null && newItem.left?.left != null) {
                differHandler.areItemsTheSame(
                    oldItem = oldItem.left!!.left!!,
                    newItem = newItem.left!!.left!!
                )
            } else {
                false
            }
        }

        override fun areContentsTheSame(
            oldItem: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>,
            newItem: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>
        ): Boolean {
            return if (oldItem.left?.left != null && newItem.left?.left != null) {
                differHandler.areContentsTheSame(
                    oldItem = oldItem.left!!.left!!,
                    newItem = newItem.left!!.left!!
                )
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

    override fun canHandleTypes(): List<Int> =
        listOf(contentLayoutId, loadingLayoutId, errorLayoutId)

    override fun createBinding(context: Context, parent: ViewGroup, viewType: Int)
            : ViewDataBinding = combineAdapterSpec.createBinding(context, parent, viewType)

    fun isLastData(item: D): Single<Boolean> = dataSubject
        .firstOrError()
        .map { sumItem ->
            val lastData = sumItem.asReversed()
                .find {
                    when (it) {
                        is SumAdapterDataItem.Left -> {
                            when (it.left) {
                                is SumAdapterDataItem.Left -> {
                                    true
                                }
                                is SumAdapterDataItem.Right -> {
                                    false
                                }
                            }
                        }
                        is SumAdapterDataItem.Right -> {
                            false
                        }
                    }
                }?.left?.left
            item == lastData
        }

    fun error(e: Throwable): Completable = updateState { PagingWithFootViewState.Error(e) }

    fun loadingMore(): Completable = updateState { PagingWithFootViewState.LoadingMore }

    fun finish(): Completable = updateState { PagingWithFootViewState.Finish }

    override fun adapterAttachToRecyclerView() {
        super.adapterAttachToRecyclerView()
        combineAdapterSpec.adapterAttachToRecyclerView()
    }

    override fun adapterDetachToRecyclerView() {
        super.adapterDetachToRecyclerView()
        combineAdapterSpec.adapterDetachToRecyclerView()
    }

}


sealed class PagingWithFootViewState {
    object LoadingMore : PagingWithFootViewState()
    object Finish : PagingWithFootViewState()
    class Error(val e: Throwable) : PagingWithFootViewState()
}