package org.waterloorocketry.parsdroid

import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.waterloorocketry.parsdroid.ui.theme.ParsdroidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    
    DashboardContent(
        uiState = uiState,
        messages = messages,
        onKeepScreenOnToggle = { viewModel.toggleKeepScreenOn(it) },
        onAutoScrollToggle = { viewModel.toggleAutoScroll(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    uiState: DashboardState,
    messages: List<String>,
    onKeepScreenOnToggle: (Boolean) -> Unit = {},
    onAutoScrollToggle: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    DisposableEffect(uiState.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (uiState.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Live Telemetry",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_adaptive_fore),
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(70.dp)
                            .padding(start = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DashboardCard(label = "GPS Coordinate", value = "${uiState.latitude},${uiState.longitude}")

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardCard(label = "GPS Satellites", value = uiState.numSatellites)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardCard(label = "GPS Time", value = uiState.timestamp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                DashboardCard(label = "Live Telemetry RSSI", value = uiState.rssi)

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardCard(label = "Battery Voltage", value = uiState.batteryVoltage)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardCard(label = "Battery Current", value = uiState.batteryCurrent)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = uiState.keepScreenOn,
                            onCheckedChange = onKeepScreenOnToggle
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Keep Screen On",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    StatusBadge(uiState.isConnected)
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Message Log",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Auto-scroll",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = uiState.isAutoScrollEnabled,
                        onCheckedChange = onAutoScrollToggle,
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }

            val logState = rememberLazyListState()
            LaunchedEffect(messages.size, uiState.isAutoScrollEnabled) {
                if (messages.isNotEmpty() && uiState.isAutoScrollEnabled) {
                    logState.animateScrollToItem(messages.size - 1)
                }
            }

            LazyColumn(
                state = logState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 200.dp)
                    .background(Color.Black.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages) { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(isConnected: Boolean) {
    Surface(
        color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = if (isConnected) "CONNECTED" else "DISCONNECTED",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun DashboardCard(label: String, value: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                clipboardManager.setText(AnnotatedString(value))
                Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    val mockState = DashboardState(
        latitude = "43° 28.3692' N",
        longitude = "80° 33.9810' W",
        numSatellites = "12",
        timestamp = "23:09:36",
        batteryVoltage = "12.40 V",
        batteryCurrent = "350 mA",
        rssi = "-51 dBm",
        isConnected = true
    )
    ParsdroidTheme {
        DashboardContent(
            uiState = mockState,
            messages = listOf(
                "[18:00:01] GPS_LATITUDE: {\"degs\": 43, \"mins\": 28, \"dmins\": 3692, \"direction\": \"N\"}",
                "[18:00:02] GPS_LONGITUDE: {\"degs\": 80, \"mins\": 33, \"dmins\": 9810, \"direction\": \"W\"}",
                "[18:00:03] TELEMETRY_INFO (ROCKET): {\"rssi\": -51, \"lqi\": 20}"
            ),
            onKeepScreenOnToggle = {},
            onAutoScrollToggle = {}
        )
    }
}
