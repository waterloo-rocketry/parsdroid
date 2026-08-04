package org.waterloorocketry.parsdroid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardState(
    val latitude: String = "--",
    val longitude: String = "--",
    val numSatellites: String = "--",
    val timestamp: String = "--",
    val batteryVoltage: String = "--",
    val batteryCurrent: String = "--",
    val rssi: String = "--",
    val keepScreenOn: Boolean = false,
    val isAutoScrollEnabled: Boolean = true,
    val isConnected: Boolean = false
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    val isConnected = SerialManager.isConnected

    init {
        viewModelScope.launch {
            SerialManager.latestData.collect { json ->
                updateState(json)
            }
        }
        viewModelScope.launch {
            SerialManager.isConnected.collect { connected ->
                _uiState.update { it.copy(isConnected = connected) }
            }
        }
    }

    fun toggleKeepScreenOn(enabled: Boolean) {
        _uiState.update { it.copy(keepScreenOn = enabled) }
    }

    fun toggleAutoScroll(enabled: Boolean) {
        _uiState.update { it.copy(isAutoScrollEnabled = enabled) }
    }

    private fun updateState(json: JSONObject) {
        val boardTypeId = json.optString("board_type_id")
        val msgType = json.optString("msg_type")
        val metadata = json.optString("msg_metadata", "")
        val data = json.optJSONObject("data") ?: return
        
        val systemTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

        _uiState.update { currentState ->
            var newState = currentState

            if (boardTypeId == "GPS") {
                when (msgType) {
                    "GPS_LATITUDE" -> {
                        val lat = "${data.optInt("degs")}°${data.optInt("mins")}.${data.optInt("dmins").toString().padStart(4, '0')}'${data.optString("direction")}"
                        newState = newState.copy(latitude = lat)
                    }
                    "GPS_LONGITUDE" -> {
                        val lon = "${data.optInt("degs")}°${data.optInt("mins")}.${data.optInt("dmins").toString().padStart(4, '0')}'${data.optString("direction")}"
                        newState = newState.copy(longitude = lon)
                    }
                    "GPS_TIMESTAMP" -> {
                        val ts = "${data.optInt("hrs").toString().padStart(2, '0')}:${data.optInt("mins").toString().padStart(2, '0')}:${data.optInt("secs").toString().padStart(2, '0')}"
                        newState = newState.copy(timestamp = ts)
                    }
                    "GPS_INFO" -> {
                        newState = newState.copy(numSatellites = data.optInt("num_sats").toString())
                    }
                }
            } else if (boardTypeId == "POWER") {
                if (msgType == "SENSOR_ANALOG16") {
                    when (metadata) {
                        "SENSOR_BATT_VOLT" -> {
                            val volts = data.optDouble("value") / 1000.0
                            newState = newState.copy(batteryVoltage = String.format(Locale.US, "%.1f V", volts))
                        }
                        "SENSOR_BATT_CURR" -> {
                            val curr = data.optInt("value")
                            newState = newState.copy(batteryCurrent = String.format(Locale.US, "%d mA", curr))
                        }
                    }
                }
            } else if (msgType == "TELEMETRY_INFO" && metadata == "ROCKET") {
                val rssi = data.optInt("rssi")
                newState = newState.copy(rssi = if (rssi != -128) "$rssi dBm" else "--")
            }

            if(newState.isAutoScrollEnabled) {
                val metadataPart = if (metadata.isNotEmpty() && metadata != "0") " ($metadata)" else ""
                val logEntry = "[$systemTime] $msgType $metadataPart: $data"
                _messages.update { (it + logEntry).takeLast(100) }
            }

            newState
        }
    }
}
