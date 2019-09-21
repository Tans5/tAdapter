package com.tans.tadapter.spec

import android.content.Context
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import com.tans.tadapter.DifferHandler
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.lang.RuntimeException

class SumAdapterSpec<LD, RD, LBinding : ViewDataBinding, RBinding : ViewDataBinding>(
    val leftSpec: AdapterSpec<LD, LBinding>,
    val rightSpec: AdapterSpec<RD, RBinding>
) : AdapterSpec<SumAdapterDataItem<LD, RD>, ViewDataBinding> {

    override val lifeCompositeDisposable: CompositeDisposable = CompositeDisposable()

    override val dataSubject: Subject<List<SumAdapterDataItem<LD, RD>>> =
        BehaviorSubject.createDefault<List<SumAdapterDataItem<LD, RD>>>(emptyList()).toSerialized()

    override val bindData: (position: Int, data: SumAdapterDataItem<LD, RD>, binding: ViewDataBinding) -> Unit = { position, data, binding ->
        val (leftSize, rightSize) = childrenSize().blockingGet()
        when (data) {
            is SumAdapterDataItem.Left -> {
                val lBinding: LBinding? = (binding as? LBinding)
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
        }

    override val dataUpdater: Observable<List<SumAdapterDataItem<LD, RD>>> =
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

    override fun adapterAttachToRecyclerView() {
        super.adapterAttachToRecyclerView()
        leftSpec.adapterAttachToRecyclerView()
        rightSpec.adapterAttachToRecyclerView()
    }

    override fun adapterDetachToRecyclerView() {
        super.adapterDetachToRecyclerView()
        leftSpec.adapterDetachToRecyclerView()
        rightSpec.adapterDetachToRecyclerView()
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