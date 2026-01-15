package com.example.cahier

import android.content.ContentValues
import android.graphics.*
import android.graphics.Canvas as AndroidCanvas
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
const val MAX_Y = PAGE_HEIGHT - 150f // Leave bottom margin

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
                    }) { Text("Export All Pages") }
                }
                Box(Modifier.weight(1f)) { HandwritingPage(typedText, glyphMap, cursorVisible) }
            }
        }
    }
}

/**
 * FEATURE: MULTI-PAGE EXPORT
 * Splits text into chunks that fit on individual bitmaps.
 */
suspend fun generatePages(text: String, glyphMap: Map<String, Glyph>): List<Bitmap> = withContext(Dispatchers.Default) {
    val pages = mutableListOf<Bitmap>()
    val tokens = tokenizeMyanmar(text)
    var remainingTokens = tokens

    while (remainingTokens.isNotEmpty()) {
        val bitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(AndroidColor.rgb(255, 252, 242)) // Paper tint
        
        // Render and find out how many tokens were actually consumed
        val (consumedCount, _) = renderTextChunk(canvas, remainingTokens, glyphMap, PAGE_WIDTH.toFloat(), isExport = true)
        
        pages.add(bitmap)
        remainingTokens = remainingTokens.drop(consumedCount)
        if (consumedCount == 0) break // Prevent infinite loop on single giant token
    }
    pages
}

/**
 * FEATURE: LINE SQUEEZE & INK PRESSURE
 * Returns (tokensConsumed, finalLayout)
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
    val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }

    for (token in tokens) {
        val glyph = glyphMap[token]
        if (glyph != null) {
            // 1. RANDOM INK VARIATION (mimics pressure)
            val pressure = Random.nextInt(-15, 15)
            val colorFilter = PorterDuffColorFilter(
                AndroidColor.rgb(35 + pressure, 65 + pressure, 160 + pressure), 
                PorterDuff.Mode.SRC_ATOP
            )
            paint.colorFilter = colorFilter

            // 2. LINE SQUEEZE LOGIC
            var scaleAdjust = 1.0f
            val baseScale = FONT_SIZE / glyph.bitmap.height
            val estimatedW = glyph.bitmap.width * baseScale
            
            // If the token is too wide for the line, squeeze it slightly
            if (xPos + estimatedW > canvasWidth - 50f) {
                if (xPos > START_X + 200f) { // If not at start of line, wrap
                    xPos = START_X
                    yPos += LINE_SPACING
                } else { // If already at start, squeeze to fit margin
                    scaleAdjust = 0.85f 
                }
            }

            // Page Break Check
            if (yPos > MAX_Y && isExport) break

            val randomRot = Random.nextFloat() * 3f - 1.5f
            val jitterY = Random.nextFloat() * 4f - 2f
            val finalScale = baseScale * scaleAdjust
            val w = glyph.bitmap.width * finalScale
            val top = (yPos + jitterY) - (glyph.bitmap.height * finalScale) + (glyph.baselineOffset * finalScale)

            val matrix = Matrix().apply {
                postScale(finalScale, finalScale)
                postRotate(randomRot, w / 2, FONT_SIZE / 2)
                postTranslate(xPos, top)
            }

            canvas.drawBitmap(glyph.bitmap, matrix, paint)
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

// --- Support Functions (Tokenize, Scan, Save) remain robust ---

fun tokenizeMyanmar(text: String): List<String> {
    // Regex for Myanmar Grapheme Clusters
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