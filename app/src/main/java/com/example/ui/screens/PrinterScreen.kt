package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.printer.BluetoothPrinterDevice
import com.example.printer.ReceiptData
import com.example.qris.QrisEngine
import com.example.ui.QrisViewModel
import com.example.ui.theme.QrisGreen
import com.example.ui.theme.QrisRed
import com.example.ui.theme.QrisTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrinterScreen(
    viewModel: QrisViewModel
) {
    val context = LocalContext.current
    val pairedPrinters by viewModel.pairedPrinters.collectAsState()
    val selectedPrinter by viewModel.selectedPrinter.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()
    val merchantInfo by viewModel.merchantInfo.collectAsState()
    val displayTotal by viewModel.displayTotal.collectAsState()
    val activeInvoice by viewModel.activeInvoice.collectAsState()
    val customerName by viewModel.customerName.collectAsState()
    val receiptAddress by viewModel.receiptStoreAddress.collectAsState()
    val receiptPhone by viewModel.receiptStorePhone.collectAsState()
    val receiptFooter by viewModel.receiptFooterMessage.collectAsState()

    // Bluetooth CONNECT permission launcher (required for Android 12+)
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.refreshBluetoothDevices()
        }
    }

    // Helper to safely refresh Bluetooth devices with permission check
    fun safeRefreshBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                viewModel.refreshBluetoothDevices()
            } else {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            viewModel.refreshBluetoothDevices()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                viewModel.refreshBluetoothDevices()
            } else {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            viewModel.refreshBluetoothDevices()
        }
    }

    val sampleInvoice = activeInvoice.ifBlank { "INV-20260830-1015" }
    val sampleTotal = if (displayTotal > 0) displayTotal else 75000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("printer_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Clean Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedPrinter != null) QrisGreen.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selectedPrinter != null) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                            contentDescription = "Printer",
                            tint = if (selectedPrinter != null) QrisGreen else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = selectedPrinter?.name ?: "Printer Thermal Bluetooth",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (selectedPrinter != null) "● Siap Cetak (ESC/POS)" else "Mendukung printer thermal 58/80mm",
                            fontSize = 12.sp,
                            color = if (selectedPrinter != null) QrisGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selectedPrinter != null) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = { safeRefreshBluetooth() },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Pindai", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pindai", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List of Available Printers
        Text(
            text = "Daftar Perangkat Bluetooth",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (pairedPrinters.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tidak ada printer Bluetooth yang terhubung. Pastikan Bluetooth aktif dan printer sudah dipasangkan (paired).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pairedPrinters.forEach { device ->
                    val isSelected = selectedPrinter?.address == device.address
                    PrinterDeviceCard(
                        device = device,
                        isSelected = isSelected,
                        onClick = { viewModel.selectPrinter(device) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Open Phone Bluetooth Settings Button
        OutlinedButton(
            onClick = {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Buka Pengaturan Bluetooth HP (Pairing Printer Baru)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Receipt Live Monospace Preview
        Text(
            text = "Pratinjau Struk Pembayaran",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Realistic Thermal Paper Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = merchantInfo.merchantName.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = receiptAddress.ifBlank { "${merchantInfo.merchantCity} • NMID: ${merchantInfo.nmid}" },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
                if (receiptPhone.isNotBlank()) {
                    Text(
                        text = "Telp/WA: $receiptPhone",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = "--------------------------------",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                ReceiptMonoRow("No. Transaksi", sampleInvoice)
                ReceiptMonoRow("Waktu", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date()))
                if (customerName.isNotBlank()) {
                    ReceiptMonoRow("Pelanggan", customerName)
                }
                ReceiptMonoRow("Metode", "QRIS Dinamis")
                ReceiptMonoRow("Status", "LUNAS [SELESAI]")

                Text(
                    text = "--------------------------------",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                val activeItems = viewModel.getReceiptItems()
                if (activeItems.isNotEmpty()) {
                    activeItems.forEach { (itemLabel, itemAmt) ->
                        ReceiptMonoRow(itemLabel, QrisEngine.formatRupiah(itemAmt))
                    }
                } else {
                    ReceiptMonoRow("2.5 x 5.000", "Rp 12.500")
                    ReceiptMonoRow("2 x 7.500", "Rp 15.000")
                }
                ReceiptMonoRow("Biaya Admin", "Rp 0")

                Text(
                    text = "================================",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.Black
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL BAYAR", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Black)
                    Text(QrisEngine.formatRupiah(sampleTotal), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Black)
                }

                Text(
                    text = "--------------------------------",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))
                val displayFooter = receiptFooter.ifBlank { "Terima Kasih Atas Kunjungan Anda!\nBarang yang dibeli tidak dapat ditukar." }
                Text(
                    text = displayFooter,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons Row (Tes Koneksi & Cetak Struk)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.testPrinterConnection() },
                enabled = !isPrinting,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Tes", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tes Koneksi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { viewModel.printCurrentReceipt() },
                enabled = !isPrinting,
                modifier = Modifier
                    .weight(1.5f)
                    .height(50.dp)
                    .testTag("print_receipt_action_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isPrinting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Memproses...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cetak Struk", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PrinterDeviceCard(
    device: BluetoothPrinterDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                        contentDescription = "Printer",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = device.address,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Terhubung",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Aktif",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptMonoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
        Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
    }
}
