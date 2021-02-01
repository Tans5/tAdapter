package com.tans.tadapter.spec

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tans.tadapter.core.Stateable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

/**
 *
 * author: pengcheng.tan
 * date: 2019-09-17
 */

private class PagingWithFootViewAdapterSpec<D, DBinding : ViewDataBinding,
        LBinding : ViewDataBinding,
        EBinding : ViewDataBinding>(
    val loadingLayoutId: Int,
    val errorLayoutId: Int,
    val dataAdapterSpec: AdapterSpec<D, DBinding>,
    val loadingStateUpdater: Observable<PagingWithFootViewState>,
    val bindDataError: (Int, Throwable, EBinding) -> Unit = { _, _, _ -> Unit },
    val loadNextPage: () -> Unit = { },
    val initShowLoading: Boolean = false
) : BaseAdapterSpec<SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>, ViewDataBinding>(),
    Stateable<PagingWithFootViewState> by Stateable(if (initShowLoading) PagingWithFootViewState.LoadingMore else PagingWithFootViewState.InitLoading) {

    val lastItemShowSubject: Subject<Unit> = PublishSubject.create<Unit>().toSerialized()

    val recyclerViewScrollListener = object : RecyclerView.OnScrollListener() {

        var isLastItemVisible = false

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount = layoutManager.childCount
            val totalItemCount: Int = layoutManager.itemCount
            isLastItemVisible = firstVisibleItem + visibleItemCount == totalItemCount && dy > 0
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE && isLastItemVisible) {
                lastItemShowSubject.onNext(Unit)
            }
        }

    }

    val loadingAdapterSpec = SimpleAdapterSpec<PagingWithFootViewState.LoadingMore, LBinding>(
        layoutId = loadingLayoutId,
        bindData = { _, _, _ -> Unit },
        dataUpdater = bindState()
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
                binding
            )
        },
        dataUpdater = bindState().distinctUntilChanged()
            .map { state ->
                if (state is PagingWithFootViewState.Error) {
                    listOf(state)
                } else {
                    emptyList()
                }
            }
    )

    val combineAdapterSpec = dataAdapterSpec + loadingAdapterSpec + errorAdapterSpec

    override val dataUpdater: Observable<List<SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>>> = combineAdapterSpec.dataUpdater

    override val swipeRemove: (position: Int, data: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>) -> Unit = combineAdapterSpec.swipeRemove

    override val bindData: (
        position: Int,
        data: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>,
        binding: ViewDataBinding
    ) -> Unit = { position, item, binding ->
        combineAdapterSpec.bindData(position, item, binding)
    }

    override val bindDataPayload: (position: Int, data: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>, binding: ViewDataBinding, payloads: List<Any>) -> Boolean =
        combineAdapterSpec.bindDataPayload

    override val differHandler = combineAdapterSpec.differHandler

    override val itemId: (position: Int, data: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>) -> Long = combineAdapterSpec.itemId

    override val hasStableIds: Boolean = combineAdapterSpec.hasStableIds

    override fun itemType(
        position: Int,
        item: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>
    ): Int = combineAdapterSpec.itemType(position, item)

    override fun canHandleTypes(): List<Int> = combineAdapterSpec.canHandleTypes()

    override fun createBinding(context: Context, parent: ViewGroup, viewType: Int)
            : ViewDataBinding = combineAdapterSpec.createBinding(context, parent, viewType)

    override fun adapterAttachToRecyclerView(recyclerView: RecyclerView) {
        super.adapterAttachToRecyclerView(recyclerView)
        combineAdapterSpec.adapterAttachToRecyclerView(recyclerView)
        recyclerView.addOnScrollListener(recyclerViewScrollListener)
        loadingStateUpdater.distinctUntilChanged()
            .flatMapSingle { newState ->
                updateState { newState }
            }
            .bindLife()

        lastItemShowSubject
            .withLatestFrom(bindState())
            .map { it.second }
            .filter { it == PagingWithFootViewState.LoadingMore }
            .doOnNext {
                loadNextPage()
            }
            .bindLife()
    }

    override fun adapterDetachToRecyclerView(recyclerView: RecyclerView) {
        super.adapterDetachToRecyclerView(recyclerView)
        combineAdapterSpec.adapterDetachToRecyclerView(recyclerView)
        recyclerView.removeOnScrollListener(recyclerViewScrollListener)
    }

    override val itemClicks: List<(binding: ViewDataBinding, type: Int) -> Pair<View, (position: Int, data: SumAdapterDataItem<SumAdapterDataItem<D, PagingWithFootViewState.LoadingMore>, PagingWithFootViewState.Error>) -> Single<Unit>>?> =
        combineAdapterSpec.itemClicks

}


sealed class PagingWithFootViewState {
    object InitLoading : PagingWithFootViewState()
    object LoadingMore : PagingWithFootViewState()
    object Finish : PagingWithFootViewState()
    class Error(val e: Throwable) : PagingWithFootViewState()
}

private fun <D, DBinding : ViewDataBinding, LBinding : ViewDataBinding, EBinding : ViewDataBinding> AdapterSpec<D, DBinding>.pagingWithFootView(
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
        initShowLoading = initShowLoading
    )