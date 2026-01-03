package com.unisight.gropos.features.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.PrimaryButton
import com.unisight.gropos.core.device.HardwareConfig
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * Hardware Settings Screen
 * 
 * Per REMEDIATION_CHECKLIST: Hardware Settings - COM port configuration for scanner/scale.
 * Per DESKTOP_HARDWARE.md: Configure serial ports for peripherals.
 * 
 * Allows configuration of:
 * - Scanner COM port
 * - Printer COM port
 * - Scale COM port
 * - Cash drawer port
 * - Payment terminal IP/port
 * - Simulated hardware mode
 */
class HardwareSettingsScreen : Screen {
    
    @Composable
    override fun Content() {
        // In a real implementation, this would use a ViewModel
        var config by remember { mutableStateOf(HardwareConfig()) }
        var availablePorts by remember { mutableStateOf(listOf("COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8")) }
        
        HardwareSettingsContent(
            config = config,
            availablePorts = availablePorts,
            onConfigChanged = { config = it },
            onRefreshPorts = { /* Refresh available ports */ },
            onSave = { /* Save configuration */ },
            onBack = { /* Navigate back */ }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareSettingsContent(
    config: HardwareConfig,
    availablePorts: List<String>,
    onConfigChanged: (HardwareConfig) -> Unit,
    onRefreshPorts: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hardware Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshPorts) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh ports"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GroPOSColors.PrimaryGreen,
                    titleContentColor = GroPOSColors.White,
                    navigationIconContentColor = GroPOSColors.White,
                    actionIconContentColor = GroPOSColors.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(GroPOSSpacing.M)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
        ) {
            // Simulated Mode Toggle
            SimulatedModeCard(
                useSimulated = config.useSimulatedHardware,
                onToggle = { onConfigChanged(config.copy(useSimulatedHardware = it)) }
            )
            
            // Scanner Settings
            PeripheralCard(
                title = "Barcode Scanner",
                icon = "📷",
                selectedPort = config.scannerPort,
                availablePorts = availablePorts,
                onPortSelected = { onConfigChanged(config.copy(scannerPort = it)) },
                enabled = !config.useSimulatedHardware
            )
            
            // Printer Settings
            PeripheralCard(
                title = "Receipt Printer",
                icon = "🖨️",
                selectedPort = config.printerPort,
                availablePorts = availablePorts,
                onPortSelected = { onConfigChanged(config.copy(printerPort = it)) },
                enabled = !config.useSimulatedHardware
            )
            
            // Scale Settings
            PeripheralCard(
                title = "Weight Scale",
                icon = "⚖️",
                selectedPort = config.scalePort,
                availablePorts = availablePorts,
                onPortSelected = { onConfigChanged(config.copy(scalePort = it)) },
                enabled = !config.useSimulatedHardware
            )
            
            // Cash Drawer Settings
            PeripheralCard(
                title = "Cash Drawer",
                icon = "💵",
                selectedPort = config.cashDrawerPort,
                availablePorts = availablePorts,
                onPortSelected = { onConfigChanged(config.copy(cashDrawerPort = it)) },
                enabled = !config.useSimulatedHardware
            )
            
            // Payment Terminal Settings
            PaymentTerminalCard(
                ip = config.paymentTerminalIp,
                port = config.paymentTerminalPort,
                onIpChanged = { onConfigChanged(config.copy(paymentTerminalIp = it)) },
                onPortChanged = { onConfigChanged(config.copy(paymentTerminalPort = it)) },
                enabled = !config.useSimulatedHardware
            )
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.L))
            
            // Save Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlineButton(onClick = onBack) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(GroPOSSpacing.M))
                PrimaryButton(onClick = onSave) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(end = GroPOSSpacing.XS)
                    )
                    Text("Save Settings", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SimulatedModeCard(
    useSimulated: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GroPOSRadius.Medium),
        colors = CardDefaults.cardColors(
            containerColor = if (useSimulated) GroPOSColors.WarningOrange.copy(alpha = 0.1f) else GroPOSColors.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GroPOSSpacing.M),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🧪 Simulated Hardware Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (useSimulated) {
                        "Using simulated peripherals for development"
                    } else {
                        "Using real hardware connections"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = GroPOSColors.TextSecondary
                )
            }
            
            Switch(
                checked = useSimulated,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = GroPOSColors.WarningOrange,
                    checkedThumbColor = GroPOSColors.White
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeripheralCard(
    title: String,
    icon: String,
    selectedPort: String?,
    availablePorts: List<String>,
    onPortSelected: (String?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GroPOSRadius.Medium),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) GroPOSColors.White else GroPOSColors.LightGray2
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GroPOSSpacing.M)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
            ) {
                Text(text = icon, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) GroPOSColors.TextPrimary else GroPOSColors.TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
            
            ExposedDropdownMenuBox(
                expanded = expanded && enabled,
                onExpandedChange = { if (enabled) expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedPort ?: "Not configured",
                    onValueChange = {},
                    readOnly = true,
                    enabled = enabled,
                    label = { Text("COM Port") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded && enabled,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            onPortSelected(null)
                            expanded = false
                        }
                    )
                    availablePorts.forEach { port ->
                        DropdownMenuItem(
                            text = { Text(port) },
                            onClick = {
                                onPortSelected(port)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentTerminalCard(
    ip: String?,
    port: Int?,
    onIpChanged: (String?) -> Unit,
    onPortChanged: (Int?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GroPOSRadius.Medium),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) GroPOSColors.White else GroPOSColors.LightGray2
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GroPOSSpacing.M)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
            ) {
                Text(text = "💳", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Payment Terminal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) GroPOSColors.TextPrimary else GroPOSColors.TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
            ) {
                OutlinedTextField(
                    value = ip ?: "",
                    onValueChange = { onIpChanged(it.ifEmpty { null }) },
                    label = { Text("IP Address") },
                    placeholder = { Text("192.168.1.100") },
                    enabled = enabled,
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = port?.toString() ?: "",
                    onValueChange = { onPortChanged(it.toIntOrNull()) },
                    label = { Text("Port") },
                    placeholder = { Text("10009") },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}

