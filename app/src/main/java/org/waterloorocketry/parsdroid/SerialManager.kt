package org.waterloorocketry.parsdroid

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

object SerialManager {
    private val _latestData = MutableSharedFlow<JSONObject>(extraBufferCapacity = 100)
    val latestData: SharedFlow<JSONObject> = _latestData.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    fun updateData(data: JSONObject) {
        _latestData.tryEmit(data)
    }

    fun setConnected(connected: Boolean) {
        _isConnected.value = connected
    }
}
