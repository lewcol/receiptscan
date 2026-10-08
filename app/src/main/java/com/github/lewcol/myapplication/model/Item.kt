package com.github.lewcol.myapplication.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName="items",
    foreignKeys=[
        ForeignKey(
            entity = Receipt::class,
            parentColumns = ["receiptId"],
            childColumns = ["itemReceiptId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Item(
    @PrimaryKey(autoGenerate=true) val itemId : Long = 0,
    val name : String,
    val price : Float,
    val itemReceiptId : Long
)
