package com.example.tadapter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tadapter.databinding.LayoutItemType1Binding
import com.example.tadapter.databinding.LayoutItemType2Binding
import com.example.tadapter.databinding.LayoutItemType3Binding
import com.tans.tadapter.SimpleAdapterSpec
import com.tans.tadapter.plus
import com.tans.tadapter.toAdapter
import io.reactivex.Single
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val adapter = (SimpleAdapterSpec<Unit, LayoutItemType1Binding>(layoutId = R.layout.layout_item_type_1,
            dataUpdater = emptyData(),
            bindData = { data: Unit, binding: LayoutItemType1Binding ->
                println("type1 data")
            }) + SimpleAdapterSpec<Unit, LayoutItemType2Binding>(layoutId = R.layout.layout_item_type_2,
            dataUpdater = emptyData(),
            bindData = { data: Unit, binding: LayoutItemType2Binding ->
                println("type2 data")
            }) + SimpleAdapterSpec<Unit, LayoutItemType3Binding>(layoutId = R.layout.layout_item_type_3,
            dataUpdater = emptyData(),
            bindData = { data: Unit, binding: LayoutItemType3Binding ->
                println("type3 data")
            })).toAdapter()
        val adapter1 = SimpleAdapterSpec<Unit, LayoutItemType1Binding>(layoutId = R.layout.layout_item_type_1,
            dataUpdater = emptyData(),
            bindData = { data: Unit, binding: LayoutItemType1Binding ->
                println("type1 data")
            }).toAdapter()
        test_rv.layoutManager = LinearLayoutManager(this)
        test_rv.adapter = adapter

    }

    fun emptyData(size: Int = 10) = Single.just<List<Unit>>(List(size) { Unit }).toObservable()
}
