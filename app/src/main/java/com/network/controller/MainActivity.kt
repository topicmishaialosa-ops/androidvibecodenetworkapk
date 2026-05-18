package com.network.controller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

data class Device(val name: String, val ip: String, var isOnline: Boolean = false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NetworkControllerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkControllerApp() {
    var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌐 Network Controller") },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isScanning = true
                                devices = scanNetwork()
                                isScanning = false
                                resultMessage = "Найдено ${devices.size} устройств"
                            }
                        },
                        enabled = !isScanning
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("🔍 Сканирование сети...", style = MaterialTheme.typography.bodyMedium)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "📱 Устройства (${devices.size})",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn {
                items(devices) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.name, style = MaterialTheme.typography.titleMedium)
                                Text(device.ip, style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = {
                                scope.launch {
                                    val reachable = pingDevice(device.ip)
                                    resultMessage = "${device.name}: ${if (reachable) "✅ Доступен" else "❌ Не отвечает"}"
                                }
                            }) {
                                Text("Ping")
                            }
                        }
                    }
                }
            }
            
            resultMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

suspend fun scanNetwork(): List<Device> = withContext(Dispatchers.IO) {
    val devices = mutableListOf<Device>()
    val localIp = getLocalIpAddress()
    val baseIp = localIp?.substringBeforeLast('.') ?: "192.168.1"
    
    for (i in 1..254) {
        val ip = "$baseIp.$i"
        try {
            val address = InetAddress.getByName(ip)
            if (address.isReachable(500)) {
                val name = address.canonicalHostName?.takeIf { it != ip } ?: "Устройство"
                devices.add(Device(name, ip, true))
            }
        } catch (e: Exception) { }
    }
    devices
}

suspend fun pingDevice(ip: String): Boolean = withContext(Dispatchers.IO) {
    try {
        InetAddress.getByName(ip).isReachable(1000)
    } catch (e: Exception) {
        false
    }
}

fun getLocalIpAddress(): String? {
    try {
        java.net.NetworkInterface.getNetworkInterfaces().toList().forEach { ni ->
            ni.inetAddresses.toList().forEach { addr ->
                if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                    return addr.hostAddress
                }
            }
        }
    } catch (e: Exception) { }
    return null
}
