package com.github.lewcol.myapplication.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="receipts")
data class Receipt(
    @PrimaryKey(autoGenerate=true) val receiptId : Int = 0,
    val storeName : String,
    val date : Long
)
