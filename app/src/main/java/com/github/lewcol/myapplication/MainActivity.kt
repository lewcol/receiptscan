package com.github.lewcol.myapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.lewcol.myapplication.model.ReceiptDb
import com.github.lewcol.myapplication.ui.main.MainScaffold
import com.github.lewcol.myapplication.ui.theme.MyApplicationTheme
import com.github.lewcol.myapplication.viewmodel.OCRViewModel
import com.github.lewcol.myapplication.viewmodel.OCRViewModelFactory
import com.github.lewcol.myapplication.viewmodel.ReceiptViewModel
import com.github.lewcol.myapplication.viewmodel.ReceiptViewModelFactory
import java.io.OutputStream

class MainActivity : ComponentActivity() {
    private var reportContent: String = ""

    private val saveReportLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                if (uri != null) {
                    writeReportToUri(uri, reportContent)
                }
            } else {
                Toast.makeText(this, "Report generation cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    private fun initiateFileSave(content: String) {
        if (content.isEmpty()) {
            Toast.makeText(this, "Report content is empty, Cannot save.", Toast.LENGTH_SHORT).show()
            return
        }
        reportContent = content

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "Receipt_Report_${System.currentTimeMillis()}.txt")
        }

        saveReportLauncher.launch(intent)
    }

    private fun writeReportToUri(uri: Uri, content: String) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                outputStream.write(content.toByteArray())
                Toast.makeText(this, "Report saved successfully", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save report: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val saveReportCallback: (reportContent: String) -> Unit = { content ->
            initiateFileSave(content)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = ReceiptDb.getDatabase(this)
        val dao = db.receiptDao()

        setContent {
            MyApplicationTheme {
                val receiptViewModel: ReceiptViewModel = viewModel(factory=ReceiptViewModelFactory(dao))
                val ocrViewModel: OCRViewModel = viewModel(factory= OCRViewModelFactory(receiptViewModel))
                MainScaffold(this, receiptViewModel = receiptViewModel, ocrViewModel = ocrViewModel, onReportGenerated = saveReportCallback)
        }
    }
}

}