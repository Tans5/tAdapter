package com.tans.tadapter.adapter

import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.paging.PagedListAdapter
import com.tans.tadapter.paging.PagingAdapterSpec

/**
 *
 * author: pengcheng.tan
 * date: 2019-09-26
 */
class BasePagingAdapter<D, Binding : ViewDataBinding>(
    val pagingAdapterSpec: PagingAdapterSpec<D, Binding>
) : PagedListAdapter<D, BaseViewHolder<Binding>>(pagingAdapterSpec.differHandler) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            : BaseViewHolder<Binding> = BaseViewHolder(
        pagingAdapterSpec.createBinding(
            context = parent.context,
            parent = parent,
            viewType = viewType
        )
    )

    override fun onBindViewHolder(holder: BaseViewHolder<Binding>, position: Int) {
        pagingAdapterSpec.bindData(position, getItem(position)!!, holder.binding)
    }

}