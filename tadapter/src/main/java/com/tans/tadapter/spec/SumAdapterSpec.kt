package com.tans.tadapter.spec

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.tans.tadapter.adapter.DifferHandler
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.withLatestFrom
import java.lang.RuntimeException

class SumAdapterSpec<LD, RD, LBinding : ViewDataBinding, RBinding : ViewDataBinding>(
        val leftSpec: AdapterSpec<LD, LBinding>,
        val rightSpec: AdapterSpec<RD, RBinding>
) : BaseAdapterSpec<SumAdapterDataItem<LD, RD>, ViewDataBinding>() {

    override val bindData: (position: Int, data: SumAdapterDataItem<LD, RD>, binding: ViewDataBinding) -> Unit = { _, data, binding ->
        when (data) {
            is SumAdapterDataItem.Left -> {
                val lBinding: LBinding? = binding as? LBinding
                if (lBinding != null) {
                    leftSpec.bindData(data.position, data.left, lBinding)
                }
            }
            is SumAdapterDataItem.Right -> {
                val rBinding: RBinding? = (binding as? RBinding)
                if (rBinding != null) {
                    rightSpec.bindData(data.position, data.right, rBinding)
                }
            }
        }
    }

    override val swipeRemove: (position: Int, data: SumAdapterDataItem<LD, RD>) -> Unit = { _, data ->
        when (data) {
            is SumAdapterDataItem.Left -> {
                leftSpec.swipeRemove(data.position, data.left)
            }
            is SumAdapterDataItem.Right -> {
                rightSpec.swipeRemove(data.position, data.right)
            }
        }
    }

    override val bindDataPayload: (position: Int, data: SumAdapterDataItem<LD, RD>, binding: ViewDataBinding, payloads: List<Any>) -> Boolean = { _, data, binding, payloads ->
        when (data) {
            is SumAdapterDataItem.Left -> {
                val lBinding: LBinding? = (binding as? LBinding)
                if (lBinding != null) {
                    leftSpec.bindDataPayload(data.position, data.left, lBinding, payloads)
                } else {
                    false
                }
            }
            is SumAdapterDataItem.Right -> {
                val rBinding: RBinding? = (binding as? RBinding)
                if (rBinding != null) {
                    rightSpec.bindDataPayload(data.position, data.right, rBinding, payloads)
                } else {
                    false
                }
            }
        }
    }

    override val hasStableIds: Boolean = leftSpec.hasStableIds || rightSpec.hasStableIds

    override val itemId: (position: Int, data: SumAdapterDataItem<LD, RD>) -> Long = { _, data ->
        when (data) {
            is SumAdapterDataItem.Left -> {
                leftSpec.itemId(data.position, data.left)
            }
            is SumAdapterDataItem.Right -> {
                rightSpec.itemId(data.position, data.right)
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

    override val dataUpdater: Observable<List<SumAdapterDataItem<LD, RD>>> =
        Observable.merge(leftSpec.dataUpdater
            .toLeft()
            .distinctUntilChanged()
            .withLatestFrom(rightSpec.dataUpdater.toRight())
            .map { it.first + it.second },
            rightSpec.dataUpdater
                .toRight()
                .distinctUntilChanged()
                .withLatestFrom(leftSpec.dataUpdater.toLeft())
                .map { it.second + it.first })

    private fun Observable<List<LD>>.toLeft() = this.map { data ->
        val size = data.size
        data.withIndex().map { (position, item) ->
            SumAdapterDataItem.Left<LD, RD>(left = item, length = size, position = position)
        }
    }

    private fun Observable<List<RD>>.toRight() = this.map { data ->
        val size = data.size
        data.withIndex().map { (position, item) ->
            SumAdapterDataItem.Right<LD, RD>(right = item, length = size, position = position)
        }
    }

    override fun itemType(position: Int, item: SumAdapterDataItem<LD, RD>): Int {
        return when (item) {
             is SumAdapterDataItem.Left -> leftSpec.itemType(item.position, item.left)
             is SumAdapterDataItem.Right -> rightSpec.itemType(item.position, item.right)
        }
    }

    override fun canHandleTypes(): List<Int> =
            leftSpec.canHandleTypes() + rightSpec.canHandleTypes()

    override fun createBinding(
            context: Context,
            parent: ViewGroup,
            viewType: Int
    ): ViewDataBinding = when {
        leftSpec.canHandleTypes().contains(viewType) -> leftSpec.createBinding(context, parent, viewType)
        rightSpec.canHandleTypes().contains(viewType) -> rightSpec.createBinding(context, parent, viewType)
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

    override val itemClicks: List<ItemClick<ViewDataBinding, SumAdapterDataItem<LD, RD>>> =
        leftSpec.itemClicks.map { leftClick ->
            object : ItemClick<ViewDataBinding, SumAdapterDataItem<LD, RD>> {
                override fun invoke(
                    binding: ViewDataBinding,
                    type: Int
                ): Pair<View, (position: Int, data: SumAdapterDataItem<LD, RD>) -> Single<Unit>>? {
                    return if (leftSpec.canHandleTypes().contains(type)) {
                        val viewAndHandle: Pair<View, (Int, LD) -> Single<Unit>>? =
                            leftClick.invoke(binding as LBinding, type)
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

            }
        } + rightSpec.itemClicks.map { rightClick ->
            object : ItemClick<ViewDataBinding, SumAdapterDataItem<LD, RD>> {
                override fun invoke(
                    binding: ViewDataBinding,
                    type: Int
                ): Pair<View, (position: Int, data: SumAdapterDataItem<LD, RD>) -> Single<Unit>>? {
                    return if (rightSpec.canHandleTypes().contains(type)) {
                        val clickAndHandle = rightClick(binding as RBinding, type)
                        if (clickAndHandle == null) {
                            null
                        } else {
                            val (view, clickHandle) = clickAndHandle
                            view to { _: Int, data: SumAdapterDataItem<LD, RD> ->
                                clickHandle(data.position, data.right!!)
                            }
                        }
                    } else {
                        null
                    }
                }

            }
        }


}


sealed class SumAdapterDataItem<Left, Right> {
    abstract val left: Left?
    abstract val right: Right?
    abstract val length: Int
    abstract val position: Int

    class Left<L, R>(
        override val left: L,
        override val length: Int,
        override val position: Int
    ) : SumAdapterDataItem<L, R>() {
        override val right: R? = null
    }

    class Right<L, R>(
        override val right: R,
        override val position: Int,
        override val length: Int
    ) : SumAdapterDataItem<L, R>() {
        override val left: L? = null
    }
}

operator fun <LD, RD, LBinding : ViewDataBinding, RBinding : ViewDataBinding> AdapterSpec<LD, LBinding>.plus(
        right: AdapterSpec<RD, RBinding>
) = SumAdapterSpec(leftSpec = this, rightSpec = right)