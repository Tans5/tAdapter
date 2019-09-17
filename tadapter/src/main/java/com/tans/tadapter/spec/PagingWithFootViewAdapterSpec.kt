package com.tans.tadapter.spec

import android.content.Context
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import com.tans.tadapter.DifferHandler
import com.tans.tadapter.core.BindLife
import com.tans.tadapter.core.Output
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/**
 *
 * author: pengcheng.tan
 * date: 2019-09-17
 */

class PagingWithFootViewAdapterSpec<D, Binding : ViewDataBinding> : AdapterSpec<D, Binding>, BindLife,
    Output<PagingWithFootViewState> {

    override val lifeCompositeDisposable: CompositeDisposable = CompositeDisposable()

    override val outputSubject: Subject<PagingWithFootViewState> = Output.defaultOutputSubject(PagingWithFootViewState.LoadingMore)


    override val dataSubject: Subject<List<D>> = BehaviorSubject.createDefault<List<D>>(emptyList()).toSerialized()
    override val differHandler: DifferHandler<D> = DifferHandler()

    override fun itemType(position: Int, item: D): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canHandleTypes(): List<Int> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createBinding(context: Context, parent: ViewGroup, viewType: Int): Binding {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun bindData(data: D, binding: Binding) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun dataUpdater(): Observable<List<D>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}


sealed class PagingWithFootViewState {
    object LoadingMore : PagingWithFootViewState()
    object Finish : PagingWithFootViewState()
    object Error : PagingWithFootViewState()
}