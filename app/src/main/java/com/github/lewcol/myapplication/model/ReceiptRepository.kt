package com.github.lewcol.myapplication.model

import kotlinx.coroutines.flow.Flow

class ReceiptRepository(private val receiptDao: ReceiptDao) {
    val receipts: Flow<List<ReceiptWithItems>> = receiptDao.getReceiptWithItems()
    suspend fun insertReceipt(receipt: Receipt) : Int = receiptDao.insertReceipt(receipt)
    suspend fun insertItem(item: Item) = receiptDao.insertItem(item)
    suspend fun updateReceipt(receipt: Receipt) = receiptDao.updateReceipt(receipt)
    suspend fun updateItem(item: Item) = receiptDao.updateItem(item)
    suspend fun deleteReceipt(receipt: Receipt) = receiptDao.deleteReceipt(receipt)
    suspend fun deleteReceiptById(id: Int) = receiptDao.deleteReceiptById(id)
    suspend fun deleteItem(item: Item) = receiptDao.deleteItem(item)
}