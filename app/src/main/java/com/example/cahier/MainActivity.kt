package com.example.cahier

import android.content.ContentValues
import android.graphics.*
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
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

// --- Parameters ---
const val START_X = 110f
const val START_Y = 280f
const val LINE_SPACING = 140f
const val FONT_SIZE = 95f
const val PAGE_WIDTH = 1080
const val PAGE_HEIGHT = 1920

data class Glyph(val bitmap: Bitmap, val baselineOffset: Float)
data class LayoutResult(val x: Float, val y: Float)

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
                            if (sheet != null) {
                                // NEW: Best solution - Universal Auto-Detection
                                scanAnySheetUniversal(sheet, glyphMap)
                                Toast.makeText(context, "Handwriting Analyzed!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            LaunchedEffect(Unit) { while(true) { delay(500); cursorVisible = !cursorVisible } }

            Column(Modifier.fillMaxSize().padding(16.dp)) {
                TextField(value = typedText, onValueChange = { typedText = it }, 
                    modifier = Modifier.fillMaxWidth(), placeholder = { Text("Type English or Myanmar...") })
                
                Row(Modifier.padding(vertical = 8.dp)) {
                    Button(onClick = { pickerLauncher.launch("image/*") }) { Text("Scan Any Sheet") }
                }
                
                Box(Modifier.weight(1f)) { 
                    HandwritingPage(typedText, glyphMap, cursorVisible) 
                }
            }
        }
    }
}

/**
 * UNIVERSAL SCANNER: The Best Solution
 * Instead of a grid, this scans for "Ink Islands".
 * It works for English, Myanmar, or any symbols.
 */
suspend fun scanAnySheetUniversal(sheet: Bitmap, map: MutableMap<String, Glyph>) = withContext(Dispatchers.Default) {
    // This list defines the order the app expects the characters on your sheet
    val masterAlphabet = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "ကခဂငစဆဇဈညဋဌဍဎဏတထဒဓနပဖဗဘမယရလဝသဟဠအ").split("").filter { it.isNotEmpty() }
    
    val w = sheet.width
    val h = sheet.height
    val pixels = IntArray(w * h)
    sheet.getPixels(pixels, 0, w, 0, 0, w, h)

    var charIndex = 0
    var inInkZone = false
    var startX = 0

    // Horizontal Scanning (Sliding Window)
    for (x in 0 until w) {
        var colHasInk = false
        for (y in 0 until h) {
            val p = pixels[y * w + x]
            // Threshold: Detect dark or red ink, ignore white/blue lines
            if (AndroidColor.red(p) < 180 && (AndroidColor.red(p) + AndroidColor.green(p) + AndroidColor.blue(p)) / 3 < 190) {
                colHasInk = true
                break
            }
        }

        if (colHasInk && !inInkZone) {
            inInkZone = true
            startX = x
        } else if (!colHasInk && inInkZone) {
            inInkZone = false
            val charWidth = x - startX
            
            // Only capture if the width is significant (ignores noise)
            if (charWidth > 5 && charIndex < masterAlphabet.size) {
                val cell = Bitmap.createBitmap(sheet, startX, 0, charWidth, h)
                val glyph = processGlyphInk(cell)
                if (glyph != null) {
                    val label = masterAlphabet[charIndex]
                    map[label] = glyph
                    map[label.lowercase()] = glyph // Support both cases
                    charIndex++
                }
            }
        }
    }
}

/**
 * CLEANER: Crops the glyph to its exact ink height so letters don't "float".
 */
fun processGlyphInk(source: Bitmap): Glyph? {
    val w = source.width
    val h = source.height
    val pixels = IntArray(w * h)
    source.getPixels(pixels, 0, w, 0, 0, w, h)
    
    val result = IntArray(w * h)
    var topInk = h
    var bottomInk = 0
    var found = false

    for (y in 0 until h) {
        for (x in 0 until w) {
            val p = pixels[y * w + x]
            if (AndroidColor.red(p) < 180 && (AndroidColor.red(p) + AndroidColor.green(p) + AndroidColor.blue(p)) / 3 < 190) {
                result[y * w + x] = AndroidColor.rgb(30, 50, 140) // Convert to clean blue ink
                topInk = minOf(topInk, y)
                bottomInk = maxOf(bottomInk, y)
                found = true
            } else {
                result[y * w + x] = AndroidColor.TRANSPARENT
            }
        }
    }

    if (!found) return null
    
    val inkHeight = (bottomInk - topInk).coerceAtLeast(1)
    val out = Bitmap.createBitmap(w, inkHeight, Bitmap.Config.ARGB_8888)
    out.setPixels(result, topInk * w, w, 0, 0, w, inkHeight)
    
    return Glyph(out, (h - bottomInk).toFloat())
}

@Composable
fun HandwritingPage(text: String, glyphMap: Map<String, Glyph>, cursorVisible: Boolean) {
    var caretPos by remember { mutableStateOf(LayoutResult(START_X, START_Y)) }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawIntoCanvas {
            val paint = AndroidPaint().apply { isAntiAlias = true; isFilterBitmap = true }
            var x = START_X
            var y = START_Y
            
            val tokens = tokenizeHybrid(text)

            tokens.forEach { token ->
                val glyph = glyphMap[token]
                if (glyph != null) {
                    val scale = FONT_SIZE / glyph.bitmap.height
                    val w = glyph.bitmap.width * scale
                    
                    if (x + w > size.width - 50f) { x = START_X; y += LINE_SPACING }
                    
                    val top = y - (glyph.bitmap.height * scale) + (glyph.baselineOffset * scale)
                    it.nativeCanvas.drawBitmap(glyph.bitmap, null, RectF(x, top, x + w, top + (glyph.bitmap.height * scale)), paint)
                    x += w + 6f
                } else if (token == " ") {
                    x += 45f
                } else if (token == "\n") {
                    x = START_X; y += LINE_SPACING
                }
            }
            caretPos = LayoutResult(x, y)
        }
        
        if (cursorVisible) {
            drawLine(Color(0xFF0A84FF), Offset(caretPos.x + 2f, caretPos.y - 70f), Offset(caretPos.x + 2f, caretPos.y), 2.dp.toPx())
        }
    }
}

fun tokenizeHybrid(text: String): List<String> {
    // Regex: Myanmar grapheme clusters OR English letters OR single chars
    val regex = Regex("[\u1000-\u1021](?:\u1039[\u1000-\u1021])?[\u102B-\u103E]*|[a-zA-Z0-9]|.")
    return regex.findAll(text).map { it.value }.toList()
}
