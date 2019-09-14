package com.tans.tadapter

import android.content.Context
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.lang.RuntimeException

class SumAdapterSpec<LD, RD, LBinding : ViewDataBinding, RBinding : ViewDataBinding>(
    val leftSpec: AdapterSpec<LD, LBinding>,
    val rightSpec: AdapterSpec<RD, RBinding>
) : AdapterSpec<SumAdapterDataItem<LD, RD>, ViewDataBinding> {

    override val dataSubject: Subject<List<SumAdapterDataItem<LD, RD>>> =
        BehaviorSubject.createDefault<List<SumAdapterDataItem<LD, RD>>>(emptyList()).toSerialized()

    override fun dataUpdater(): Observable<List<SumAdapterDataItem<LD, RD>>> {
        val l: Observable<List<SumAdapterDataItem<LD, RD>>> = leftSpec.dataUpdater()
            .toLeft()
            .distinctUntilChanged()
            .withLatestFrom(rightSpec.dataSubject.toRight())
            .map { it.first + it.second }
        val r: Observable<List<SumAdapterDataItem<LD, RD>>> = rightSpec.dataUpdater()
            .toRight()
            .distinctUntilChanged()
            .withLatestFrom(leftSpec.dataSubject.toLeft())
            .map { it.second + it.first }

        return Observable.merge(l, r)
            .doOnNext { dataSubject.onNext(it) }
    }

    private fun Observable<List<LD>>.toLeft() = this.map { data ->
        data.map { item -> SumAdapterDataItem.Left<LD, RD>(left = item) }
    }

    private fun Observable<List<RD>>.toRight() = this.map { data ->
        data.map { item -> SumAdapterDataItem.Right<LD, RD>(right = item) }
    }

    private fun isLeft(position: Int): Maybe<Boolean> = dataSubject.firstElement()
        .flatMap {
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
            when (position) {
                in 0 until leftItemSize -> Maybe.just(true)
                in leftItemSize until leftItemSize + rightItemSize -> Maybe.just(false)
                else -> Maybe.empty<Boolean>()
            }
        }

    override fun itemType(position: Int): Int = isLeft(position)
        .map {
            if (it) {
                leftSpec.itemType(position)
            } else {
                rightSpec.itemType(position)
            }
        }
        .toSingle(0)
        .blockingGet()

    override fun canHandleTypes(): List<Int> = leftSpec.canHandleTypes() + rightSpec.canHandleTypes()

    override fun createBinding(
        context: Context,
        parent: ViewGroup,
        viewType: Int
    ): ViewDataBinding = when  {
            leftSpec.canHandleTypes().contains(viewType) -> leftSpec.createBinding(context, parent, viewType)
            rightSpec.canHandleTypes().contains(viewType) -> rightSpec.createBinding(context, parent, viewType)
            else -> throw RuntimeException("Can't deal viewType: $viewType")
        }


    override fun bindData(data: SumAdapterDataItem<LD, RD>, binding: ViewDataBinding) {
        when (data) {
            is SumAdapterDataItem.Left -> {
                val lBinding: LBinding? = (binding as? LBinding)
                if (lBinding != null) {
                    leftSpec.bindData(data.left, lBinding)
                }
            }
            is SumAdapterDataItem.Right -> {
                val rBinding: RBinding? = (binding as? RBinding)
                if (rBinding != null) {
                    rightSpec.bindData(data.right, rBinding)
                }
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

operator fun <LD, RD, LBinding : ViewDataBinding, RBinding : ViewDataBinding> AdapterSpec<LD, LBinding>.plus(right: AdapterSpec<RD, RBinding>)
= SumAdapterSpec(leftSpec = this, rightSpec = right)