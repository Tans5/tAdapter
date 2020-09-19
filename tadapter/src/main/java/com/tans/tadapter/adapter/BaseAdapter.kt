package com.tans.tadapter.adapter

import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tans.tadapter.core.BindLife
import com.tans.tadapter.spec.AdapterSpec
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

abstract class BaseAdapter<D, Binding : ViewDataBinding>(
    val adapterSpec: AdapterSpec<D, Binding>
) : ListAdapter<D, BaseViewHolder<Binding>>(adapterSpec.differHandler),
    BindLife by BindLife(), CoroutineScope by CoroutineScope(Dispatchers.Main) {

    override fun getItemViewType(position: Int): Int {
        return adapterSpec.itemType(position, getItem(position))
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
        adapterSpec.bindData(position, getItem(position), holder.binding)

    override fun onBindViewHolder(
        holder: BaseViewHolder<Binding>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        adapterSpec.adapterAttachToRecyclerView()
        adapterSpec.dataSubject
            .distinctUntilChanged()
            .doOnNext { submitList(it) }
            .bindLife()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        adapterSpec.adapterDetachToRecyclerView()
        lifeCompositeDisposable.clear()
        cancel()
    }
}

open class DifferHandler<D>(
    val itemsTheSame: (oldItem: D, newItem: D) -> Boolean = { _, _ -> false },
    val contentTheSame: (oldItem: D, newItem: D) -> Boolean = { _, _ -> false }) : DiffUtil.ItemCallback<D>() {

    override fun areItemsTheSame(oldItem: D, newItem: D): Boolean = itemsTheSame(oldItem, newItem)

    override fun areContentsTheSame(oldItem: D, newItem: D): Boolean =
        contentTheSame(oldItem, newItem)
}

open class BaseViewHolder<Binding : ViewDataBinding>(val binding: Binding) :
    RecyclerView.ViewHolder(binding.root)