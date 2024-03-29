package com.tans.tadapter.spec

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.tans.tadapter.adapter.DifferHandler
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlin.RuntimeException

/**
 *
 * author: pengcheng.tan
 * date: 2019-09-16
 */

class TypesAdapterSpec<D : Any>(
    val layoutIdAndBinding: Map<Int, (parent: ViewGroup) -> ViewDataBinding>,
    val typeHandler: (D) -> Int,
    override val bindData: (Int, D, ViewDataBinding) -> Unit,
    override val bindDataPayload: (position: Int, data: D, binding: ViewDataBinding, payloads: List<Any>) -> Boolean = { _, _, _, _ -> false },
    override val dataUpdater: Observable<List<D>>,
    override val differHandler: DifferHandler<D> = DifferHandler(),
    override val itemId: (position: Int, data: D) -> Long = { _, _ -> RecyclerView.NO_ID },
    override val hasStableIds: Boolean = false,
    override val itemClicks: List<(binding: ViewDataBinding, type: Int) -> Pair<View, (position: Int, data: D) -> Single<Unit>>?> = emptyList(),
    override val swipeRemove: (position: Int, data: D) -> Unit = { _, _ -> }
) : BaseAdapterSpec<D, ViewDataBinding>() {

    override fun itemType(position: Int, item: D): Int {
        val layoutId = typeHandler(item)
        return if (layoutIdAndBinding.containsKey(layoutId)) {
            layoutId
        } else {
            throw RuntimeException("Can't deal type $layoutId")
        }
    }

    override fun canHandleTypes(): List<Int> = layoutIdAndBinding.keys.toList()

    override fun createBinding(
            context: Context,
            parent: ViewGroup,
            viewType: Int
    ): ViewDataBinding =
            (layoutIdAndBinding[viewType] ?: error("Can't deal viewType: $viewType")).invoke(parent)


}