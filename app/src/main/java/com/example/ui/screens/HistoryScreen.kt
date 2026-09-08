package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.printer.BluetoothThermalPrinter
import com.example.printer.ReceiptData
import com.example.qris.QrisEngine
import com.example.ui.QrisViewModel
import com.example.ui.theme.QrisGreen
import com.example.ui.theme.QrisRed
import com.example.ui.theme.QrisTeal
import com.example.util.ImageShareUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: QrisViewModel,
    onNavigateToCalculator: () -> Unit
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val merchantInfo by viewModel.merchantInfo.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, TODAY, SUCCESS, PENDING
    var selectedTxForDetail by remember { mutableStateOf<TransactionEntity?>(null) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    val calendar = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val startOfToday = calendar.timeInMillis

    val filteredList = remember(allTransactions, searchQuery, selectedFilter) {
        allTransactions.filter { tx ->
            val matchesSearch = tx.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    tx.note.contains(searchQuery, ignoreCase = true) ||
                    tx.totalAmount.toString().contains(searchQuery)

            val matchesFilter = when (selectedFilter) {
                "TODAY" -> tx.timestamp >= startOfToday
                "SUCCESS" -> tx.status == "SUCCESS"
                "PENDING" -> tx.status == "PENDING"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val totalFilteredNominal = filteredList.filter { it.status == "SUCCESS" }.sumOf { it.totalAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("history_screen")
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar & Clear Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("history_search_input"),
                placeholder = { Text("Cari nomor invoice atau catatan...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.outline)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            if (allTransactions.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showDeleteAllConfirm = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "Bersihkan Semua",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("Semua (${allTransactions.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "TODAY",
                    onClick = { selectedFilter = "TODAY" },
                    label = { Text("Hari Ini") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "SUCCESS",
                    onClick = { selectedFilter = "SUCCESS" },
                    label = { Text("Lunas") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1B3828),
                        selectedLabelColor = Color(0xFF4ADE80)
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "PENDING",
                    onClick = { selectedFilter = "PENDING" },
                    label = { Text("Pending") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        selectedLabelColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Summary Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total (${filteredList.size} Data)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = QrisEngine.formatRupiah(totalFilteredNominal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Transaction List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tidak Ada Data Transaksi",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Tidak ditemukan transaksi sesuai pencarian" else "Belum ada riwayat transaksi yang tersimpan",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredList, key = { it.id }) { tx ->
                    TransactionListItem(
                        tx = tx,
                        onClick = { selectedTxForDetail = tx }
                    )
                }
            }
        }
    }

    // Detail Dialog when transaction is tapped
    selectedTxForDetail?.let { tx ->
        val context = LocalContext.current
        TransactionDetailDialog(
            tx = tx,
            merchantName = merchantInfo.merchantName,
            merchantCity = merchantInfo.merchantCity,
            nmid = merchantInfo.nmid,
            onDismiss = { selectedTxForDetail = null },
            onDelete = {
                viewModel.deleteTransaction(tx)
                selectedTxForDetail = null
            },
            onSaveGallery = {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val bitmap = ImageShareUtils.createQrisCardBitmap(
                            context = context,
                            merchantName = merchantInfo.merchantName,
                            merchantCity = merchantInfo.merchantCity,
                            nmid = merchantInfo.nmid,
                            invoice = tx.invoiceNumber,
                            amount = tx.totalAmount,
                            qrisPayload = tx.qrisPayload,
                            customerName = tx.customerName
                        )
                        val result = ImageShareUtils.saveBitmapToGallery(context, bitmap, tx.invoiceNumber)
                        withContext(Dispatchers.Main) {
                            result.onSuccess {
                                Toast.makeText(context, "Struk ${tx.invoiceNumber} berhasil disimpan ke Galeri! 📸", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(context, "Gagal simpan galeri: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onShare = {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val bitmap = ImageShareUtils.createQrisCardBitmap(
                            context = context,
                            merchantName = merchantInfo.merchantName,
                            merchantCity = merchantInfo.merchantCity,
                            nmid = merchantInfo.nmid,
                            invoice = tx.invoiceNumber,
                            amount = tx.totalAmount,
                            qrisPayload = tx.qrisPayload,
                            customerName = tx.customerName
                        )
                        val custText = if (tx.customerName.isNotBlank()) " a/n ${tx.customerName}" else ""
                        val result = ImageShareUtils.shareBitmap(
                            context = context,
                            bitmap = bitmap,
                            subject = "Struk Transaksi ${tx.invoiceNumber}",
                            text = "Bukti pembayaran QRIS Dinamis ${QrisEngine.formatRupiah(tx.totalAmount)}$custText di ${merchantInfo.merchantName} (${tx.invoiceNumber})."
                        )
                        withContext(Dispatchers.Main) {
                            result.onFailure { err ->
                                Toast.makeText(context, "Gagal membagikan: ${err.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Gagal membagikan: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onPrint = {
                viewModel.printHistoryReceipt(tx)
            }
        )
    }

    // Confirmation dialog to clear all transactions
    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text("Hapus Semua Riwayat?") },
            text = { Text("Semua data transaksi tersimpan akan dihapus permanen. Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllTransactions()
                        showDeleteAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus Semua")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun TransactionListItem(
    tx: TransactionEntity,
    onClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(tx.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
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
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (tx.status == "SUCCESS") QrisGreen.copy(alpha = 0.12f)
                            else Color(0xFFFFF3CD)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (tx.status == "SUCCESS") Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                        contentDescription = tx.status,
                        tint = if (tx.status == "SUCCESS") QrisGreen else Color(0xFFB45309),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tx.invoiceNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (tx.customerName.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = tx.customerName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (tx.note.isNotBlank()) {
                        Text(
                            text = tx.note,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = QrisEngine.formatRupiah(tx.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (tx.status == "SUCCESS") Color(0xFF4ADE80) else MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (tx.status == "SUCCESS") Color(0xFF1B3828) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (tx.status == "SUCCESS") "LUNAS" else "PENDING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tx.status == "SUCCESS") Color(0xFF4ADE80) else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailDialog(
    tx: TransactionEntity,
    merchantName: String,
    merchantCity: String,
    nmid: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSaveGallery: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit
) {
    val dateStr = SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm:ss", Locale("id", "ID")).format(Date(tx.timestamp))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Rincian Transaksi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Tagihan Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOTAL PEMBAYARAN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = QrisEngine.formatRupiah(tx.totalAmount),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Details Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DetailRow("No. Invoice", tx.invoiceNumber)
                        if (tx.customerName.isNotBlank()) {
                            DetailRow("Pelanggan", tx.customerName)
                        }
                        DetailRow("Waktu", dateStr)
                        DetailRow("Toko", merchantName)
                        DetailRow("Status", if (tx.status == "SUCCESS") "LUNAS 🟢" else "PENDING 🟡")
                        DetailRow("Metode", tx.paymentSource.ifBlank { "QRIS Dinamis" })
                        if (tx.note.isNotBlank()) {
                            DetailRow("Catatan", tx.note)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Primary Action: Cetak Struk
                Button(
                    onClick = onPrint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cetak Struk Thermal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Secondary Row: Simpan Galeri & Bagikan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveGallery,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Galeri", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Galeri", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Bagikan", tint = QrisGreen, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bagikan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
