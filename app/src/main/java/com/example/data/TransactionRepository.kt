package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TransactionRepository(private val dao: TransactionDao) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()

    fun getTodayTransactions(): Flow<List<TransactionEntity>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return dao.getTodayTransactions(calendar.timeInMillis)
    }

    suspend fun insert(transaction: TransactionEntity): Long {
        return dao.insertTransaction(transaction)
    }

    suspend fun update(transaction: TransactionEntity) {
        dao.updateTransaction(transaction)
    }

    suspend fun delete(transaction: TransactionEntity) {
        dao.deleteTransaction(transaction)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
