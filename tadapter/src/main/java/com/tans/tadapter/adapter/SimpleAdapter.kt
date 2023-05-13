package com.tans.tadapter.adapter

import androidx.databinding.ViewDataBinding
import com.tans.tadapter.spec.AdapterSpec

/**
 *
 * author: pengcheng.tan
 * date: 2019-09-25
 */

class SimpleAdapter<D : Any, Binding : ViewDataBinding>(adapterSpec: AdapterSpec<D, Binding>, onChangeCommit: (list: List<D>) -> Unit = {})
    : BaseAdapter<D, Binding>(adapterSpec, onChangeCommit)