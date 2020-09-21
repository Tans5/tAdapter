package com.tans.tadapter.adapter

import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import com.tans.tadapter.core.BindLife
import com.tans.tadapter.spec.AdapterSpec
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

abstract class BaseAdapter<D, Binding : ViewDataBinding>(
    val adapterSpec: AdapterSpec<D, Binding>
) : ListAdapter<D, BaseViewHolder<Binding>>(adapterSpec.differHandler), BindLife by BindLife(),
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

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
        Observable.fromIterable(adapterSpec.itemClicks)
            .flatMap {
                val viewAndClickHandle = it(binding, viewType)
                if (viewAndClickHandle != null) {
                    val (view, clickHandle) = viewAndClickHandle
                    view.clicks()
                        .flatMapSingle {
                            clickHandle(
                                holder.adapterPosition,
                                getItem(holder.adapterPosition)
                            )
                        }
                } else {
                    Observable.empty<Unit>()
                }
            }
            .bindLife()
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
        adapterSpec.dataUpdater
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { submitList(it) }
            .bindLife()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        adapterSpec.adapterDetachToRecyclerView(recyclerView)
        lifeCompositeDisposable.clear()
        cancel()
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