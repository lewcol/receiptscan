package com.github.lewcol.myapplication.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.github.lewcol.myapplication.utils.millisToLocalDate
import com.github.lewcol.myapplication.viewmodel.ReceiptViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Reports(context: Context, viewModel: ReceiptViewModel, onReportGenerated: (String) -> Unit) {
    var reportContent by remember { mutableStateOf<String?>(null) }
    var showStartDateModal by rememberSaveable { mutableStateOf(false) }
    var showEndDateModal by rememberSaveable { mutableStateOf(false) }
    var storeName by rememberSaveable { mutableStateOf("") }
    val today = LocalDate.now()
    val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    var startDate by rememberSaveable { mutableStateOf(todayMillis) }
    var endDate by rememberSaveable { mutableStateOf(todayMillis) }
    var startLocalDate by rememberSaveable { mutableStateOf(today) }
    var endLocalDate by rememberSaveable { mutableStateOf(today) }

    LaunchedEffect(reportContent) {
        if (reportContent != null) {
            onReportGenerated(reportContent!!)
            reportContent = null
        }
    }

    Scaffold (
        topBar={
            TopAppBar(
                colors= TopAppBarDefaults.topAppBarColors(
                    containerColor=MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor=MaterialTheme.colorScheme.primary
                ),
                title={Text("Generate Report")}
            )
        }
    ){ innerPadding ->
        Column (
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ){
            TextField(
                value = storeName,
                label = { Text("Store Name") },
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { storeName = it }
            )

            // Set start date
            Column(Modifier.fillMaxWidth()) {
                startLocalDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))?.let {
                    TextField(
                        value = it,
                        readOnly = true,
                        label = { Text("Start Date") },
                        onValueChange = {},
                        trailingIcon = {
                            IconButton(onClick = { showStartDateModal = !showStartDateModal }) {
                                Icon(Icons.Default.DateRange, "Date Selector")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (showStartDateModal) {
                    Dialog(onDismissRequest = { showStartDateModal = false }) {
                        DatePickerModal(
                            onDateSelected = {
                                it?.let {
                                    startLocalDate = millisToLocalDate(it)
                                    startDate = it
                                }
                                showStartDateModal = false
                            },
                            onDismiss={ showStartDateModal = false }
                        )
                    }
                }
            }

            // Set end date
            Column(Modifier.fillMaxWidth()) {
                endLocalDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))?.let {
                    TextField(
                        value = it,
                        readOnly = true,
                        label = { Text("End Date") },
                        onValueChange = {},
                        trailingIcon = {
                            IconButton(onClick = { showEndDateModal = !showEndDateModal }) {
                                Icon(Icons.Default.DateRange, "Date Selector")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (showEndDateModal) {
                    Dialog(onDismissRequest = { showEndDateModal = false }) {
                        DatePickerModal(
                            onDateSelected = {
                                it?.let {
                                    endLocalDate = millisToLocalDate(it)
                                    endDate = it
                                }
                                showEndDateModal = false
                            },
                            onDismiss={ showEndDateModal = false }
                        )
                    }
                }
            }

            Button(onClick = {
                if (storeName.isBlank()) {
                    Toast.makeText(context, "Enter a Store Name", Toast.LENGTH_SHORT).show()
                } else {
                    val generatedReport = viewModel.generateStoreNameReport(storeName)
                    reportContent = generatedReport
                }
            }, content = { Text("Generate Report for Store") })
            Button(onClick = {
                if (startDate > endDate) {
                    Toast.makeText(context, "Start Date can't be after End Date", Toast.LENGTH_SHORT).show()
                } else {
                    val generatedReport = viewModel.generateDateReport(startDate, endDate)
                    reportContent = generatedReport
                }
            }, content = { Text("Generate Report for Dates") })
        }
    }
}