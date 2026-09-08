package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TransactionEntity
import com.example.data.TransactionRepository
import com.example.notification.NotificationHelper
import com.example.printer.BluetoothPrinterDevice
import com.example.printer.BluetoothThermalPrinter
import com.example.printer.ReceiptData
import com.example.qris.MerchantInfo
import com.example.qris.QrisEngine
import com.example.security.EncryptedPreferencesManager
import com.example.util.ImageShareUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LineItem(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val formula: String,
    val amount: Long
)

class QrisViewModel(application: Application) : AndroidViewModel(application) {

    private val securePrefs = EncryptedPreferencesManager(application)
    private val db = AppDatabase.getDatabase(application)
    private val repository = TransactionRepository(db.transactionDao())

    // --- Calculator & Dynamic QRIS State ---
    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _rawResult = MutableStateFlow(0.0)
    val rawResult: StateFlow<Double> = _rawResult.asStateFlow()

    private val _resultDisplay = MutableStateFlow("0")
    val resultDisplay: StateFlow<String> = _resultDisplay.asStateFlow()

    private val _displayTotal = MutableStateFlow(0L)
    val displayTotal: StateFlow<Long> = _displayTotal.asStateFlow()

    private val _lineItems = MutableStateFlow<List<LineItem>>(emptyList())
    val lineItems: StateFlow<List<LineItem>> = _lineItems.asStateFlow()

    private val _isQrisMode = MutableStateFlow(false)
    val isQrisMode: StateFlow<Boolean> = _isQrisMode.asStateFlow()

    private val _activeDynamicQris = MutableStateFlow("")
    val activeDynamicQris: StateFlow<String> = _activeDynamicQris.asStateFlow()

    private val _activeInvoice = MutableStateFlow("")
    val activeInvoice: StateFlow<String> = _activeInvoice.asStateFlow()

    private val _activeStatus = MutableStateFlow("PENDING") // PENDING, SUCCESS
    val activeStatus: StateFlow<String> = _activeStatus.asStateFlow()

    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    fun onCustomerNameChange(name: String) {
        _customerName.value = name
    }

    private val _currentTransactionId = MutableStateFlow(0L)

    // --- Merchant & Base QRIS State ---
    private val _merchantInfo = MutableStateFlow(
        MerchantInfo(
            merchantName = "WARUNG BERKAH QRIS",
            merchantCity = "JAKARTA",
            postalCode = "10110",
            nmid = "ID1020304050607",
            mpan = "936001234567890",
            rawPayload = QrisEngine.DEFAULT_STATIC_QRIS
        )
    )
    val merchantInfo: StateFlow<MerchantInfo> = _merchantInfo.asStateFlow()

    // --- App Theme & UI State ---
    private val _themeMode = MutableStateFlow("SYSTEM") // "SYSTEM", "DARK", "LIGHT"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _showGuide = MutableStateFlow(false)
    val showGuide: StateFlow<Boolean> = _showGuide.asStateFlow()

    // --- Bluetooth Printer State ---
    private val _pairedPrinters = MutableStateFlow<List<BluetoothPrinterDevice>>(emptyList())
    val pairedPrinters: StateFlow<List<BluetoothPrinterDevice>> = _pairedPrinters.asStateFlow()

    private val _selectedPrinter = MutableStateFlow<BluetoothPrinterDevice?>(null)
    val selectedPrinter: StateFlow<BluetoothPrinterDevice?> = _selectedPrinter.asStateFlow()

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    // --- Receipt Header & Footer Customization State ---
    private val _receiptStoreAddress = MutableStateFlow("")
    val receiptStoreAddress: StateFlow<String> = _receiptStoreAddress.asStateFlow()

    private val _receiptStorePhone = MutableStateFlow("")
    val receiptStorePhone: StateFlow<String> = _receiptStorePhone.asStateFlow()

