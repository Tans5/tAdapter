package com.tans.tadapter

import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class BaseAdapter<D, Binding : ViewDataBinding>(
    val adapterSpec: AdapterSpec<D, Binding>,
    val differCallBack: DiffUtil.ItemCallback<D> = defaultDifferCallBack()
) : ListAdapter<D, BaseViewHolder<Binding>>(differCallBack), BindLife {

    override fun getItemViewType(position: Int): Int {
        return adapterSpec.itemType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Binding> =
        BaseViewHolder(
            adapterSpec.createBinding(
                context = parent.context,
                parent = parent,
                viewType = viewType
            )
        )

    override fun onBindViewHolder(holder: BaseViewHolder<Binding>, position: Int) =
        adapterSpec.bindData(getItem(position), holder.binding)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        adapterSpec.dataUpdater()
            .doOnNext { submitList(it) }
            .bindLife()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        lifeCompositeDisposable.clear()
    }
}

fun <D> defaultDifferCallBack() = object : DiffUtil.ItemCallback<D>() {
    override fun areItemsTheSame(oldItem: D, newItem: D): Boolean = false
    override fun areContentsTheSame(oldItem: D, newItem: D): Boolean = false
}

open class BaseViewHolder<Binding : ViewDataBinding>(val binding: Binding) :
    RecyclerView.ViewHolder(binding.root)