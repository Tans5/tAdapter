package com.tans.tadapter.spec

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.tans.tadapter.core.BindLife
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

abstract class BaseAdapterSpec<D : Any, Binding : ViewDataBinding> : AdapterSpec<D, Binding>,
    BindLife by BindLife(), CoroutineScope by CoroutineScope(Dispatchers.Main) {

    override fun adapterDetachToRecyclerView(recyclerView: RecyclerView) {
        lifeCompositeDisposable.clear()
        cancel()
    }

}