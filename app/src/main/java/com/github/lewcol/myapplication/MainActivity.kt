package com.github.lewcol.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.lewcol.myapplication.model.ReceiptDb
import com.github.lewcol.myapplication.ui.main.MainScaffold
import com.github.lewcol.myapplication.ui.theme.MyApplicationTheme
import com.github.lewcol.myapplication.viewmodel.ReceiptViewModel
import com.github.lewcol.myapplication.viewmodel.ReceiptViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = ReceiptDb.getDatabase(this)
        val dao = db.receiptDao()

        setContent {
            MyApplicationTheme {
                val viewModel: ReceiptViewModel = viewModel(factory=ReceiptViewModelFactory(dao))
                MainScaffold(viewModel = viewModel)
        }
    }
}

}