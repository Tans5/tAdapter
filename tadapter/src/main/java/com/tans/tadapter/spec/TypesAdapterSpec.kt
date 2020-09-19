package com.tans.tadapter.spec

import android.content.Context
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import com.tans.tadapter.adapter.DifferHandler
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlin.RuntimeException

/**
 *
 * author: pengcheng.tan
 * date: 2019-09-16
 */

class TypesAdapterSpec<D>(
    val layoutIdAndBinding: Map<Int, (parent: ViewGroup) -> ViewDataBinding>,
    val typeHandler: (D) -> Int,
    override val bindData: (Int, D, ViewDataBinding) -> Unit,
    override val dataUpdater: Observable<List<D>>,
    override val differHandler: DifferHandler<D> = DifferHandler()
) : BaseAdapterSpec<D, ViewDataBinding>() {

    override val dataSubject: Subject<List<D>> = BehaviorSubject.create<List<D>>().toSerialized()

    override val lifeCompositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun itemType(position: Int, item: D): Int {
        val layoutId = typeHandler(item)
        return if (layoutIdAndBinding.containsKey(layoutId)) {
            layoutId
        } else {
            throw RuntimeException("Can't deal type $layoutId")
        }
    }

    override fun canHandleTypes(): List<Int> = layoutIdAndBinding.keys.toList()

    override fun createBinding(
        context: Context,
        parent: ViewGroup,
        viewType: Int
    ): ViewDataBinding =
        (layoutIdAndBinding[viewType] ?: error("Can't deal viewType: $viewType")).invoke(parent)


}