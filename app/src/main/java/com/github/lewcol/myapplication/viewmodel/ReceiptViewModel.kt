package com.github.lewcol.myapplication.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.lewcol.myapplication.model.Item
import com.github.lewcol.myapplication.model.Receipt
import com.github.lewcol.myapplication.model.ReceiptRepository
import com.github.lewcol.myapplication.model.ReceiptWithItems
import com.github.lewcol.myapplication.utils.millisToLocalDate
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID


@OptIn(FlowPreview::class)
class ReceiptViewModel(private val repository: ReceiptRepository) : ViewModel() {
    // Store current state of receipt and each item
    data class ReceiptInputState(
        val id: Long? = null,    // Remember id in database if this is not new
        val date: LocalDate? = millisToLocalDate(System.currentTimeMillis()),
        val storeName: String = "",
        val dateError: String? = null,
        val storeNameError: String? = null,
        val total: Float = 0.0f,
        val items: List<ItemInputState>
    ) {
        val isReceiptFormValid: Boolean     // Flag errors
            get() = date != null && dateError == null && storeNameError == null
                    && items.all{ it.isItemFormValid } && items.isNotEmpty()
    }

    data class ItemInputState(
        val itemId: Long,
        val name: String = "",
        val nameError: String? = null,
        val price: String = "",
        val priceError: String? = null
    ) {
        val isItemFormValid: Boolean
            get() = nameError == null && priceError == null
    }

    private val _state = MutableStateFlow(ReceiptInputState(items = listOf(createNewItemState())))
    val state: StateFlow<ReceiptInputState> = _state.asStateFlow()

    companion object {
        private var nextTempId = -1L

        private fun createNewItemState() = ItemInputState(itemId = --nextTempId)
    }

    // Current receipts in data layer
    val receipts = repository.receipts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        viewModelScope.launch {
            _state
                .debounce { 1000L }
                .drop(1)
                .collect {
                    saveReceiptToDatabase()
                }
        }
    }

    fun clearState() {
        _state.value = ReceiptInputState(items = listOf(createNewItemState()))
    }

    // Update and validate date
    fun updateDate(newDate: LocalDate?) {
        _state.update { it.copy(date = newDate) }
        validateDate(newDate)
    }

    private fun validateDate(date: LocalDate?) {
        val error = if (date == null) "Date is required." else ""
        _state.update { it.copy(dateError = error) }
    }

    // Update and validate store name
    fun updateStoreName(newStoreName: String) {
        _state.update { it.copy(storeName = newStoreName) }
        validateStoreName(newStoreName)
    }

    private fun validateStoreName(storeName: String) {
        val error = if (storeName.isBlank()) "Store name is required." else ""
        _state.update { it.copy(storeNameError = error) }
    }

    private fun calculateTotal(items: List<ItemInputState>): Float {
        return items.mapNotNull { it.price.toFloatOrNull() }.sum()
    }

    private fun calculateTotalForRawItems(items: List<Item>): Float {
        return items.map { it.price }.sum()
    }

    // Add a blank new item
    fun addItem(): ItemInputState {
        val newItem = createNewItemState()
        _state.update {
            it.copy(items = it.items + newItem, total = calculateTotal(it.items + newItem))
        }
        return newItem
    }

    // Delete an item
    fun removeItem(itemId: Long) {
        _state.update { state ->
            val updatedItems = state.items.filter { item -> item.itemId != itemId }
            state.copy(
                items = updatedItems,
                total = calculateTotal(updatedItems)
            )
        }
    }

    // Update and validate item name
    fun updateItemName(itemId: Long, itemName: String) {
        _state.update { it.copy(items = it.items.map { item ->
            if (item.itemId == itemId) {
                item.copy(name = itemName,
                          nameError = if (itemName.isBlank()) "Item name is required." else ""
                )
            } else item
        }) }
    }

    // Update and validate item price
    fun updatePrice(itemId: Long, price: String) {
        val priceError = validatePrice(price)
        _state.update {
            val updatedItems = it.items.map { item ->
                if (item.itemId == itemId) {
                    item.copy(
                        price = price,
                        priceError = priceError
                    )
                } else item
            }
            it.copy(
                items = updatedItems,
                total = calculateTotal(updatedItems)
            )
        }
    }

    private fun validatePrice(price: String): String? {
        val regex = "^[0-9]+(\\.[0-9]{1,2})?$".toRegex()
        return if (regex.containsMatchIn(price)) "" else "Invalid price provided."
    }

    // Save receipt and its items from state to database
    fun saveReceiptToDatabase() {
        val state = _state.value
        if (state.date == null) return

        val receipt = Receipt(
            receiptId = state.id ?: 0L,
            date = state.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            storeName = state.storeName,
            total = state.total
        )

        val itemStates = state.items

        var items = emptyList<Item>()

        for (i in itemStates) {
            items = items + Item(
                itemId = UUID.randomUUID().mostSignificantBits,
                itemReceiptId = receipt.receiptId,
                name = i.name,
                price = i.price.toFloatOrNull() ?: 0.0f,
            )
        }

        viewModelScope.launch {
            try {
                val newReceiptId = repository.upsertReceiptWithItems(receipt, items)
                if (state.id == null) {
                    _state.update {
                        it.copy(id = newReceiptId)
                    }
                }
            } catch (e: Exception) {
                Log.e("InsertFailure","Insertion failed:", e)
            }
        }
    }

    // Load existing receipt and its items from database to state
    fun loadExistingReceipt(receiptId: Long) {
        viewModelScope.launch {
            val receiptWithItems = receipts.value.firstOrNull { it.receipt.receiptId == receiptId }
            receiptWithItems?.let { data ->
                val loadedItems = data.items.map { dbItem ->
                    ItemInputState(
                        itemId = dbItem.itemId,
                        name = dbItem.name,
                        price = dbItem.price.toString()
                    )
                }
                _state.update {
                    ReceiptInputState(
                        id = data.receipt.receiptId,
                        storeName = data.receipt.storeName,
                        date = millisToLocalDate(data.receipt.date),
                        total = calculateTotal(loadedItems),
                        items = loadedItems
                    )
                }
            }
        }
    }

