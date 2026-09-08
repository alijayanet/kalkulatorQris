package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qris_transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val customerName: String = "",
    val nominal: Long,
    val fee: Long = 0,
    val totalAmount: Long,
    val note: String = "",
    val merchantName: String,
    val qrisPayload: String,
    val status: String = "SUCCESS", // SUCCESS, PENDING, CANCELLED
    val timestamp: Long = System.currentTimeMillis(),
    val paymentSource: String = "QRIS Dinamis"
)
