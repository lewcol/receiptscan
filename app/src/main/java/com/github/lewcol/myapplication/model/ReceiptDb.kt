package com.github.lewcol.myapplication.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Receipt::class, Item::class], version = 1, exportSchema = false)
abstract class ReceiptDb: RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao

    companion object {
        @Volatile
        private var INSTANCE: ReceiptDb? = null

        fun getDatabase(context: Context): ReceiptDb {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReceiptDb::class.java,
                    "receipt_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}