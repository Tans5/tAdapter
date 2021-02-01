package com.example.tadapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.tadapter.databinding.ActivityMainBinding
import com.example.tadapter.databinding.CatItemLayoutBinding
import com.example.tadapter.databinding.DogItemLayoutBinding
import com.tans.tadapter.core.BindLife
import com.tans.tadapter.core.Stateable
import com.tans.tadapter.spec.SimpleAdapterSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.tans.tadapter.spec.plus
import com.tans.tadapter.spec.toAdapter
import com.tans.tadapter.spec.toSwipeDeleteAdapter

data class MainState(
    val dogs: List<Dog> = emptyList(),
    val cats: List<Cat> = emptyList()
)

class MainActivity : AppCompatActivity(),
    BindLife by BindLife(),
    CoroutineScope by CoroutineScope(Dispatchers.Main),
    Stateable<MainState> by Stateable(MainState()) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        updateState {
            MainState(
                dogs = Array<Dog>(40) { i ->
                    Dog(i, "Jakey")
                }.toList(),
                cats = Array<Cat>(40) { i ->
                    Cat(i, "May")
                }.toList()
            )
        }.bindLife()


        binding.testRv.adapter = (SimpleAdapterSpec<Dog, DogItemLayoutBinding>(
            layoutId = R.layout.dog_item_layout,
            bindData = { _, dog, lBinding -> lBinding.dog = dog },
            dataUpdater = bindState().map { it.dogs },
            swipeRemove = { _, dog ->
                updateState { state -> state.copy(dogs = state.dogs - dog) }.bindLife()
            }
        ) + SimpleAdapterSpec<Cat, CatItemLayoutBinding>(
            layoutId = R.layout.cat_item_layout,
            bindData = { _, cat, lBinding -> lBinding.cat = cat },
            dataUpdater = bindState().map { it.cats },
            swipeRemove = { _, cat ->
                updateState { state -> state.copy(cats = state.cats - cat) }.bindLife()
            }
        )).toSwipeDeleteAdapter(background = ColorDrawable(Color.BLACK)) {

        }

    }

}
