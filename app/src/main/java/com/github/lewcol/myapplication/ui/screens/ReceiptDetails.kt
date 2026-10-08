package com.github.lewcol.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.github.lewcol.myapplication.utils.millisToLocalDate
import com.github.lewcol.myapplication.viewmodel.ReceiptViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ItemCard(item: ReceiptViewModel.ItemInputState, edit: (Long) -> Unit, delete: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var contextMenuExpanded by remember { mutableStateOf(false) }

    Card (
        Modifier
            .fillMaxWidth()
            .padding(horizontal=8.dp, vertical=4.dp)
            .clickable{ expanded = !expanded }
            .pointerInput(Unit){
                detectTapGestures (
                    onLongPress= {
                        contextMenuExpanded = true
                    },
                    onTap={
                        expanded = !expanded
                    }
                )
            },
        elevation=CardDefaults.elevatedCardElevation(2.dp),
        shape=MaterialTheme.shapes.medium,
        colors=CardDefaults.cardColors(
            containerColor=MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse note" else "Expand note",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column {
                Text(
                    text = "Price: €%.2f".format(item.price.toFloatOrNull() ?: 0.0),
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                        .fillMaxWidth(),
                    maxLines = 1,
                )
            }

            DropdownMenu(
                expanded=contextMenuExpanded,
                onDismissRequest={ contextMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text={ Text("Edit") },
                    onClick={
                        edit(item.itemId)
                        contextMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text={ Text("Delete") },
                    onClick={
                        delete(item.itemId)
                        contextMenuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun EditBox(item: ReceiptViewModel.ItemInputState, onDismiss: () -> Unit, viewModel: ReceiptViewModel) {
    val title = "Editing Item"
    Card (
        Modifier
            .padding(32.dp)
            .fillMaxWidth(.8f),
        elevation=CardDefaults.elevatedCardElevation(8.dp),
        shape=MaterialTheme.shapes.large
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row (
                Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically
            ) {
                IconButton(onClick=onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription="Close Dialog")
                }
                Text(
                    text=title,
                    fontSize=18.sp,
                    color=Color.LightGray,
                    modifier=Modifier.weight(1f),
                    maxLines=1,
                    overflow=TextOverflow.Ellipsis
                )
            }
            OutlinedTextField(
                value = item.name,
                onValueChange = { viewModel.updateItemName(item.itemId, it) },
                label = { Text("Name") },
                isError = !item.nameError.isNullOrBlank(),
                supportingText = {
                    if (!item.nameError.isNullOrBlank()) {
                        Text(
                            text = item.nameError,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
            )
            OutlinedTextField(
                value = item.price,
                onValueChange = { viewModel.updatePrice(item.itemId, it) },
                label = { Text("Price") },
                isError = !item.priceError.isNullOrBlank(),
                supportingText = {
                    if (!item.priceError.isNullOrBlank()) {
                        Text(
                            text = item.priceError,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
fun DeleteBox(item: ReceiptViewModel.ItemInputState, onDismiss: () -> Unit, viewModel: ReceiptViewModel) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Item?") },
        text = {
            Text(
                text = "Are you sure you want to delete this item?"
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.removeItem(item.itemId)
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
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetails(navController: NavHostController, viewModel: ReceiptViewModel, receiptId: Long?) {
    val currentReceipt by viewModel.state.collectAsState()

    LaunchedEffect(receiptId) {
        if (receiptId != null) {
            if (receiptId >= 0) {
                viewModel.loadExistingReceipt(receiptId)
            }
        } else {
            viewModel.clearState()
        }
    }

    var showDateModal by rememberSaveable { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableLongStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = { Text("Task Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to List"
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val newItem = viewModel.addItem()
                    selectedItem = newItem.itemId
                    showEditDialog = true
                }
            ) { Icon(Icons.Filled.Add, contentDescription = "Add Item") }
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            item {
                OutlinedTextField(
                    value = currentReceipt.storeName,
                    onValueChange = { viewModel.updateStoreName(it) },
                    label = { Text("Store Name") },
                    isError = !currentReceipt.storeNameError.isNullOrBlank(),
                    supportingText = {
                        if (!currentReceipt.storeNameError.isNullOrBlank()) {
                            currentReceipt.storeNameError?.let {
                                Text(text = it, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Column(Modifier.fillMaxWidth()) {
                    currentReceipt.date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))?.let {
                        TextField(
                            value = it,
                            readOnly = true,
                            label = { Text("Date") },
                            onValueChange = {},
                            trailingIcon = {
                                IconButton(onClick = { showDateModal = !showDateModal }) {
                                    Icon(Icons.Default.DateRange, "Date Selector")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (showDateModal) {
                        Dialog(onDismissRequest = { showDateModal = false }) {
                            DatePickerModal(
                                onDateSelected = {
                                    viewModel.updateDate(it?.let { millisToLocalDate(it) })
                                    showDateModal = false
                                },
                                onDismiss={ showDateModal = false }
                            )
                        }
                    }
                }
            }
            items(
                currentReceipt.items,
                key = { item -> item.itemId }
            ) { item ->
                ItemCard(
                    item,
                    edit = { id ->
                        selectedItem = id
                        showEditDialog = true
                    },
                    delete = { id ->
                        selectedItem = id
                        showDeleteDialog = true
                    })
            }
        }

        val itemToEditOrDelete = currentReceipt.items.find { it.itemId == selectedItem }

        if (showEditDialog) {
            Dialog(onDismissRequest = { showEditDialog = false }) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (itemToEditOrDelete != null) {
                        EditBox(
                            item = itemToEditOrDelete,
                            onDismiss = {
                                showEditDialog = false
                                selectedItem = 0L
                            },
                            viewModel = viewModel
                        )
                    } else {
                        LaunchedEffect(Unit) { showEditDialog = false }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            Dialog(onDismissRequest = { showDeleteDialog = false }) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (itemToEditOrDelete != null) {
                        DeleteBox(
                            item = itemToEditOrDelete,
                            onDismiss = {
                                showDeleteDialog = false
                                selectedItem = 0L
                            },
                            viewModel = viewModel
                        )
                    } else {
                        LaunchedEffect(Unit) { showDeleteDialog = false }
                    }
                }
            }
        }
    }
}