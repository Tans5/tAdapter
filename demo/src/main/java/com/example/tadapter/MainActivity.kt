package com.example.tadapter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.example.tadapter.core.InputOwner
import com.example.tadapter.databinding.ActivityMainBinding
import com.example.tadapter.databinding.LayoutItemType1Binding
import com.example.tadapter.databinding.LayoutItemType2Binding
import com.example.tadapter.databinding.LayoutItemType3Binding
import com.example.tadapter.model.Product
import com.tans.tadapter.DifferHandler
import com.tans.tadapter.spec.SimpleAdapterSpec
import com.tans.tadapter.spec.TypesAdapterSpec
import com.tans.tadapter.spec.plus
import com.tans.tadapter.spec.toAdapter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class MainActivity : AppCompatActivity(), InputOwner {

    override val inputCompositeDisposable: CompositeDisposable = CompositeDisposable()

    val type1NextPage: Subject<Unit> = PublishSubject.create<Unit>()

    val type2NextPage: Subject<Unit> = PublishSubject.create<Unit>()

    val type3NextPage: Subject<Unit> = PublishSubject.create<Unit>()

    val viewModel: MainViewModel by lazy {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        viewModel.setInput(MainInput(type1ProductsNext = type1NextPage,
            type2ProductsNext = type2NextPage,
            type3ProductsNext = type3NextPage), this)
        viewModel.init()

        val sumAdapter = (SimpleAdapterSpec<Product, LayoutItemType1Binding>(
            layoutId = R.layout.layout_item_type_1,
            dataUpdater = viewModel.bindOutputState().map { it.type1Products.second },
            bindData = { data: Product, binding: LayoutItemType1Binding ->
                binding.data = data
                binding.root.setOnClickListener { type1NextPage.onNext(Unit) }
            },
            differHandler = DifferHandler(itemsTheSame = { old, new -> old.id == new.id})
        ) + SimpleAdapterSpec<Product, LayoutItemType2Binding>(layoutId = R.layout.layout_item_type_2,
            dataUpdater = viewModel.bindOutputState().map { it.type2Products.second },
            bindData = { data: Product, binding: LayoutItemType2Binding ->
                binding.data = data
                binding.root.setOnClickListener { type2NextPage.onNext(Unit) }
            },
            differHandler = DifferHandler(itemsTheSame = { old, new -> old.id == new.id})
        ) + SimpleAdapterSpec<Product, LayoutItemType3Binding>(layoutId = R.layout.layout_item_type_3,
            dataUpdater =viewModel.bindOutputState().map { it.type3Products.second },
            bindData = { data: Product, binding: LayoutItemType3Binding ->
                binding.data = data
                binding.root.setOnClickListener { type3NextPage.onNext(Unit) }
            },
            differHandler = DifferHandler(itemsTheSame = { old, new -> old.id == new.id})
        )).toAdapter()

        val typesAdapter = TypesAdapterSpec<Product>(
            layoutIdAndBinding = mapOf(R.layout.layout_item_type_1 to { parent ->
                DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.layout_item_type_1, parent, false)
            }, R.layout.layout_item_type_2 to { parent ->
                DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.layout_item_type_2, parent, false)
            }),
            typeHandler = { if (it.id % 2 == 0) R.layout.layout_item_type_1 else R.layout.layout_item_type_2 },
            dataBind = { data, binding ->
                when (binding) {
                    is LayoutItemType1Binding -> {
                        binding.data = data
                    }

                    is LayoutItemType2Binding -> {
                        binding.data = data
                        binding.root.setOnClickListener { type1NextPage.onNext(Unit) }
                    }
                    else -> {

                    }
                }
            },
            differHandler = DifferHandler(itemsTheSame = { old, new -> old.id == new.id }),
            dataGetter = viewModel.bindOutputState().map { it.type1Products.second }).toAdapter()

        binding.testRv.adapter = typesAdapter

    }
}
