package com.tans.tadapter.spec

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.tans.tadapter.adapter.BaseAdapter
import com.tans.tadapter.adapter.DifferHandler
import com.tans.tadapter.adapter.SimpleAdapter
import com.tans.tadapter.adapter.SwipeToRemoveAdapter
import com.tans.tadapter.core.BindLife
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.internal.subscriptions.SubscriptionHelper.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

typealias ItemClick<Binding, D> = ((binding: Binding, type: Int) -> (Pair<View, (position: Int, data: D) -> Single<Unit>>?))

interface AdapterSpec<D, Binding : ViewDataBinding> {

    val differHandler: DifferHandler<D>

    val dataUpdater: Observable<List<D>>

    val bindData: (position: Int, data: D, binding: Binding) -> Unit

    val bindDataPayload: (position: Int, data: D, binding: Binding, payloads: List<Any>) -> Boolean

    val itemId: (position: Int, data: D) -> Long

    val hasStableIds: Boolean

    val itemClicks: List<ItemClick<Binding, D>>

    val swipeRemove: (position: Int, data: D) -> Unit

    fun itemType(position: Int, item: D): Int

    fun canHandleTypes(): List<Int>

    fun createBinding(context: Context, parent: ViewGroup, viewType: Int): Binding

    fun adapterAttachToRecyclerView(recyclerView: RecyclerView) {
    }

    fun adapterDetachToRecyclerView(recyclerView: RecyclerView) {
    }

}

fun <D, Binding : ViewDataBinding> AdapterSpec<D, Binding>.toAdapter()
        : BaseAdapter<D, Binding> = SimpleAdapter(this)

fun <D, Binding : ViewDataBinding> AdapterSpec<D, Binding>.toSwipeDeleteAdapter(
    deleteIcon: Drawable? = null,
    background: Drawable,
    onChangeCommit: (list: List<D>) -> Unit
): BaseAdapter<D, Binding> = SwipeToRemoveAdapter(
        adapterSpec = this,
        deleteIcon = deleteIcon,
        background = background,
        onChangeCommit = onChangeCommit
)

