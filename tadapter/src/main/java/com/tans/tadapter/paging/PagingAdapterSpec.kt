package com.tans.tadapter.paging

import android.content.Context
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.paging.PagedList
import com.tans.tadapter.adapter.DifferHandler
import com.tans.tadapter.core.BindLife
import io.reactivex.subjects.Subject

/**
 *
 * author: pengcheng.tan
 * date: 2019-09-26
 */

interface PagingAdapterSpec <D, Binding : ViewDataBinding> : BindLife {

    val dataSubject: Subject<PagedList<D>>

    val differHandler: DifferHandler<D>

    val bindData: (position: Int, data: D, binding: Binding) -> Unit

    fun itemType(position: Int, item: D): Int

    fun canHandleTypes(): List<Int>

    fun createBinding(context: Context, parent: ViewGroup, viewType: Int): Binding

    fun adapterAttachToRecyclerView() {
    }

    fun adapterDetachToRecyclerView() {
        lifeCompositeDisposable.clear()
    }

}