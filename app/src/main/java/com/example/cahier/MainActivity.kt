package com.example.cahier

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint as AndroidPaint // Aliased to avoid conflict
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import android.graphics.Color as AndroidColor
import kotlin.random.Random

// --- Pro Parameters ---
const val START_X = 110f
const val START_Y = 280f
const val LINE_SPACING = 140f
const val FONT_SIZE = 95f
const val PAGE_WIDTH = 1080
const val PAGE_HEIGHT = 1920
const val MAX_Y = PAGE_HEIGHT - 150f 

data class Glyph(val bitmap: Bitmap, val baselineOffset: Float)
data class LayoutResult(val x: Float, val y: Float, val currentFontSize: Float)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            var typedText by remember { mutableStateOf("") }
            val glyphMap = remember { mutableStateMapOf<String, Glyph>() }
            var cursorVisible by remember { mutableStateOf(true) }

            val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let {
                    scope.launch {
                        try {
                            val stream = context.contentResolver.openInputStream(it)
                            val sheet = BitmapFactory.decodeStream(stream)
                            if (sheet != null) scanSheetToMap(sheet, glyphMap)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            LaunchedEffect(Unit) { while(true) { delay(500); cursorVisible = !cursorVisible } }

            Column(Modifier.fillMaxSize().padding(16.dp)) {
                TextField(value = typedText, onValueChange = { typedText = it }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.padding(vertical = 8.dp)) {
                    Button(onClick = { pickerLauncher.launch("image/*") }) { Text("Scan") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        scope.launch {
                            val pages = generatePages(typedText, glyphMap)
                            pages.forEachIndexed { index, page ->
                                saveToGallery(context, page, "Page_${index + 1}")
                            }
                            Toast.makeText(context, "Saved ${pages.size} pages!", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Export All") }
                }
                Box(Modifier.weight(1f)) { HandwritingPage(typedText, glyphMap, cursorVisible) }
            }
        }
    }
}

suspend fun generatePages(text: String, glyphMap: Map<String, Glyph>): List<Bitmap> = withContext(Dispatchers.Default) {
    val pages = mutableListOf<Bitmap>()
    val tokens = tokenizeMyanmar(text)
    var remainingTokens = tokens

    while (remainingTokens.isNotEmpty()) {
        val bitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(AndroidColor.rgb(255, 252, 242)) 
        
        val (consumedCount, _) = renderTextChunk(canvas, remainingTokens, glyphMap, PAGE_WIDTH.toFloat(), isExport = true)
        
        pages.add(bitmap)
        remainingTokens = remainingTokens.drop(consumedCount)
        if (consumedCount == 0) break 
    }
    pages
}

/**
 * FIXED: Explicitly using android.graphics.Paint and Matrix
 */
fun renderTextChunk(
    canvas: AndroidCanvas,
    tokens: List<String>,
    glyphMap: Map<String, Glyph>,
    canvasWidth: Float,
    isExport: Boolean
): Pair<Int, LayoutResult> {
    var xPos = START_X
    var yPos = START_Y
    var tokensConsumed = 0
    val nativePaint = AndroidPaint().apply { 
        isAntiAlias = true
        isFilterBitmap = true 
    }

    for (token in tokens) {
        val glyph = glyphMap[token]
        if (glyph != null) {
            // Ink variation
            val p = Random.nextInt(-12, 12)
            nativePaint.colorFilter = PorterDuffColorFilter(
                AndroidColor.rgb(35 + p, 65 + p, 160 + p), 
                PorterDuff.Mode.SRC_ATOP
            )

            val baseScale = FONT_SIZE / glyph.bitmap.height
            var scaleAdjust = 1.0f
            val estimatedW = glyph.bitmap.width * baseScale
            
            if (xPos + estimatedW > canvasWidth - 50f) {
                if (xPos > START_X + 200f) {
                    xPos = START_X
                    yPos += LINE_SPACING
                } else {
                    scaleAdjust = 0.82f 
                }
            }

            if (yPos > MAX_Y && isExport) break

            val finalScale = baseScale * scaleAdjust
            val w = glyph.bitmap.width * finalScale
            val top = (yPos + (Random.nextFloat() * 4f - 2f)) - (glyph.bitmap.height * finalScale) + (glyph.baselineOffset * finalScale)

            val matrix = Matrix()
            matrix.postScale(finalScale, finalScale)
            matrix.postRotate(Random.nextFloat() * 3f - 1.5f, w / 2, FONT_SIZE / 2)
            matrix.postTranslate(xPos, top)

            canvas.drawBitmap(glyph.bitmap, matrix, nativePaint)
            xPos += w + 2f
        } else if (token == " ") {
            xPos += 45f
        } else if (token == "\n") {
            xPos = START_X
            yPos += LINE_SPACING
        }
        tokensConsumed++
    }
    return tokensConsumed to LayoutResult(xPos, yPos, FONT_SIZE)
}

@Composable
fun HandwritingPage(text: String, glyphMap: Map<String, Glyph>, cursorVisible: Boolean) {
    var caretPos by remember { mutableStateOf(LayoutResult(START_X, START_Y, FONT_SIZE)) }
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawIntoCanvas {
            val (_, result) = renderTextChunk(it.nativeCanvas, tokenizeMyanmar(text), glyphMap, size.width, false)
            caretPos = result
        }
        if (cursorVisible) {
            drawLine(Color(0xFF0A84FF), Offset(caretPos.x + 2f, caretPos.y - 70f), Offset(caretPos.x + 2f, caretPos.y), 3.dp.toPx())
        }
    }
}

fun tokenizeMyanmar(text: String): List<String> {
    val regex = Regex("[\u1000-\u1021](?:\u1039[\u1000-\u1021])?[\u102B-\u103E]*|.")
    return regex.findAll(text).map { it.value }.toList()
}



suspend fun scanSheetToMap(sheet: Bitmap, map: MutableMap<String, Glyph>) = withContext(Dispatchers.Default) {
    val alphabet = listOf("က", "ခ", "ဂ", "င", "စ", "ဆ", "ဇ", "ဈ", "ည", "ဋ", "ဌ", "ဍ", "ဎ", "ဏ", "တ", "ထ", "ဒ", "ဓ", "န", "ပ") 
    val cellW = sheet.width / 5
    val cellH = sheet.height / 4
    alphabet.forEachIndexed { i, char ->
        val x = (i % 5) * cellW
        val y = (i / 5) * cellH
        val cell = Bitmap.createBitmap(sheet, x, y, minOf(cellW, sheet.width - x), minOf(cellH, sheet.height - y))
        val pixels = IntArray(cell.width * cell.height)
        cell.getPixels(pixels, 0, cell.width, 0, 0, cell.width, cell.height)
        var lowestY = 0
        var found = false
        for (idx in pixels.indices) {
            val p = pixels[idx]
            if ((AndroidColor.red(p) + AndroidColor.green(p) + AndroidColor.blue(p)) / 3 < 160) {
                pixels[idx] = AndroidColor.argb(255, 40, 40, 40)
                lowestY = maxOf(lowestY, idx / cell.width)
                found = true
            } else pixels[idx] = AndroidColor.TRANSPARENT
        }
        if (found) {
            val out = Bitmap.createBitmap(cell.width, cell.height, Bitmap.Config.ARGB_8888)
            out.setPixels(pixels, 0, cell.width, 0, 0, cell.width, cell.height)
            map[char] = Glyph(out, (cell.height - lowestY).toFloat())
        }
    }
}

suspend fun saveToGallery(context: android.content.Context, bmp: Bitmap, name: String) = withContext(Dispatchers.IO) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$name-${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let { context.contentResolver.openOutputStream(it)?.use { s -> bmp.compress(Bitmap.CompressFormat.PNG, 100, s) } }
}