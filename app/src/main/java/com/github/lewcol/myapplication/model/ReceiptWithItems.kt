package com.github.lewcol.myapplication.model

import androidx.room.Embedded
import androidx.room.Relation

data class ReceiptWithItems(
    @Embedded val receipt: Receipt,
    @Relation(
        parentColumn = "receiptId",
        entityColumn = "itemReceiptId"
    )
    val items: List<Item>
)
