package org.waterloorocketry.parsdroid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.chaquo.python.android.AndroidPlatform
import com.chaquo.python.Python
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.Executors

class UsbSerialService : Service(), SerialInputOutputManager.Listener {

    private var usbSerialPort: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val buffer = StringBuilder()

    private lateinit var python: Python
    private lateinit var parsley: PyObject;
    private lateinit var usbParser: PyObject;

    override fun onCreate() {
        super.onCreate()

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        python = Python.getInstance()
        parsley = python.getModule("parsley")
        usbParser = parsley.callAttr("USBDebugParser")

        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }

        if (device != null) {
            connectToDevice(device)
        }

        return START_STICKY
    }

    private fun connectToDevice(device: UsbDevice) {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device) ?: return
        val connection = usbManager.openDevice(driver.device) ?: return

        // Close any existing connection
        ioManager?.stop()
        ioManager = null
        try {
            usbSerialPort?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing previous port", e)
        }
        usbSerialPort = null

        val port = driver.ports[0] // Most devices have just one port
        try {
            port.open(connection)
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            usbSerialPort = port
            
            ioManager = SerialInputOutputManager(port, this)
            ioManager?.start()
            
            SerialManager.setConnected(true)
            Log.d(TAG, "Connected to ${device.deviceName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device", e)
            stopSelf()
        }
    }

    override fun onNewData(data: ByteArray?) {
        if (data == null) return
        synchronized(buffer) {
            buffer.append(String(data))
            val lines = mutableListOf<String>()
            var index: Int
            while (buffer.indexOf("\n").also { index = it } != -1) {
                val line = buffer.substring(0, index).trim()
                buffer.delete(0, index + 1)
                if (line.isNotEmpty()) {
                    lines.add(line)
                }
            }

            if (lines.isNotEmpty()) {
                scope.launch {
                    for (line in lines) {
                        try {
                            val parsed = usbParser.callAttr("parse", line)
                            SerialManager.updateData(JSONObject(parsed.callAttr("model_dump_json").toString()))
                        } catch (e: PyException) {
                            Log.e(TAG, "Error parsing line: $line", e)
                        }
                    }
                }
            }
        }
    }

    override fun onRunError(e: Exception?) {
        Log.e(TAG, "Serial error", e)
        SerialManager.setConnected(false)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        ioManager?.stop()
        usbSerialPort?.close()
        SerialManager.setConnected(false)
        scope.cancel()
        synchronized(buffer) {
            buffer.clear()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "usb_serial_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "USB Serial Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("USB Serial Connected")
            .setContentText("Processing serial data stream...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()
    }

    companion object {
        private const val TAG = "UsbSerialService"
        private const val NOTIFICATION_ID = 1
    }
}
