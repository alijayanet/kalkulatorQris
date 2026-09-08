package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qris.QrisEngine
import com.example.ui.QrisViewModel
import com.example.ui.theme.CalcKeyActionDark
import com.example.ui.theme.CalcKeyActionLight
import com.example.ui.theme.CalcKeyNumberDark
import com.example.ui.theme.CalcKeyNumberLight
import com.example.ui.theme.CalcKeyOperatorDark
import com.example.ui.theme.CalcKeyOperatorLight
import com.example.ui.theme.CalcKeySpecialDark
import com.example.ui.theme.CalcKeySpecialLight
import com.example.ui.theme.QrisGreen
import com.example.ui.theme.QrisRed
import com.example.ui.theme.QrisTeal

@Composable
fun CalculatorScreen(
    viewModel: QrisViewModel,
    onNavigateToUpload: () -> Unit = {}
) {
    val expression by viewModel.expression.collectAsState()
    val resultDisplay by viewModel.resultDisplay.collectAsState()
    val displayTotal by viewModel.displayTotal.collectAsState()
    val lineItems by viewModel.lineItems.collectAsState()
    val isQrisMode by viewModel.isQrisMode.collectAsState()
    val dynamicQrisPayload by viewModel.activeDynamicQris.collectAsState()
    val activeInvoice by viewModel.activeInvoice.collectAsState()
    val activeStatus by viewModel.activeStatus.collectAsState()
    val merchantInfo by viewModel.merchantInfo.collectAsState()
    val customerName by viewModel.customerName.collectAsState()

    val quickNominals = remember {
        listOf(
            "5rb" to 5_000L,
            "10rb" to 10_000L,
            "20rb" to 20_000L,
            "50rb" to 50_000L,
            "100rb" to 100_000L,
            "200rb" to 200_000L,
            "500rb" to 500_000L
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp)
            .testTag("calculator_screen")
    ) {
        // --- Slim Top Merchant & Status Strip ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToUpload() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = merchantInfo.merchantName.ifBlank { "WARUNG QRIS" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isQrisMode) Color(0xFF1B3828) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isQrisMode) Color(0xFF4ADE80) else MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isQrisMode) "QRIS AKTIF" else "MODE KASIR",
                        color = if (isQrisMode) Color(0xFF4ADE80) else MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- Slim Customer Name Input Bar ---
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(0.5.dp, if (customerName.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = if (customerName.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                BasicTextField(
                    value = customerName,
                    onValueChange = { viewModel.onCustomerNameChange(it) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (customerName.isEmpty()) {
                            Text(
                                text = "Nama Pelanggan / Catatan Struk (opsional)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                        innerTextField()
                    }
                )
                if (customerName.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hapus Nama",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .size(13.dp)
                            .clickable { viewModel.onCustomerNameChange("") }
                    )
                }
            }
        }

        // --- Screen Display Container (Formula + Standard Number Result) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calculator_display_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Expression Formula
                Text(
                    text = expression.ifBlank { if (lineItems.isNotEmpty()) "${lineItems.size} Item Tersimpan" else "0" },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Calculated Main Nominal Result
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (displayTotal > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = if (lineItems.isNotEmpty()) "TOTAL: ${QrisEngine.formatRupiah(displayTotal)}" else "QRIS: ${QrisEngine.formatRupiah(displayTotal)}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "HASIL",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Clean Standard Number (e.g. 1.5, 3.75, 25.000)
                    Text(
                        text = resultDisplay,
                        fontSize = if (resultDisplay.length > 10) 22.sp else 28.sp,
                        fontWeight = FontWeight.Black,
                        color = if (displayTotal > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        // --- Line Items Cart Tray (If Multiple Items Added) ---
        AnimatedVisibility(visible = lineItems.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${lineItems.size} Item:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(lineItems, key = { it.id }) { item ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${item.formula}=${QrisEngine.formatRupiah(item.amount)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { viewModel.removeLineItem(item) }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reset",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clickable { viewModel.clearAllLineItems() }
                            .padding(2.dp)
                    )
                }
            }
        }

        // --- Quick Preset Nominal Chips + Add Item Button ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.addCurrentToLineItems() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Tambah Item",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "+ Item",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 1.dp)
            ) {
                items(quickNominals) { (label, amount) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.onQuickNominalClick(amount) }
                    ) {
                        Text(
                            text = "+$label",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- Bottom Area: Animated Morphing between Numeric Keypad and Dynamic QRIS ---
        AnimatedContent(
            targetState = isQrisMode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.94f))
                    .togetherWith(fadeOut(animationSpec = tween(180)))
            },
            label = "keypad_qris_morph",
            modifier = Modifier.weight(1f)
        ) { qrisActive ->
            if (qrisActive) {
                // ==========================================
                // DYNAMIC QRIS VIEW (REPLACES NUMERIC KEYBOARD)
                // ==========================================
                DynamicQrisCardView(
                    viewModel = viewModel,
                    payload = dynamicQrisPayload,
                    invoice = activeInvoice,
                    total = displayTotal,
                    merchantName = merchantInfo.merchantName,
                    merchantCity = merchantInfo.merchantCity,
                    nmid = merchantInfo.nmid,
                    status = activeStatus
                )
            } else {
                // ==========================================
                // NUMERIC KEYPAD WITH ARITHMETIC & GENERATE BUTTON
                // ==========================================
                CalculatorKeypadView(
                    viewModel = viewModel,
                    displayTotal = displayTotal
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

/**
 * Standard Calculator Keypad Grid with numbers, decimals, equal, and Generate QRIS action.
 */
@Composable
private fun CalculatorKeypadView(
    viewModel: QrisViewModel,
    displayTotal: Long
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calculator_keypad"),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Row 1: C, DEL, %, ÷
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CalcButton(
                text = "C",
                type = CalcButtonType.SPECIAL,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.onClearClick() }
            )
            CalcIconButton(
                icon = Icons.Default.Backspace,
                type = CalcButtonType.SPECIAL,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.onDeleteClick() }
            )
            CalcButton(
                text = "%",
                type = CalcButtonType.OPERATOR,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.onOperatorClick("%") }
            )
            CalcButton(
                text = "÷",
                type = CalcButtonType.OPERATOR,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.onOperatorClick("÷") }
            )
        }

        // Row 2: 7, 8, 9, ×
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CalcButton("7", CalcButtonType.NUMBER, Modifier.weight(1f)) { viewModel.onDigitClick("7") }
            CalcButton("8", CalcButtonType.NUMBER, Modifier.weight(1f)) { viewModel.onDigitClick("8") }
            CalcButton("9", CalcButtonType.NUMBER, Modifier.weight(1f)) { viewModel.onDigitClick("9") }
            CalcButton("×", CalcButtonType.OPERATOR, Modifier.weight(1f)) { viewModel.onOperatorClick("×") }
        }

        // Row 3: 4, 5, 6, -
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CalcButton("4", CalcButtonType.NUMBER, Modifier.weight(1f)) { viewModel.onDigitClick("4") }
            CalcButton("5", CalcButtonType.NUMBER, Modifier.weight(1f)) { viewModel.onDigitClick("5") }
            CalcButton("6", CalcButtonType.NUMBER, Modifier.weight(1f)) { viewModel.onDigitClick("6") }
            CalcButton("-", CalcButtonType.OPERATOR, Modifier.weight(1f)) { viewModel.onOperatorClick("-") }
        }

        // Row 4: 1, 2, 3, +
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CalcButton("1", CalcButtonType.NUMBER, Modifier.weight(1f)) { viewModel.onDigitClick("1") }
            CalcButton("2", CalcButtonType.NUMBER, Modifier.weight(1f)) { viewModel.onDigitClick("2") }
            CalcButton("3", CalcButtonType.NUMBER, Modifier.weight(1f)) { viewModel.onDigitClick("3") }
            CalcButton("+", CalcButtonType.OPERATOR, Modifier.weight(1f)) { viewModel.onOperatorClick("+") }
        }

        // Row 5: 00, 0, ., =
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CalcButton("00", CalcButtonType.NUMBER, Modifier.weight(1f)) { viewModel.onDigitClick("00") }
            CalcButton("0", CalcButtonType.NUMBER, Modifier.weight(1f)) { viewModel.onDigitClick("0") }
            CalcButton(".", CalcButtonType.NUMBER, Modifier.weight(1f)) { viewModel.onDecimalClick() }
            CalcButton("=", CalcButtonType.ACTION, Modifier.weight(1f)) { viewModel.onEqualClick() }
        }

        // Prominent Master Button: GENERATE QRIS
        Button(
            onClick = { viewModel.generateDynamicQris() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("generate_qris_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "Generate QRIS",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (displayTotal > 0) "GENERATE QRIS (${QrisEngine.formatRupiah(displayTotal)})" else "GENERATE KODE QRIS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * Dynamic QRIS Card View (which replaces the numeric keyboard when generated).
 */
@Composable
private fun DynamicQrisCardView(
    viewModel: QrisViewModel,
    payload: String,
    invoice: String,
    total: Long,
    merchantName: String,
    merchantCity: String,
    nmid: String,
    status: String
) {
    val context = LocalContext.current
    val pairedPrinters by viewModel.pairedPrinters.collectAsState()
    val selectedPrinter by viewModel.selectedPrinter.collectAsState()
    val customerName by viewModel.customerName.collectAsState()
    var showPrinterPickerModal by remember { mutableStateOf(false) }

    val printPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
        } else {
            true
        }
        if (isGranted) {
            if (selectedPrinter == null || selectedPrinter?.name?.contains("Demo", true) == true || selectedPrinter?.address == "00:11:22:33:44:55") {
                viewModel.refreshBluetoothDevices()
                showPrinterPickerModal = true
            } else {
                viewModel.printCurrentReceipt()
            }
        } else {
            Toast.makeText(context, "Izin Bluetooth (Perangkat di Sekitar) diperlukan untuk mencetak struk", Toast.LENGTH_LONG).show()
        }
    }

    val qrBitmap = remember(payload) {
        if (payload.isNotBlank()) {
            QrisEngine.generateQrBitmap(payload, 500)
        } else {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dynamic_qris_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar: Merchant Name, NMID, and Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = merchantName.ifBlank { "KASIR QRIS" }.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "NMID: $nmid • $merchantCity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Payment Status Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (status == "SUCCESS") Color(0xFF1B3828) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (status == "SUCCESS") Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                            contentDescription = status,
                            tint = if (status == "SUCCESS") Color(0xFF4ADE80) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (status == "SUCCESS") "LUNAS" else "Menunggu Pembayaran",
                            color = if (status == "SUCCESS") Color(0xFF4ADE80) else MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Total Tagihan Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL TAGIHAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = QrisEngine.formatRupiah(total),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // High Resolution QR Code Image
            Box(
                modifier = Modifier
                    .size(205.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QRIS Dinamis Code",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "QR Placeholder",
                        tint = Color.Gray,
                        modifier = Modifier.size(90.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Invoice & Customer Info
            Text(
                text = invoice,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (customerName.isNotBlank()) {
                Text(
                    text = "Pelanggan: $customerName",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (status != "SUCCESS") {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    onClick = { viewModel.simulateCustomerPayment() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .testTag("simulate_payment_button"),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1B3828)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QRIS",
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Silakan Scan QRIS untuk Membayar",
                            color = Color(0xFF4ADE80),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Rincian Item Breakdown (if items > 1)
            val receiptItems = remember { viewModel.getReceiptItems() }
            if (receiptItems.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rincian Pembelian (${receiptItems.size} Item)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Subtotal",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(6.dp))
                        receiptItems.forEach { (itemLabel, itemAmt) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = itemLabel,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = QrisEngine.formatRupiah(itemAmt),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Tagihan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = QrisEngine.formatRupiah(total),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = QrisGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Action: Cetak Struk (Full Width prominent button)
            Button(
                onClick = {
                    val triggerPrint: () -> Unit = {
                        if (selectedPrinter == null || selectedPrinter?.name?.contains("Demo", true) == true || selectedPrinter?.address == "00:11:22:33:44:55") {
                            viewModel.refreshBluetoothDevices()
                            showPrinterPickerModal = true
                        } else {
                            viewModel.printCurrentReceipt()
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            triggerPrint()
                        } else {
                            printPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH_SCAN
                                )
                            )
                        }
                    } else {
                        triggerPrint()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Print, contentDescription = "Cetak", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cetak Struk Thermal (Bluetooth)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Actions Row: Simpan Galeri & Bagikan QRIS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Galeri Badge
                Surface(
                    onClick = { viewModel.saveQrisToGallery() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Galeri",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simpan Galeri",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Bagikan Badge
                Surface(
                    onClick = { viewModel.shareQrisImage() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Bagikan",
                            tint = QrisGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bagikan QRIS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Switch Back to Keypad button
            OutlinedButton(
                onClick = { viewModel.onClearClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("back_to_calculator_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Transaksi Baru (Kembali ke Keypad)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    // Modal Dialog to Select Physical Bluetooth Printer
    if (showPrinterPickerModal) {
        val realPrinters = pairedPrinters.filter { !it.name.contains("Demo", true) && it.address != "00:11:22:33:44:55" }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPrinterPickerModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih Printer Thermal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Pilih printer Bluetooth yang terhubung ke HP Anda:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (realPrinters.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Belum ada printer Bluetooth fisik yang terpasang (paired) di HP Anda.\n\nSilakan hidupkan printer, lalu klik tombol di bawah untuk memasangkannya di Pengaturan Bluetooth HP.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        realPrinters.forEach { device ->
                            val isSelected = selectedPrinter?.address == device.address
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.selectPrinter(device)
                                        showPrinterPickerModal = false
                                        viewModel.printCurrentReceipt()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(device.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(device.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Terpilih",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka Bluetooth HP (Pairing Printer Baru)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPrinterPickerModal = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

enum class CalcButtonType {
    NUMBER, OPERATOR, SPECIAL, ACTION
}

@Composable
private fun CalcButton(
    text: String,
    type: CalcButtonType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.3f
    val bgColor = when (type) {
        CalcButtonType.NUMBER -> if (isDark) CalcKeyNumberDark else CalcKeyNumberLight
        CalcButtonType.OPERATOR -> if (isDark) CalcKeyOperatorDark else CalcKeyOperatorLight
        CalcButtonType.SPECIAL -> if (isDark) CalcKeySpecialDark else CalcKeySpecialLight
        CalcButtonType.ACTION -> if (isDark) CalcKeyActionDark else CalcKeyActionLight
    }

    val textColor = when (type) {
        CalcButtonType.NUMBER -> if (isDark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
        CalcButtonType.OPERATOR -> if (isDark) Color(0xFFD0BCFF) else Color(0xFF381E72)
        CalcButtonType.SPECIAL -> if (isDark) Color(0xFFF2B8B5) else Color(0xFFB3261E)
        CalcButtonType.ACTION -> if (isDark) Color(0xFF381E72) else Color(0xFFFFFFFF)
    }

    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (!isDark && type == CalcButtonType.NUMBER) 1.dp else if (isDark) 1.dp else 0.dp,
                color = if (!isDark && type == CalcButtonType.NUMBER) Color(0xFFE0E0E0)
                        else if (isDark && type == CalcButtonType.NUMBER) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        shadowElevation = if (!isDark && type == CalcButtonType.NUMBER) 1.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = if (text.length > 2) 14.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun CalcIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    type: CalcButtonType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.3f
    val bgColor = when (type) {
        CalcButtonType.NUMBER -> if (isDark) CalcKeyNumberDark else CalcKeyNumberLight
        CalcButtonType.SPECIAL -> if (isDark) CalcKeySpecialDark else CalcKeySpecialLight
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val iconTint = when (type) {
        CalcButtonType.SPECIAL -> if (isDark) Color(0xFFF2B8B5) else Color(0xFFB3261E)
        CalcButtonType.OPERATOR -> if (isDark) Color(0xFFD0BCFF) else Color(0xFF381E72)
        else -> if (isDark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
    }

    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (!isDark && type == CalcButtonType.NUMBER) 1.dp else if (isDark) 1.dp else 0.dp,
                color = if (!isDark && type == CalcButtonType.NUMBER) Color(0xFFE0E0E0)
                        else if (isDark && type == CalcButtonType.NUMBER) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        shadowElevation = if (!isDark && type == CalcButtonType.NUMBER) 1.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Action",
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
