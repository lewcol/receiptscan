package com.github.lewcol.myapplication.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.lewcol.myapplication.R
import com.github.lewcol.myapplication.model.ReceiptWithItems
import com.github.lewcol.myapplication.viewmodel.ReceiptViewModel

@Composable
fun ReceiptButton(navController: NavHostController, fullReceipt: ReceiptWithItems, delete : (Long) -> Unit) {
    var contextMenuExpanded by remember { mutableStateOf(false) }

    Card (
        Modifier
            .fillMaxWidth()
            .padding(horizontal=8.dp, vertical=4.dp)
            .combinedClickable(
                onClick = {
                    navController.navigate("receiptdetails/${fullReceipt.receipt.receiptId}")
                },
                onLongClick = {
                    contextMenuExpanded = true
                }
            ),
        elevation= CardDefaults.elevatedCardElevation(2.dp),
        shape=MaterialTheme.shapes.medium,
        colors=CardDefaults.cardColors(
            containerColor=MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Row (
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment=Alignment.CenterVertically
        ) {
            Image(
                painter=painterResource(id=R.drawable.outline_description_24),
                contentDescription="Receipt Icon",
                modifier=Modifier.padding(8.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text("%s - €%.2f".format(fullReceipt.receipt.storeName, fullReceipt.receipt.total))
        }

        DropdownMenu(
            expanded=contextMenuExpanded,
            onDismissRequest={ contextMenuExpanded = false },
        ) {
            DropdownMenuItem(
                text={ Text("Delete") },
                onClick={
                    delete(fullReceipt.receipt.receiptId)
                    contextMenuExpanded = false
                }
            )
        }
    }
}

@Composable
fun DeleteReceiptBox(receipt: Long, onDismiss: () -> Unit, viewModel: ReceiptViewModel) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Receipt?") },
        text = {
            Text(
                text = "Are you sure you want to delete this receipt?"
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.removeReceipt(receipt)
                    onDismiss()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptList(navController: NavHostController, viewModel: ReceiptViewModel) {
    val receipts by viewModel.receipts.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedReceipt by remember { mutableStateOf(-1L) }
    var storeNameQuery by rememberSaveable { mutableStateOf("") }
    var itemQuery by rememberSaveable { mutableStateOf("") }
    val filteredReceipts by remember(receipts, storeNameQuery, itemQuery) {
        derivedStateOf {
            receipts
                .filter{ storeNameQuery.isBlank() || it.receipt.storeName.contains(storeNameQuery, ignoreCase = true) }
                .filter{ item -> itemQuery.isBlank() || item.items.any { it.name.contains(itemQuery, ignoreCase = true) } }
        }
    }

    Scaffold (
        topBar={
            TopAppBar(
                colors= TopAppBarDefaults.topAppBarColors(
                    containerColor=MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor=MaterialTheme.colorScheme.primary
                ),
                title={Text("Receipt List")}
            )
        },
        floatingActionButton={
            FloatingActionButton (
                onClick={
                    viewModel.clearState()
                    navController.navigate("receiptdetails/${-1}")
                }
            ) { Icon(Icons.Filled.Add, contentDescription="Add Note") }
        }
    ){ innerPadding ->
        LazyColumn (
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ){ item {
            TextField(
                    value = storeNameQuery,
                    label = { Text("Store Name") },
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { storeNameQuery = it }
                )
            }

            item {
                TextField(
                    value = itemQuery,
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { itemQuery = it }
                )
            }

            items(filteredReceipts) { receipt ->
            ReceiptButton(navController, receipt, delete = { id ->
                selectedReceipt = id
                showDeleteDialog = true
            })
        } }
    }

    if (showDeleteDialog && selectedReceipt >= 0) {
        DeleteReceiptBox(
            receipt = selectedReceipt,
            onDismiss = {
                showDeleteDialog = false
                selectedReceipt = -1L
            },
            viewModel = viewModel
        )
    }

}