fun loadParsedReceipt(parsedData: Map<String, String>) {
    var items = emptyList<ItemInputState>()
    val storeName = parsedData["_title"] ?: ""
    val date = parsedData["_date"]?.let { millisToLocalDate(it.toLong()) } ?: LocalDate.now()

    // Populate items
    parsedData.forEach { (k, v) ->
        if (!k.startsWith('_')) {
            val newItem = ItemInputState(
                itemId = --nextTempId,
                name = k,
                price = v,
                nameError = null,
                priceError = null
            )
            items = items + newItem
        }
    }

    _state.update {
        ReceiptInputState(
            id = null,
            storeName = storeName,
            date = date,
            total = calculateTotal(items),
            items = items
        )
    }
}

    fun removeReceipt(receiptId: Long) {
        viewModelScope.launch {
            repository.deleteReceiptById(receiptId)
        }
    }

    fun generateReport(filteredReceipts: List<ReceiptWithItems>) : String {
        val header = "--- Receipt Report ---\n" +
                "Generated on: ${LocalDate.now()}\n\n"

        val reportBuilder = StringBuilder(header)
        var overallTotal = 0.0f

        filteredReceipts.forEachIndexed { i, fullReceipt ->
            val receipt = fullReceipt.receipt
            val items = fullReceipt.items

            reportBuilder.append("--------------------------------------------------\n")
            reportBuilder.append("Receipt #${i + 1}: ${receipt.storeName} (ID: ${receipt.receiptId})\n\n")
            reportBuilder.append("Date: ${millisToLocalDate(receipt.date).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)}\n")
            reportBuilder.append("Receipt Total: €%.2f\n".format(receipt.total))
            reportBuilder.append("Items:\n")

            items.forEach { item ->
                reportBuilder.append("  - ${item.name}: €%.2f\n".format(item.price))
            }
            reportBuilder.append("\n")
            overallTotal += receipt.total
        }

        reportBuilder.append("--------------------------------------------------\n")
        reportBuilder.append("Total for all receipts: €%.2f\n".format(overallTotal))
        reportBuilder.append("--------------------------------------------------\n")

        return reportBuilder.toString()
    }

    fun generateStoreNameReport(storeName: String) : String {
        val filteredReceipts = receipts.value.filter { (receipt, _) -> receipt.storeName.equals(storeName, ignoreCase = true) }
        return generateReport(filteredReceipts)
    }

    fun generateDateReport(startDate: Long, endDate: Long) : String {
        val filteredReceipts = receipts.value.filter { (receipt, _) -> receipt.date in startDate..endDate }
        return generateReport(filteredReceipts)
    }
}