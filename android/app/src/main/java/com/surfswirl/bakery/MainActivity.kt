package com.surfswirl.bakery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.surfswirl.bakery.ui.App
import com.surfswirl.bakery.ui.GameViewModel

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels { GameViewModel.factory(application as BakeryApp) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App(vm) }
    }

    override fun onResume() {
        super.onResume()
        vm.onForeground()
    }

    override fun onStop() {
        super.onStop()
        vm.onBackground(this)
    }
}
