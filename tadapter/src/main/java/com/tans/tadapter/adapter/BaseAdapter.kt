package com.tans.tadapter.adapter

import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tans.tadapter.core.BindLife
import com.tans.tadapter.spec.AdapterSpec
import io.reactivex.disposables.CompositeDisposable

abstract class BaseAdapter<D, Binding : ViewDataBinding>(val adapterSpec: AdapterSpec<D, Binding>) : ListAdapter<D, BaseViewHolder<Binding>>(
    adapterSpec.differHandler
),
    BindLife {

    override val lifeCompositeDisposable: CompositeDisposable = CompositeDisposable()

    init {
        this.setHasStableIds(adapterSpec.hasStableIds)
    }

    override fun getItemViewType(position: Int): Int {
        return adapterSpec.itemType(position, getItem(position))
    }

    override fun getItemId(position: Int): Long {
        return adapterSpec.itemId(position, getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Binding> {
        val binding = adapterSpec.createBinding(
            context = parent.context,
            parent = parent,
            viewType = viewType
        )
        val holder = BaseViewHolder(binding)
        adapterSpec.itemClicks.forEach {
            val (view, handle) = it(binding, viewType)
            view.setOnClickListener {
                handle(holder.adapterPosition, getItem(holder.adapterPosition))
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: BaseViewHolder<Binding>, position: Int) {
        adapterSpec.bindData(position, getItem(position), holder.binding)
        holder.binding.executePendingBindings()
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<Binding>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty() || !adapterSpec.bindDataPayload(
                position,
                getItem(position),
                holder.binding,
                payloads
            )
        ) {
            onBindViewHolder(holder, position)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        adapterSpec.adapterAttachToRecyclerView(recyclerView)
        adapterSpec.dataSubject
            .distinctUntilChanged()
            .doOnNext { submitList(it) }
            .bindLife()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        adapterSpec.adapterDetachToRecyclerView(recyclerView)
        lifeCompositeDisposable.clear()
    }
}

open class DifferHandler<D>(
    val itemsTheSame: (oldItem: D, newItem: D) -> Boolean = { o, n -> o == n },
    val contentTheSame: (oldItem: D, newItem: D) -> Boolean = { o, n -> o == n },
    val changePayLoad: (oldItem: D, newItem: D) -> Any? = { _, _ -> null }
) : DiffUtil.ItemCallback<D>() {

    override fun areItemsTheSame(oldItem: D, newItem: D): Boolean = itemsTheSame(oldItem, newItem)

    override fun areContentsTheSame(oldItem: D, newItem: D): Boolean =
        contentTheSame(oldItem, newItem)

    override fun getChangePayload(oldItem: D, newItem: D): Any? = changePayLoad(oldItem, newItem)
}

open class BaseViewHolder<Binding : ViewDataBinding>(val binding: Binding) :
    RecyclerView.ViewHolder(binding.root)