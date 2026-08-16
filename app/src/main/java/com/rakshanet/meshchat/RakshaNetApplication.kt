package com.rakshanet.meshchat

import android.app.Application

class RakshaNetApplication : Application() {
    val meshRuntime: MeshRuntime by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MeshRuntime(applicationContext)
    }
}
