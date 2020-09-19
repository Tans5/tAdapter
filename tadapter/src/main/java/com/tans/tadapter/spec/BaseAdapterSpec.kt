package com.tans.tadapter.spec

import androidx.databinding.ViewDataBinding
import com.tans.tadapter.core.BindLife
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

abstract class BaseAdapterSpec<D, Binding : ViewDataBinding> : AdapterSpec<D, Binding>, BindLife by BindLife(), CoroutineScope by CoroutineScope(Dispatchers.Main)