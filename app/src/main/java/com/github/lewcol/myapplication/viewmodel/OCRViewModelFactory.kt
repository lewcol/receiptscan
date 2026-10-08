package com.github.lewcol.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class OCRViewModelFactory(private val receiptViewModel: ReceiptViewModel): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(OCRViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return OCRViewModel(receiptViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}