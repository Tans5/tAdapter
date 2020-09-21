package com.example.tadapter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProviders
import com.example.tadapter.core.InputOwner
import com.example.tadapter.databinding.*
import com.example.tadapter.model.Product
import com.example.tadapter.utils.callToObservable
import com.tans.tadapter.adapter.DifferHandler
import com.tans.tadapter.spec.*
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class MainActivity : AppCompatActivity(), InputOwner {

    override val inputCompositeDisposable: CompositeDisposable = CompositeDisposable()

    val type1NextPage: Subject<Unit> = PublishSubject.create<Unit>()

    val type2NextPage: Subject<Unit> = PublishSubject.create<Unit>()

    val type3NextPage: Subject<Unit> = PublishSubject.create()

    val viewModel: MainViewModel by lazy {
        ViewModelProviders.of(this).get(MainViewModel::class.java)
    }

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)


        val (type1RemoveRx, type1RemoveCall) = callToObservable<Product>()

        val (type1ItemChangeRx, type1ItemChangeCall) = callToObservable<Product>()

        viewModel.setInput(MainInput(type1ProductsNext = type1NextPage,
            type1Remove = type1RemoveRx,
            type1ItemChanged = type1ItemChangeRx,
            type2ProductsNext = type2NextPage,
            type3ProductsNext = type3NextPage), this)
        viewModel.init()

        val simpleAdapter = SimpleAdapterSpec<Product, LayoutItemType1Binding>(
            layoutId = R.layout.layout_item_type_1,
            dataUpdater = viewModel.bindOutputState().map { it.type1Products.second },
            bindData = { _, data: Product, binding: LayoutItemType1Binding ->
                binding.data = data
            },
            differHandler = DifferHandler(
                itemsTheSame = { old, new -> old.id == new.id },
                contentTheSame = { old, new -> old == new }),
            itemClicks = listOf { binding, _ ->
                binding.root to { position, data -> Single.fromCallable {
                    Toast.makeText(this, position.toString(), Toast.LENGTH_SHORT).show()
                    type1NextPage.onNext(Unit)
                }}
            }
        )
            .emptyView<Product, LayoutItemType1Binding, LayoutEmptyBinding>(R.layout.layout_empty, true)
            .errorView<SumAdapterDataItem<Product, Unit>, ViewDataBinding, LayoutErrorBinding>(errorLayout = R.layout.layout_error,
                errorChecker = viewModel.bindOutputState()
                    .distinctUntilChanged()
                    .map { it.type1Products.first }
                    .flatMapMaybe { if (it.page == 5) Maybe.just(Throwable("TestError")) else Maybe.empty() })
            .toAdapter()

        val sumAdapter = (SimpleAdapterSpec<Product, LayoutItemType1Binding>(
            layoutId = R.layout.layout_item_type_1,
            dataUpdater = viewModel.bindOutputState().map { it.type1Products.second },
            bindData = { _, data: Product, binding: LayoutItemType1Binding ->
                binding.data = data
            },
            itemClicks = listOf { binding, _ ->
                binding.root to { position, _ ->
                    Single.fromCallable {
                        type1NextPage.onNext(Unit)
                        Toast.makeText(this, "Type1, Position: $position", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            differHandler = DifferHandler(itemsTheSame = { old, new -> old.id == new.id })
        ) + SimpleAdapterSpec<Product, LayoutItemType2Binding>(layoutId = R.layout.layout_item_type_2,
            dataUpdater = viewModel.bindOutputState().map { it.type2Products.second },
            bindData = { _, data: Product, binding: LayoutItemType2Binding ->
                binding.data = data
            },
            itemClicks = listOf { binding, _ ->
                binding.root to { position, _ ->
                    Single.fromCallable {
                        type2NextPage.onNext(Unit)
                        Toast.makeText(this, "Type2, Position: $position", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            differHandler = DifferHandler(
                itemsTheSame = { old, new -> old.id == new.id },
                contentTheSame = { old, new -> old == new })
        ) + SimpleAdapterSpec<Product, LayoutItemType3Binding>(layoutId = R.layout.layout_item_type_3,
            dataUpdater =viewModel.bindOutputState().map { it.type3Products.second },
            bindData = { _, data: Product, binding: LayoutItemType3Binding ->
                binding.data = data
            },
            itemClicks = listOf { binding, _ ->
                binding.root to { position, _ ->
                    Single.fromCallable {
                        type3NextPage.onNext(Unit)
                        Toast.makeText(this, "Type3, Position: $position", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            differHandler = DifferHandler(
                itemsTheSame = { old, new -> old.id == new.id },
                contentTheSame = { old, new -> old == new })
        )).toAdapter()

        val typesAdapter = TypesAdapterSpec<Product>(
            layoutIdAndBinding = mapOf(R.layout.layout_item_type_1 to { parent ->
                DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.layout_item_type_1, parent, false)
            }, R.layout.layout_item_type_2 to { parent ->
                DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.layout_item_type_2, parent, false)
            }),
            typeHandler = { if (it.id % 2 == 0) R.layout.layout_item_type_1 else R.layout.layout_item_type_2 },
            bindData = { _, data, binding ->
                when (binding) {
                    is LayoutItemType1Binding -> {
                        binding.data = data
                    }

                    is LayoutItemType2Binding -> {
                        binding.data = data
                    }
                    else -> {

                    }
                }
            },
            itemClicks = listOf { binding, type ->
                when (binding) {
                    is LayoutItemType1Binding -> {
                        binding.root to { _, data ->
                            Single.fromCallable { type1ItemChangeCall(data.copy(name = "New Name")) }
                        }
                    }
                    is LayoutItemType2Binding -> {
                        binding.root to { _, _ ->
                            Single.fromCallable { type1NextPage.onNext(Unit) }
                        }
                    }
                    else -> null
                }
            },
            bindDataPayload = { position: Int, data: Product, binding: ViewDataBinding, payloads: List<Any> ->
                if (payloads.isNotEmpty()) {
                    when (binding) {
                        is LayoutItemType1Binding -> {
                            binding.nameTv.text = "Type1" + data.name
                        }

                        is LayoutItemType2Binding -> {
                            binding.nameTv.text = "Type2" + data.name
                        }
                        else -> {

                        }
                    }
                    true
                } else {
                    false
                }
            },
            differHandler = DifferHandler(
                itemsTheSame = { old, new -> old.id == new.id },
                contentTheSame = { old, new -> old == new },
                changePayLoad = { old, new ->
                    if (old.name != new.name) {
                        Any()
                    } else {
                        null
                    }
                }),
            dataUpdater = viewModel.bindOutputState().map { it.type1Products.second },
            hasStableIds = true,
            itemId = { _, data -> data.id.toLong() })
            .pagingWithFootView<Product, ViewDataBinding, LayoutItemLoadingBinding, LayoutItemErrorBinding>(
                loadingLayoutId = R.layout.layout_item_loading,
                errorLayoutId = R.layout.layout_item_error,
                loadNextPage = {
                    type1NextPage.onNext(Unit)
                },
                loadingStateUpdater = viewModel.bindOutputState()
                    .distinctUntilChanged()
                    .map {
                        val (nextParams, list) = it.type1Products
                        when {
                            nextParams.finished -> PagingWithFootViewState.Finish
                            nextParams.isError -> PagingWithFootViewState.Error(e = Throwable("Loading More Error"))
                            list.isEmpty() -> PagingWithFootViewState.InitLoading
                            else -> PagingWithFootViewState.LoadingMore
                        }
                    }
            )
            .toAdapter()

        val swipeToRemoveAdapter = SimpleAdapterSpec<Product, LayoutItemType1Binding>(
            layoutId = R.layout.layout_item_type_1,
            bindData = { _, data, binding ->
                binding.data = data
            },
            dataUpdater = viewModel.bindOutputState().map { it.type1Products.second },
            differHandler = DifferHandler(itemsTheSame = { oldItem, newItem -> oldItem.id == newItem.id }, contentTheSame = { o, n -> o == n })
        ).toSwipeDeleteAdapter(background = resources.getDrawable(R.color.colorAccent, null)) { position, item ->
            type1RemoveCall(item)
        }

        binding.testRv.adapter = typesAdapter

    }
}
