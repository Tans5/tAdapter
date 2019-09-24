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
    val loadingLayoutId: Int,
    val errorLayoutId: Int,
    val dataAdapterSpec: AdapterSpec<D, DBinding>,
    val loadingStateUpdater: Observable<PagingWithFootViewState>,
    val bindDataError: (Int, Throwable, EBinding) -> Unit = { _, _, _ -> Unit },
    val loadNextPage: () -> Unit = { },
    val initShowLoading: Boolean = false
) : AdapterSpec<SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>, ViewDataBinding>,
    Output<PagingWithFootViewState> {

    override val lifeCompositeDisposable: CompositeDisposable = CompositeDisposable()

    override val outputSubject: Subject<PagingWithFootViewState> =
        Output.defaultOutputSubject(if (initShowLoading) PagingWithFootViewState.InitLoading else PagingWithFootViewState.LoadingMore)

    val loadingAdapterSpec = SimpleAdapterSpec<PagingWithFootViewState.LoadingMore, LBinding>(
        layoutId = loadingLayoutId,
        bindData = { _, _, _ -> Unit },
        dataUpdater = bindOutputState()
            .distinctUntilChanged()
            .map { state ->
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
                binding)
        },
        dataUpdater = bindOutputState().distinctUntilChanged()
            .map { state ->
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

    override val bindData: (
        position: Int,
        data: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>,
        binding: ViewDataBinding
    ) -> Unit = { position, item, binding ->
        if (item is SumAdapterDataItem.Left) {
            if (item.left is SumAdapterDataItem.Left) {
                isLastData(item.left.left)
                    .flatMap { isLastData ->
                        if (isLastData) {
                            bindOutputState()
                                .firstOrError()
                                .map { state ->
                                    if (state is PagingWithFootViewState.LoadingMore ||
                                            state is PagingWithFootViewState.InitLoading) {
                                        loadNextPage()
                                    }
                                    Unit
                                }
                        } else {
                            Single.just(Unit)
                        }
                    }
                    .bindLife()
            }
        }
        combineAdapterSpec.bindData(position, item, binding)
    }

    override val dataSubject: Subject<List<SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>,
            PagingWithFootViewState.Error>>> = BehaviorSubject.createDefault<List<SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>,
            PagingWithFootViewState.Error>>>(emptyList()).toSerialized()

    override val differHandler = combineAdapterSpec.differHandler

    override fun itemType(
        position: Int,
        item: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>
    ): Int = combineAdapterSpec.itemType(position, item)

    override fun canHandleTypes(): List<Int> = combineAdapterSpec.canHandleTypes()

    override fun createBinding(context: Context, parent: ViewGroup, viewType: Int)
            : ViewDataBinding = combineAdapterSpec.createBinding(context, parent, viewType)

    fun isLastData(item: D): Single<Boolean> = dataAdapterSpec.dataSubject
        .firstOrError()
        .map { sumItem ->
            item == sumItem[sumItem.lastIndex]
        }

    fun error(e: Throwable): Completable = updateState { PagingWithFootViewState.Error(e) }

    fun loadingMore(): Completable = updateState { PagingWithFootViewState.LoadingMore }

    fun finish(): Completable = updateState { PagingWithFootViewState.Finish }

    override fun adapterAttachToRecyclerView() {
        super.adapterAttachToRecyclerView()
        loadingStateUpdater.distinctUntilChanged()
            .flatMapCompletable { newState ->
                updateState { newState }
            }
            .bindLife()

        combineAdapterSpec.adapterAttachToRecyclerView()
    }

    override fun adapterDetachToRecyclerView() {
        super.adapterDetachToRecyclerView()
        combineAdapterSpec.adapterDetachToRecyclerView()
    }

}


sealed class PagingWithFootViewState {
    object InitLoading : PagingWithFootViewState()
    object LoadingMore : PagingWithFootViewState()
    object Finish : PagingWithFootViewState()
    class Error(val e: Throwable) : PagingWithFootViewState()
}

fun <D, DBinding : ViewDataBinding, LBinding : ViewDataBinding, EBinding : ViewDataBinding> AdapterSpec<D, DBinding>.pagingWithFootView(
    loadingLayoutId: Int,
    errorLayoutId: Int,
    loadingStateUpdater: Observable<PagingWithFootViewState>,
    bindDataError: (Int, Throwable, EBinding) -> Unit = { _, _, _ -> Unit },
    loadNextPage: () -> Unit = { },
    initShowLoading: Boolean = false
)
        : PagingWithFootViewAdapterSpec<D, DBinding, LBinding, EBinding> =
    PagingWithFootViewAdapterSpec(
        loadingLayoutId = loadingLayoutId,
        errorLayoutId = errorLayoutId,
        dataAdapterSpec = this,
        loadingStateUpdater = loadingStateUpdater,
        bindDataError = bindDataError,
        loadNextPage = loadNextPage,
        initShowLoading = initShowLoading)