    private val _receiptFooterMessage = MutableStateFlow("Terima Kasih Atas Kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar.")
    val receiptFooterMessage: StateFlow<String> = _receiptFooterMessage.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    // --- Room Database Stream ---
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTransactions: StateFlow<List<TransactionEntity>> = repository.getTodayTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        NotificationHelper.initNotificationChannel(application)
        loadSavedMerchantProfile()
        loadThemePreference()
        loadFirstLaunchGuide()
        loadSavedPrinter()
        loadSavedReceiptSettings()
    }

    // ----------------------------------------------------
    // Calculator Operations (Standard Decimal + Multi-Item Math)
    // ----------------------------------------------------
    fun onDigitClick(digit: String) {
        if (_isQrisMode.value) {
            // Return to keypad on digit press
            _isQrisMode.value = false
            _expression.value = digit
            evaluateExpression()
            return
        }

        val current = _expression.value
        // Prevent leading zero issues unless entering decimals
        if (current == "0" && digit != "0" && digit != "00") {
            _expression.value = digit
        } else {
            _expression.value = current + digit
        }
        evaluateExpression()
    }

    fun onDecimalClick() {
        if (_isQrisMode.value) {
            _isQrisMode.value = false
            _expression.value = "0."
            evaluateExpression()
            return
        }

        val current = _expression.value
        if (current.isEmpty()) {
            _expression.value = "0."
        } else {
            val lastToken = current.split(' ').lastOrNull() ?: ""
            if (!lastToken.contains('.')) {
                if (lastToken.isEmpty() || isOperatorChar(lastToken.last())) {
                    _expression.value = "$current 0."
                } else {
                    _expression.value = "$current."
                }
            }
        }
        evaluateExpression()
    }

    fun onOperatorClick(op: String) {
        if (_isQrisMode.value) {
            _isQrisMode.value = false
        }
        val current = _expression.value.trimEnd()
        if (current.isEmpty()) {
            if (op == "-") {
                _expression.value = "-"
            }
            return
        }

        val lastChar = current.lastOrNull()
        if (lastChar != null && isOperatorChar(lastChar)) {
            // Replace last operator
            _expression.value = current.dropLast(1).trimEnd() + " $op "
        } else {
            _expression.value = "$current $op "
        }
        evaluateExpression()
    }

    fun onEqualClick() {
        evaluateExpression()
        val res = _rawResult.value
        if (_expression.value.isNotEmpty()) {
            _expression.value = formatNumberClean(res)
            _resultDisplay.value = formatDisplayNumber(res)
        }
    }

    fun onQuickNominalClick(amountToAdd: Long) {
        if (_isQrisMode.value) {
            _isQrisMode.value = false
            _expression.value = amountToAdd.toString()
            evaluateExpression()
            return
        }

        val currentTotal = _rawResult.value
        val newTotal = currentTotal + amountToAdd
        _expression.value = formatNumberClean(newTotal)
        evaluateExpression()
    }

    fun addCurrentToLineItems() {
        evaluateExpression()
        val currentVal = _rawResult.value
        if (currentVal > 0) {
            val expr = _expression.value.trim().ifBlank { formatDisplayNumber(currentVal) }
            val amt = Math.round(currentVal)
            val newItem = LineItem(
                formula = expr.replace("*", "×").replace("/", "÷"),
                amount = amt
            )
            _lineItems.value = _lineItems.value + newItem
            _expression.value = ""
            _rawResult.value = 0.0
            _resultDisplay.value = "0"
            updateTotalFromLineItems()
            _uiMessage.value = "Ditambahkan: ${newItem.formula} (${QrisEngine.formatRupiah(newItem.amount)})"
        }
    }

    fun removeLineItem(item: LineItem) {
        _lineItems.value = _lineItems.value.filter { it.id != item.id }
        updateTotalFromLineItems()
    }

    fun clearAllLineItems() {
        _lineItems.value = emptyList()
        _expression.value = ""
        _rawResult.value = 0.0
        _resultDisplay.value = "0"
        _displayTotal.value = 0L
    }

    private fun updateTotalFromLineItems() {
        val sum = _lineItems.value.sumOf { it.amount }
        _displayTotal.value = sum
        if (_expression.value.isBlank()) {
            _resultDisplay.value = if (sum > 0) formatDisplayNumber(sum.toDouble()) else "0"
        }
    }

    fun onClearClick() {
        _expression.value = ""
        _rawResult.value = 0.0
        _resultDisplay.value = "0"
        _displayTotal.value = 0L
        _lineItems.value = emptyList()
        _isQrisMode.value = false
        _activeDynamicQris.value = ""
    }

    fun onDeleteClick() {
        if (_isQrisMode.value) {
            _isQrisMode.value = false
            return
        }
        val current = _expression.value.trimEnd()
        if (current.isNotEmpty()) {
            if (current.endsWith(" ")) {
                _expression.value = current.dropLast(2).trimEnd()
            } else {
                _expression.value = current.dropLast(1)
            }
            evaluateExpression()
        }
    }

    private fun isOperatorChar(c: Char): Boolean {
        return c == '+' || c == '-' || c == '×' || c == '*' || c == '÷' || c == '/' || c == '%'
    }

    private fun evaluateExpression() {
        val expr = _expression.value.trim()
        val lineSum = _lineItems.value.sumOf { it.amount }
        if (expr.isEmpty()) {
            _rawResult.value = 0.0
            _resultDisplay.value = if (lineSum > 0) formatDisplayNumber(lineSum.toDouble()) else "0"
            _displayTotal.value = lineSum
            return
        }
        try {
            val total = calculateMathExpression(expr)
            _rawResult.value = total
            _resultDisplay.value = formatDisplayNumber(total)
            _displayTotal.value = maxOf(0L, Math.round(total) + lineSum)
        } catch (e: Exception) {
            // Partial expression or invalid syntax
        }
    }

    fun getReceiptItems(): List<Pair<String, Long>> {
        if (_lineItems.value.isNotEmpty()) {
            return _lineItems.value.map { 
                it.formula.replace("×", "x").replace("*", "x").replace("÷", "/") to it.amount 
            }
        }
        val expr = _expression.value.trim()
        if (expr.contains("+")) {
            val parts = expr.split("+").map { it.trim() }.filter { it.isNotEmpty() }
            val list = mutableListOf<Pair<String, Long>>()
            for (part in parts) {
                val amt = Math.round(calculateMathExpression(part))
                if (amt > 0) {
                    list.add(part.replace("×", "x").replace("*", "x").replace("÷", "/") to amt)
                }
            }
            if (list.isNotEmpty()) return list
        }
        val total = _displayTotal.value
        return if (total > 0) listOf("Pembayaran Kasir" to total) else emptyList()
    }

    /**
     * Mathematical evaluator supporting decimals, +, -, *, /, % in sequential/precedence order.
     */
    private fun calculateMathExpression(expression: String): Double {
        val sanitized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace(" ", "")

        if (sanitized.isEmpty()) return 0.0

        // Tokenize into numbers (including decimals) and operators
        val tokens = mutableListOf<String>()
        var numBuffer = StringBuilder()

        for (ch in sanitized) {
            if (ch.isDigit() || ch == '.') {
                numBuffer.append(ch)
            } else if (ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '%') {
                if (numBuffer.isNotEmpty()) {
                    tokens.add(numBuffer.toString())
                    numBuffer = StringBuilder()
                }
                tokens.add(ch.toString())
            }
        }
        if (numBuffer.isNotEmpty()) {
            tokens.add(numBuffer.toString())
        }

        if (tokens.isEmpty()) return 0.0

        // First pass: *, /, %
        val pass1 = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            if (token == "*" || token == "/" || token == "%") {
                if (pass1.isNotEmpty() && i + 1 < tokens.size) {
                    val prev = pass1.removeAt(pass1.size - 1).toDoubleOrNull() ?: 0.0
                    val next = tokens[i + 1].toDoubleOrNull() ?: 1.0
                    val res = when (token) {
                        "*" -> prev * next
                        "/" -> if (next != 0.0) prev / next else prev
                        "%" -> (prev * next) / 100.0
                        else -> prev
                    }
                    pass1.add(res.toString())
                    i += 2
                    continue
                }
            }
            pass1.add(token)
            i++
        }

        // Second pass: +, -
        var result = pass1.firstOrNull()?.toDoubleOrNull() ?: 0.0
        var j = 1
        while (j < pass1.size) {
            val op = pass1[j]
            val nextVal = pass1.getOrNull(j + 1)?.toDoubleOrNull() ?: 0.0
            if (op == "+") {
                result += nextVal
            } else if (op == "-") {
                result -= nextVal
            }
            j += 2
        }

        return result
    }

    /**
     * Formats number for standard calculation result display (clean, no forced 'Rp').
     */
    private fun formatDisplayNumber(value: Double): String {
        if (value == 0.0) return "0"
        if (value.isNaN() || value.isInfinite()) return "0"
        val isWhole = (value % 1.0 == 0.0) && Math.abs(value) < 1e14
        return if (isWhole) {
            val longVal = value.toLong()
            val symbols = java.text.DecimalFormatSymbols(Locale("id", "ID"))
            val df = java.text.DecimalFormat("#,###", symbols)
            df.format(longVal)
        } else {
            val symbols = java.text.DecimalFormatSymbols(Locale.US)
            val df = java.text.DecimalFormat("#,##0.######", symbols)
            df.format(value)
        }
    }

    /**
     * Formats number cleanly into expression string without scientific notation.
     */
    private fun formatNumberClean(value: Double): String {
        if (value == 0.0) return "0"
        if (value.isNaN() || value.isInfinite()) return "0"
        val isWhole = (value % 1.0 == 0.0) && Math.abs(value) < 1e14
        return if (isWhole) {
            value.toLong().toString()
        } else {
            val symbols = java.text.DecimalFormatSymbols(Locale.US)
            val df = java.text.DecimalFormat("#0.######", symbols)
            df.format(value)
        }
    }

    // ----------------------------------------------------
    // Dynamic QRIS Generation & Payment Flow
    // ----------------------------------------------------
    fun generateDynamicQris() {
        evaluateExpression()
        if (_expression.value.isNotBlank() && _rawResult.value > 0) {
            val currentVal = _rawResult.value
            val expr = _expression.value.trim().ifBlank { formatDisplayNumber(currentVal) }
            val amt = Math.round(currentVal)
            if (_lineItems.value.isNotEmpty()) {
                val newItem = LineItem(formula = expr.replace("*", "×").replace("/", "÷"), amount = amt)
                _lineItems.value = _lineItems.value + newItem
                _expression.value = ""
                _rawResult.value = 0.0
                _resultDisplay.value = "0"
                updateTotalFromLineItems()
            }
        }
        val total = _displayTotal.value
        if (total <= 0) {
            _uiMessage.value = "Masukkan nominal transaksi terlebih dahulu (> Rp 0)"
            return
        }

        val invoice = "INV-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val merchant = _merchantInfo.value
        val dynamicPayload = QrisEngine.generateDynamicQris(
            baseStaticQris = merchant.rawPayload,
            amount = total
        )

        _activeDynamicQris.value = dynamicPayload
        _activeInvoice.value = invoice
        _activeStatus.value = "PENDING"
        _isQrisMode.value = true

        // Push real-time notification
        NotificationHelper.showQrisGeneratedNotification(
            getApplication(),
            invoice,
            total
        )

        val receiptItems = getReceiptItems()
        val itemsSummary = if (receiptItems.size > 1) {
            receiptItems.joinToString(", ") { "${it.first} (${QrisEngine.formatRupiah(it.second)})" }
        } else {
            "Pembayaran Kasir (${_expression.value.ifBlank { QrisEngine.formatRupiah(total) }})"
        }

        // Insert pending transaction into Room
        viewModelScope.launch {
            val entity = TransactionEntity(
                invoiceNumber = invoice,
                customerName = _customerName.value.trim(),
                nominal = total,
                fee = 0,
                totalAmount = total,
                note = itemsSummary,
                merchantName = merchant.merchantName,
                qrisPayload = dynamicPayload,
                status = "PENDING",
                timestamp = System.currentTimeMillis()
            )
            val id = repository.insert(entity)
            _currentTransactionId.value = id
        }
    }

    fun simulateCustomerPayment() {
        val total = _displayTotal.value
        val invoice = _activeInvoice.value
        val merchant = _merchantInfo.value

        _activeStatus.value = "SUCCESS"

        // Trigger push notification real-time
        NotificationHelper.showPaymentSuccessNotification(
            getApplication(),
            invoice,
            total,
            merchant.merchantName
        )

        _uiMessage.value = "Pembayaran ${QrisEngine.formatRupiah(total)} BERHASIL diterima!"

        // Update transaction status in Room
        viewModelScope.launch {
            val txId = _currentTransactionId.value
            val entity = TransactionEntity(
                id = txId,
                invoiceNumber = invoice,
                customerName = _customerName.value.trim(),
                nominal = total,
                fee = 0,
                totalAmount = total,
                note = "Lunas via QRIS Dinamis",
                merchantName = merchant.merchantName,
                qrisPayload = _activeDynamicQris.value,
                status = "SUCCESS",
                timestamp = System.currentTimeMillis(),
                paymentSource = "QRIS Dinamis"
            )
            repository.insert(entity)
        }
    }

    fun toggleMode() {
        if (!_isQrisMode.value) {
            generateDynamicQris()
        } else {
            _isQrisMode.value = false
        }
    }

    // ----------------------------------------------------
    // Sharing & Saving Gallery
    // ----------------------------------------------------
    fun saveQrisToGallery() {
        val payload = _activeDynamicQris.value
        if (payload.isBlank()) return

        val merchant = _merchantInfo.value
        val bitmap = ImageShareUtils.createQrisCardBitmap(
            context = getApplication(),
            merchantName = merchant.merchantName,
            merchantCity = merchant.merchantCity,
            nmid = merchant.nmid,
            invoice = _activeInvoice.value,
            amount = _displayTotal.value,
            qrisPayload = payload,
            customerName = _customerName.value.trim()
        )

        viewModelScope.launch {
            val result = ImageShareUtils.saveBitmapToGallery(
                context = getApplication(),
                bitmap = bitmap,
                title = "QRIS_${_activeInvoice.value}"
            )
            result.onSuccess {
                _uiMessage.value = "Kode QRIS berhasil disimpan ke Galeri Foto! 📸"
            }.onFailure { err ->
                _uiMessage.value = "Gagal menyimpan ke galeri: ${err.message}"
            }
        }
    }

    fun shareQrisImage() {
        val payload = _activeDynamicQris.value
        if (payload.isBlank()) return

        val merchant = _merchantInfo.value
        val total = _displayTotal.value
        val cust = _customerName.value.trim()
        val bitmap = ImageShareUtils.createQrisCardBitmap(
            context = getApplication(),
            merchantName = merchant.merchantName,
            merchantCity = merchant.merchantCity,
            nmid = merchant.nmid,
            invoice = _activeInvoice.value,
            amount = total,
            qrisPayload = payload,
            customerName = cust
        )

        val custStr = if (cust.isNotBlank()) " untuk $cust" else ""
        val caption = "Silakan scan kode QRIS ${QrisEngine.formatRupiah(total)}$custStr untuk pembayaran di ${merchant.merchantName} (${_activeInvoice.value}). Terima kasih!"

        viewModelScope.launch {
            ImageShareUtils.shareBitmap(
                context = getApplication(),
                bitmap = bitmap,
                subject = "Kode QRIS ${_activeInvoice.value}",
                text = caption
            )
        }
    }

    // ----------------------------------------------------
    // Bluetooth Thermal Printing
    // ----------------------------------------------------
    private fun loadSavedPrinter() {
        val address = securePrefs.getStringDecrypted("selected_printer_addr", "")
        val name = securePrefs.getStringDecrypted("selected_printer_name", "")
        if (address.isNotBlank()) {
            _selectedPrinter.value = BluetoothPrinterDevice(name.ifBlank { "Printer Bluetooth" }, address, true)
        }
    }

    fun refreshBluetoothDevices() {
        val devices = BluetoothThermalPrinter.getPairedDevices(getApplication())
        _pairedPrinters.value = devices
        val savedAddr = securePrefs.getStringDecrypted("selected_printer_addr", "")
        val matched = devices.firstOrNull { it.address == savedAddr }
        if (matched != null) {
            _selectedPrinter.value = matched
        } else if (_selectedPrinter.value == null || _selectedPrinter.value?.address == "00:11:22:33:44:55") {
            _selectedPrinter.value = devices.firstOrNull { !it.name.contains("Demo", true) } ?: devices.firstOrNull()
        }
    }

    fun selectPrinter(device: BluetoothPrinterDevice) {
        _selectedPrinter.value = device
        securePrefs.saveStringEncrypted("selected_printer_addr", device.address)
        securePrefs.saveStringEncrypted("selected_printer_name", device.name)
        _uiMessage.value = "Printer '${device.name}' berhasil dipilih!"
    }

    fun printCurrentReceipt() {
        val merchant = _merchantInfo.value
        val total = _displayTotal.value
        val invoice = _activeInvoice.value.ifBlank { "INV-" + System.currentTimeMillis() }

        if (_selectedPrinter.value == null) {
            refreshBluetoothDevices()
        }

        val targetPrinter = _selectedPrinter.value
        if (targetPrinter == null || targetPrinter.name.contains("Demo", true) || targetPrinter.address == "00:11:22:33:44:55") {
            _uiMessage.value = "Harap pilih printer Bluetooth fisik di menu Printer atau Pengaturan HP terlebih dahulu."
            return
        }

        val receiptItems = getReceiptItems()

        val receipt = ReceiptData(
            storeName = merchant.merchantName,
            storeAddress = _receiptStoreAddress.value.ifBlank { "${merchant.merchantCity} (NMID: ${merchant.nmid})" },
            storePhone = _receiptStorePhone.value,
            invoiceNumber = invoice,
            customerName = _customerName.value.trim(),
            date = Date(),
            items = receiptItems,
            subtotal = total,
            fee = 0,
            totalAmount = total,
            paymentMethod = "QRIS Dinamis",
            qrisPayload = _activeDynamicQris.value,
            footerMessage = _receiptFooterMessage.value
        )

        _isPrinting.value = true
        viewModelScope.launch {
            val result = BluetoothThermalPrinter.printReceipt(targetPrinter.address, receipt)
            _isPrinting.value = false
            result.onSuccess { msg ->
                _uiMessage.value = msg
            }.onFailure { err ->
                _uiMessage.value = "Gagal mencetak: ${err.message}"
            }
        }
    }

    fun testPrinterConnection() {
        val targetPrinter = _selectedPrinter.value
        if (targetPrinter == null || targetPrinter.name.contains("Demo", true) || targetPrinter.address == "00:11:22:33:44:55") {
            _uiMessage.value = "Pilih printer Bluetooth fisik di daftar perangkat terlebih dahulu."
            return
        }

        _isPrinting.value = true
        viewModelScope.launch {
            val result = BluetoothThermalPrinter.testPrinterConnection(targetPrinter.address)
            _isPrinting.value = false
            result.onSuccess { msg ->
                _uiMessage.value = msg
            }.onFailure { err ->
                _uiMessage.value = err.message ?: "Gagal tes koneksi printer"
            }
        }
    }

    fun printHistoryReceipt(tx: com.example.data.TransactionEntity) {
        val merchant = _merchantInfo.value
        if (_selectedPrinter.value == null) {
            refreshBluetoothDevices()
        }

        val targetPrinter = _selectedPrinter.value
        if (targetPrinter == null || targetPrinter.name.contains("Demo", true) || targetPrinter.address == "00:11:22:33:44:55") {
            _uiMessage.value = "Pilih printer Bluetooth fisik di menu Printer terlebih dahulu."
            return
        }

        val receipt = ReceiptData(
            storeName = merchant.merchantName,
            storeAddress = _receiptStoreAddress.value.ifBlank { "${merchant.merchantCity} (NMID: ${merchant.nmid})" },
            storePhone = _receiptStorePhone.value,
            invoiceNumber = tx.invoiceNumber,
            customerName = tx.customerName,
            date = java.util.Date(tx.timestamp),
            items = listOf("Transaksi QRIS" to tx.totalAmount),
            subtotal = tx.nominal,
            fee = tx.fee,
            totalAmount = tx.totalAmount,
            paymentMethod = tx.paymentSource.ifBlank { "QRIS Dinamis" },
            qrisPayload = tx.qrisPayload,
            footerMessage = _receiptFooterMessage.value
        )

        _isPrinting.value = true
        viewModelScope.launch {
            val result = BluetoothThermalPrinter.printReceipt(targetPrinter.address, receipt)
            _isPrinting.value = false
            result.onSuccess { msg ->
                _uiMessage.value = msg
            }.onFailure { err ->
                _uiMessage.value = "Gagal mencetak: ${err.message}"
            }
        }
    }

    // ----------------------------------------------------
    // Encrypted Merchant Profile & Base QRIS Upload
    // ----------------------------------------------------
    private fun loadSavedMerchantProfile() {
        val name = securePrefs.getStringDecrypted("merchant_name", "WARUNG BERKAH QRIS")
        val city = securePrefs.getStringDecrypted("merchant_city", "JAKARTA")
        val postal = securePrefs.getStringDecrypted("merchant_postal", "10110")
        val nmid = securePrefs.getStringDecrypted("merchant_nmid", "ID1020304050607")
        val mpan = securePrefs.getStringDecrypted("merchant_mpan", "936001234567890")
        val payload = securePrefs.getStringDecrypted("merchant_qris_payload", QrisEngine.DEFAULT_STATIC_QRIS)

        _merchantInfo.value = MerchantInfo(
            merchantName = name,
            merchantCity = city,
            postalCode = postal,
            nmid = nmid,
            mpan = mpan,
            rawPayload = payload
        )
    }

    fun saveMerchantProfile(
        name: String,
        city: String,
        postal: String,
        nmid: String,
        mpan: String,
        rawPayload: String
    ) {
        val payload = if (rawPayload.isNotBlank()) {
            rawPayload
        } else {
            QrisEngine.createTemplateQris(name, city, postal, nmid, mpan)
        }

        // Encrypt and persist
        securePrefs.saveStringEncrypted("merchant_name", name)
        securePrefs.saveStringEncrypted("merchant_city", city)
        securePrefs.saveStringEncrypted("merchant_postal", postal)
        securePrefs.saveStringEncrypted("merchant_nmid", nmid)
        securePrefs.saveStringEncrypted("merchant_mpan", mpan)
        securePrefs.saveStringEncrypted("merchant_qris_payload", payload)

        _merchantInfo.value = MerchantInfo(
            merchantName = name,
            merchantCity = city,
            postalCode = postal,
            nmid = nmid,
            mpan = mpan,
            rawPayload = payload
        )
        _uiMessage.value = "Profil bisnis & QRIS tersimpan aman dengan Enkripsi AES-256! 🔒"
    }

    fun processUploadedQrisBitmap(bitmap: Bitmap) {
        val decoded = QrisEngine.decodeQrFromBitmap(bitmap)
        if (decoded != null && decoded.contains("000201")) {
            val parsed = QrisEngine.parseMerchantInfo(decoded)
            saveMerchantProfile(
                name = parsed.merchantName,
                city = parsed.merchantCity,
                postal = parsed.postalCode,
                nmid = parsed.nmid,
                mpan = parsed.mpan,
                rawPayload = decoded
            )
            _uiMessage.value = "QRIS Toko '${parsed.merchantName}' berhasil dipindai & dikonfigurasi! ✨"
        } else if (decoded != null) {
            _uiMessage.value = "QR Code terdeteksi namun bukan standar QRIS EMVCo"
        } else {
            _uiMessage.value = "Tidak dapat membaca QR Code dari gambar. Pastikan gambar jelas."
        }
    }

    // ----------------------------------------------------
    // Theme & Interactive Guide Preferences
    // ----------------------------------------------------
    private fun loadThemePreference() {
        _themeMode.value = securePrefs.getStringDecrypted("theme_mode", "SYSTEM")
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        securePrefs.saveStringEncrypted("theme_mode", mode)
    }

    private fun loadFirstLaunchGuide() {
        val hasSeen = securePrefs.getBoolean("has_seen_guide", false)
        if (!hasSeen) {
            _showGuide.value = true
        }
    }

    // ----------------------------------------------------
    // Receipt Print Header & Footer Preferences
    // ----------------------------------------------------
    private fun loadSavedReceiptSettings() {
        val address = securePrefs.getStringDecrypted("receipt_store_address", "")
        val phone = securePrefs.getStringDecrypted("receipt_store_phone", "081947215703")
        val footer = securePrefs.getStringDecrypted(
            "receipt_footer_message",
            "Terima Kasih Atas Kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar."
        )
        _receiptStoreAddress.value = address
        _receiptStorePhone.value = phone
        _receiptFooterMessage.value = footer
    }

    fun saveReceiptPrintSettings(address: String, phone: String, footer: String) {
        _receiptStoreAddress.value = address
        _receiptStorePhone.value = phone
        _receiptFooterMessage.value = footer

        securePrefs.saveStringEncrypted("receipt_store_address", address)
        securePrefs.saveStringEncrypted("receipt_store_phone", phone)
        securePrefs.saveStringEncrypted("receipt_footer_message", footer)

        _uiMessage.value = "Pengaturan header & footer struk berhasil disimpan! ✅"
    }

    fun openGuide() {
        _showGuide.value = true
    }

    fun dismissGuide() {
        _showGuide.value = false
        securePrefs.saveBoolean("has_seen_guide", true)
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.delete(tx)
            _uiMessage.value = "Transaksi ${tx.invoiceNumber} dihapus"
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.deleteAll()
            _uiMessage.value = "Semua riwayat transaksi telah dibersihkan"
        }
    }
}
