package com.github.lewcol.myapplication.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    // Insert a new receipt
    @Insert(onConflict=OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: Receipt) : Int

    // Insert a new receipt item
    @Insert(onConflict=OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    // Get receipt with its items
    @Transaction
    @Query("SELECT * FROM receipts")
    fun getReceiptWithItems(): Flow<List<ReceiptWithItems>>

    // Update receipt
    @Update
    suspend fun updateReceipt(receipt: Receipt)

    // Update receipt item
    @Update
    suspend fun updateItem(item: Item)

    // Delete receipt and its child items
    @Delete
    suspend fun deleteReceipt(receipt: Receipt)

    // Delete receipt item
    @Delete
    suspend fun deleteItem(item: Item)
}