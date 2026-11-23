package com.surfswirl.bakery

import android.app.Application
import com.surfswirl.bakery.data.SaveStore
import com.surfswirl.bakery.fx.Haptics
import com.surfswirl.bakery.fx.Sfx

class BakeryApp : Application() {
    val store by lazy { SaveStore(this) }
    val sfx by lazy { Sfx(this) }
    val haptics by lazy { Haptics(this) }
    val music by lazy { com.surfswirl.bakery.fx.Music() }
}
