package com.tans.tadapter.spec

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.tans.tadapter.adapter.DifferHandler
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.lang.RuntimeException

class SumAdapterSpec<LD, RD, LBinding : ViewDataBinding, RBinding : ViewDataBinding>(
        val leftSpec: AdapterSpec<LD, LBinding>,
        val rightSpec: AdapterSpec<RD, RBinding>,
        val syncDataByHand: Boolean = false
) : AdapterSpec<SumAdapterDataItem<LD, RD>, ViewDataBinding> {

    override val lifeCompositeDisposable: CompositeDisposable = CompositeDisposable()

    override val dataSubject: Subject<List<SumAdapterDataItem<LD, RD>>> =
            BehaviorSubject.createDefault<List<SumAdapterDataItem<LD, RD>>>(emptyList()).toSerialized()

    val dataUpdaterSubject: Subject<List<SumAdapterDataItem<LD, RD>>> =
        BehaviorSubject.createDefault<List<SumAdapterDataItem<LD, RD>>>(emptyList()).toSerialized()

    override val bindData: (position: Int, data: SumAdapterDataItem<LD, RD>, binding: ViewDataBinding) -> Unit = { position, data, binding ->
        val (leftSize, rightSize) = childrenSize().blockingGet()
        when (data) {
            is SumAdapterDataItem.Left -> {
                val lBinding: LBinding? = binding as? LBinding
                if (lBinding != null) {
                    leftSpec.bindData(position, data.left, lBinding)
                }
            }
            is SumAdapterDataItem.Right -> {
                val rBinding: RBinding? = (binding as? RBinding)
                if (rBinding != null) {
                    rightSpec.bindData(position - leftSize, data.right, rBinding)
                }
            }
        }
    }

    override val bindDataPayload: (position: Int, data: SumAdapterDataItem<LD, RD>, binding: ViewDataBinding, payloads: List<Any>) -> Boolean = { position, data, binding, payloads ->
        val (leftSize, rightSize) = childrenSize().blockingGet()
        when (data) {
            is SumAdapterDataItem.Left -> {
                val lBinding: LBinding? = (binding as? LBinding)
                if (lBinding != null) {
                    leftSpec.bindDataPayload(position, data.left, lBinding, payloads)
                } else {
                    false
                }
            }
            is SumAdapterDataItem.Right -> {
                val rBinding: RBinding? = (binding as? RBinding)
                if (rBinding != null) {
                    rightSpec.bindDataPayload(position - leftSize, data.right, rBinding, payloads)
                } else {
                    false
                }
            }
        }
    }

    override val hasStableIds: Boolean = leftSpec.hasStableIds || rightSpec.hasStableIds

    override val itemId: (position: Int, data: SumAdapterDataItem<LD, RD>) -> Long = { position, data ->
        val (leftSize, _) = childrenSize().blockingGet()
        when (data) {
            is SumAdapterDataItem.Left -> {
                leftSpec.itemId(position, data.left)
            }
            is SumAdapterDataItem.Right -> {
                rightSpec.itemId(position - leftSize, data.right)
            }
        }
    }


    override val differHandler: DifferHandler<SumAdapterDataItem<LD, RD>> =
            object : DifferHandler<SumAdapterDataItem<LD, RD>>() {

                override fun areItemsTheSame(
                        oldItem: SumAdapterDataItem<LD, RD>,
                        newItem: SumAdapterDataItem<LD, RD>
                ): Boolean {
                    return when {
                        oldItem is SumAdapterDataItem.Left && newItem is SumAdapterDataItem.Left -> {
                            leftSpec.differHandler.areItemsTheSame(oldItem.left, newItem.left)
                        }

                        oldItem is SumAdapterDataItem.Right && newItem is SumAdapterDataItem.Right -> {
                            rightSpec.differHandler.areItemsTheSame(oldItem.right, newItem.right)
                        }
                        else -> false
                    }
                }

                override fun areContentsTheSame(
                        oldItem: SumAdapterDataItem<LD, RD>,
                        newItem: SumAdapterDataItem<LD, RD>
                ): Boolean {
                    return when {
                        oldItem is SumAdapterDataItem.Left && newItem is SumAdapterDataItem.Left -> {
                            leftSpec.differHandler.areContentsTheSame(oldItem.left, newItem.left)
                        }

                        oldItem is SumAdapterDataItem.Right && newItem is SumAdapterDataItem.Right -> {
                            rightSpec.differHandler.areContentsTheSame(oldItem.right, newItem.right)
                        }
                        else -> false
                    }
                }

                override fun getChangePayload(
                    oldItem: SumAdapterDataItem<LD, RD>,
                    newItem: SumAdapterDataItem<LD, RD>
                ): Any? {
                    return when {
                        oldItem is SumAdapterDataItem.Left && newItem is SumAdapterDataItem.Left -> {
                            leftSpec.differHandler.getChangePayload(oldItem.left, newItem.left)
                        }

                        oldItem is SumAdapterDataItem.Right && newItem is SumAdapterDataItem.Right -> {
                            rightSpec.differHandler.getChangePayload(oldItem.right, newItem.right)
                        }
                        else -> null
                    }
                }

            }

    override val dataUpdater: Observable<List<SumAdapterDataItem<LD, RD>>> = if (syncDataByHand) {
        dataUpdaterSubject
    } else {
        Observable.merge(leftSpec.dataSubject
            .toLeft()
            .distinctUntilChanged()
            .withLatestFrom(rightSpec.dataSubject.toRight())
            .map { it.first + it.second },
            rightSpec.dataSubject
                .toRight()
                .distinctUntilChanged()
                .withLatestFrom(leftSpec.dataSubject.toLeft())
                .map { it.second + it.first })
    }

    fun syncData(): Single<Unit> {
        return if (syncDataByHand) {
            leftSpec.dataSubject.toLeft().firstOrError()
                .zipWith(rightSpec.dataSubject.toRight().firstOrError())
                .map { (leftData, rightData) -> leftData + rightData }
                .doOnSuccess { dataUpdaterSubject.onNext(it) }
                .map { Unit }
        } else {
            Single.just(Unit)
        }
    }

    private fun Observable<List<LD>>.toLeft() = this.map { data ->
        data.map { item -> SumAdapterDataItem.Left<LD, RD>(left = item) }
    }

    private fun Observable<List<RD>>.toRight() = this.map { data ->
        data.map { item -> SumAdapterDataItem.Right<LD, RD>(right = item) }
    }

    private fun isLeft(position: Int): Maybe<Boolean> = childrenSize()
            .flatMapMaybe { (leftItemSize, rightItemSize) ->
                when (position) {
                    in 0 until leftItemSize -> Maybe.just(true)
                    in leftItemSize until leftItemSize + rightItemSize -> Maybe.just(false)
                    else -> Maybe.empty<Boolean>()
                }
            }

    private fun childrenSize(): Single<Pair<Int, Int>> = dataSubject.firstOrError()
            .map {
                val leftItemSize = it.sumBy { item ->
                    if (item is SumAdapterDataItem.Left) {
                        1
                    } else {
                        0
                    }
                }
                val rightItemSize = it.sumBy { item ->
                    if (item is SumAdapterDataItem.Right) {
                        1
                    } else {
                        0
                    }
                }
                leftItemSize to rightItemSize
            }

    override fun itemType(position: Int, item: SumAdapterDataItem<LD, RD>): Int {
        val (leftSize, rightSize) = childrenSize().blockingGet()

        return when {
            item is SumAdapterDataItem.Left && position < leftSize -> {
                leftSpec.itemType(position, item.left)
            }
            item is SumAdapterDataItem.Right && position in leftSize until leftSize + rightSize -> {
                rightSpec.itemType(position - leftSize, item.right)
            }
            else -> 0
        }

    }

    override fun canHandleTypes(): List<Int> =
            leftSpec.canHandleTypes() + rightSpec.canHandleTypes()

    override fun createBinding(
            context: Context,
            parent: ViewGroup,
            viewType: Int
    ): ViewDataBinding = when {
        leftSpec.canHandleTypes().contains(viewType) -> leftSpec.createBinding(
                context,
                parent,
                viewType
        )
        rightSpec.canHandleTypes().contains(viewType) -> rightSpec.createBinding(
                context,
                parent,
                viewType
        )
        else -> throw RuntimeException("Can't deal viewType: $viewType")
    }

    override fun adapterAttachToRecyclerView(recyclerView: RecyclerView) {
        super.adapterAttachToRecyclerView(recyclerView)
        leftSpec.adapterAttachToRecyclerView(recyclerView)
        rightSpec.adapterAttachToRecyclerView(recyclerView)
    }

    override fun adapterDetachToRecyclerView(recyclerView: RecyclerView) {
        super.adapterDetachToRecyclerView(recyclerView)
        leftSpec.adapterDetachToRecyclerView(recyclerView)
        rightSpec.adapterDetachToRecyclerView(recyclerView)
    }

    override val itemClicks: List<(binding: ViewDataBinding, type: Int) -> Pair<View, (position: Int, data: SumAdapterDataItem<LD, RD>) -> Single<Unit>>?>
            = leftSpec.itemClicks.map { leftClick ->
        { binding: ViewDataBinding, type: Int ->
            if (leftSpec.canHandleTypes().contains(type)) {
                val viewAndHandle: Pair<View, (Int, LD) -> Single<Unit>>? = leftClick.invoke(binding as LBinding, type)
                if (viewAndHandle == null) {
                    null
                } else {
                    val (view: View, clickHandle: (Int, LD) -> Single<Unit>) = viewAndHandle
                    view to { position: Int, data: SumAdapterDataItem<LD, RD> ->
                        clickHandle(position, data.left!!)
                    }
                }
            } else {
                null
            }
        }
    } + rightSpec.itemClicks.map { rightClick ->
        { binding: ViewDataBinding, type: Int ->
            if (rightSpec.canHandleTypes().contains(type)) {
                val clickAndHandle = rightClick(binding as RBinding, type)
                if (clickAndHandle == null) {
                    null
                } else {
                    val (view, clickHandle) = clickAndHandle
                    view to { position: Int, data: SumAdapterDataItem<LD, RD> ->
                        clickHandle(position - childrenSize().blockingGet().first, data.right!!)
                    }
                }
            } else {
                null
            }
        }
    }


}


sealed class SumAdapterDataItem<Left, Right> {
    abstract val left: Left?
    abstract val right: Right?

    class Left<L, R>(override val left: L, override val right: R? = null) :
            SumAdapterDataItem<L, R>()

    class Right<L, R>(override val left: L? = null, override val right: R) :
            SumAdapterDataItem<L, R>()
}

operator fun <LD, RD, LBinding : ViewDataBinding, RBinding : ViewDataBinding> AdapterSpec<LD, LBinding>.plus(
        right: AdapterSpec<RD, RBinding>
) = SumAdapterSpec(leftSpec = this, rightSpec = right)