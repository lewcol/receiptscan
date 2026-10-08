package com.github.lewcol.myapplication.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.lewcol.myapplication.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import kotlin.math.abs

sealed class OcrState {
    data object Idle : OcrState()
    data object Loading : OcrState()
    data class Success(val parsedData: Map<String, String>) : OcrState()
    data class Error(val message: String) : OcrState()
}

class OCRViewModel(private val receiptViewModel: ReceiptViewModel) : ViewModel() {
    private val _ocrState = MutableStateFlow<OcrState>(OcrState.Idle)
    val ocrState: StateFlow<OcrState> = _ocrState.asStateFlow()

    data class VisionLineData(
        val lineText: String,
        val elements: List<Text.Element>,
        val yCenter: Int
    )

    fun createTempFile(context: Context, extension: String = ".jpg"): File {
        val cacheDir = context.cacheDir
        val filename = "tmp_${System.currentTimeMillis()}$extension"
        return File(cacheDir, filename)
    }

    private fun readBrandLinesToSet(context: Context) : Set<String> {
        val inStream = context.resources.openRawResource(R.raw.brands)

        val b = ByteArray(inStream.available())
        inStream.read(b)

        val rawString = String(b)

        val strSet = mutableSetOf<String>()
        for (line in rawString.lines()) {
            strSet.add(line.lowercase())
        }

        return strSet.toSet()
    }

    private fun parseReceipt(context: Context, txt: Text) : MutableMap<String, String> {
        val brands = readBrandLinesToSet(context)
        val priceRegex = "^[\\$£€]?\\s*(\\d{1,3}(,\\d{3})*|\\d+)(\\.\\d{2})?$".toRegex()
        val priceExtractRegex = "^[\\$£€]?\\s*((\\d{1,3}(,\\d{3})*|\\d+)\\.\\d{2})$".toRegex()
        val strictNameRegex = "^[a-z 0-9:]+\$".toRegex(RegexOption.IGNORE_CASE)

        val dateTimeFormatterBuilder = DateTimeFormatterBuilder().append(DateTimeFormatter.ofPattern("[dd/MM/yyyy] + [dd-MM-yyyy] + [dd.MM.yyyy] + [dd/MM/yy] + [dd-MM-yy] + [dd.MM.yy]"))
        val dateTimeFormatter = dateTimeFormatterBuilder.toFormatter()

        val allLines = mutableListOf<VisionLineData>()
        val priceLines = mutableListOf<VisionLineData>()
        val itemLines = mutableListOf<VisionLineData>()

        // Sane defaults
        var title = ""
        var date = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val items = mutableMapOf<String, String>()
        var total = "0.00"

        // Get y coordinate of lines
        for (block in txt.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                box?.let { box ->
                    val yCenter = (box.top + box.bottom) / 2
                    allLines.add(VisionLineData(line.text, line.elements, yCenter))
                }
            }
        }

        // Distinguish if line is a price or other
        for (line in allLines) {
            if (priceRegex.matches(line.lineText.trim())) {
                priceLines.add(line)
            } else {
                itemLines.add(line)
            }
        }

        // Parse store name, check if a recognisable name is found
        title = if (itemLines.isNotEmpty()) itemLines[0].lineText else ""
        for (line in itemLines) {
            if (line.lineText in brands) title = line.lineText
        }

        // Parse date if found
        for (line in itemLines) {
            try {
                val receiptDate = LocalDate.parse(line.lineText, dateTimeFormatter)
                date = receiptDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                continue
            } catch (e: DateTimeParseException) {
                // This exception is expected if the text isn't a date
            }
        }

        // Parse items
        for (priceLine in priceLines) {
            // Determine closest line vertically to price - should be item name
            var bestMatch: VisionLineData? = null
            var minVDist = Int.MAX_VALUE

            for (itemLine in itemLines) {
                val vDist = abs(priceLine.yCenter - itemLine.yCenter)
                if (vDist < 50 && vDist < minVDist) {
                    minVDist = vDist
                    bestMatch = itemLine
                }
            }

            // If a match was found
            if (bestMatch != null) {
                val itemName = bestMatch.lineText.trim()
                val price = priceLine.lineText.trim()

                // Capture unmessy price
                val priceMatch = priceExtractRegex.find(price)
                if (priceMatch != null) {
                    val exactPrice = priceMatch.groups[1]?.value
                    exactPrice?.let { exactPrice ->
                        // Filter messy name values
                        val strictName = itemName.matches(strictNameRegex)

                        // If the item name is "Total" record this as total
                        if (strictName) {
                            if (itemName.lowercase().contains("total")) total = exactPrice
                            else items[itemName] = exactPrice
                        }
                    }
                }
                // Remove item from future processing
                itemLines.remove(bestMatch)
            }
        }

        // Basic error checking on prices
        items.filter { (_, v) -> v != total }

        // Collect in map
        items["_title"] = title
        items["_date"] = date.toString()
        items["_total"] = total

        return items
    }

    fun processImage(bitmap: Bitmap, context: Context, recognizer: TextRecognizer) {
        if (_ocrState.value is OcrState.Loading) return
        _ocrState.value = OcrState.Loading

        viewModelScope.launch {
            val image = InputImage.fromBitmap(bitmap, 0)

            try {
                val visionText = recognizer.process(image).await()
                val parsedText = parseReceipt(context, visionText)

                parsedText["_id"] = receiptViewModel.loadParsedReceipt(parsedText.toMap()).toString()

                _ocrState.value = OcrState.Success(parsedText)
            } catch (e: Exception) {
                Log.e("OCRViewModel", "OCR Error: ", e)
                _ocrState.value = OcrState.Error(e.message ?: "Unknown OCR Error")
            }
        }
    }
}