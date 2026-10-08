package com.github.lewcol.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.lewcol.myapplication.model.ReceiptDao
import com.github.lewcol.myapplication.model.ReceiptRepository
import kotlin.jvm.java

class ReceiptViewModelFactory(private val dao: ReceiptDao): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(ReceiptViewModel::class.java)){
            val repository = ReceiptRepository(dao)
            @Suppress("UNCHECKED_CAST")
            return ReceiptViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}