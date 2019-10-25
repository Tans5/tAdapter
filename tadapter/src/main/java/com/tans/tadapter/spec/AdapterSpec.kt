package com.tans.tadapter.spec

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.tans.tadapter.adapter.BaseAdapter
import com.tans.tadapter.adapter.DifferHandler
import com.tans.tadapter.adapter.SimpleAdapter
import com.tans.tadapter.adapter.SwipeToRemoveAdapter
import com.tans.tadapter.core.BindLife
import io.reactivex.Observable
import io.reactivex.subjects.Subject

interface AdapterSpec<D, Binding : ViewDataBinding> : BindLife {

    val dataSubject: Subject<List<D>>

    val differHandler: DifferHandler<D>

    val dataUpdater: Observable<List<D>>

    val bindData: (position: Int, data: D, binding: Binding) -> Unit

    val bindDataPayload: (position: Int, data: D, binding: Binding, payloads: List<Any>) -> Boolean

    fun itemType(position: Int, item: D): Int

    fun canHandleTypes(): List<Int>

    fun createBinding(context: Context, parent: ViewGroup, viewType: Int): Binding

    fun adapterAttachToRecyclerView(recyclerView: RecyclerView) {
        dataUpdater
                .distinctUntilChanged()
                .doOnNext {
                    dataSubject.onNext(it)
                }
                .bindLife()
    }

    fun adapterDetachToRecyclerView(recyclerView: RecyclerView) {
        lifeCompositeDisposable.clear()
    }

}

fun <D, Binding : ViewDataBinding> AdapterSpec<D, Binding>.toAdapter()
        : BaseAdapter<D, Binding> = SimpleAdapter(this)

fun <D, Binding : ViewDataBinding> AdapterSpec<D, Binding>.toSwipeDeleteAdapter(
        deleteIcon: Drawable? = null,
        background: Drawable,
        removeCallback: (position: Int, item: D) -> Unit
)
        : BaseAdapter<D, Binding> = SwipeToRemoveAdapter(
        adapterSpec = this,
        deleteIcon = deleteIcon,
        background = background,
        removeCallBack = removeCallback
)

