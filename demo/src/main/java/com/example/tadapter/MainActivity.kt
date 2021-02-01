package com.example.tadapter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.tadapter.databinding.ActivityMainBinding
import com.tans.tadapter.core.BindLife
import com.tans.tadapter.core.Stateable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : AppCompatActivity(),
    BindLife by BindLife(),
    CoroutineScope by CoroutineScope(Dispatchers.Main),
    Stateable<Unit> by Stateable(Unit) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
    }

}
