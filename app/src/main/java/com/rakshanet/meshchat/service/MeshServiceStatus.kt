package com.rakshanet.meshchat.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object MeshServiceStatus {
    private val _status = MutableStateFlow("Background relay stopped")
    val status = _status.asStateFlow()

    fun set(value: String) {
        _status.value = value
    }
}
