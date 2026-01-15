package com.example.cahier

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Environment
import android.provider.MediaStore
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Color as AndroidColor

// Data class to store the bitmap and its distance from the bottom to the ink baseline
data class Glyph(val bitmap: Bitmap, val baselineOffset: Float)
// Tracking object for the caret/cursor
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

            // 1. Scan Sheet Pipeline
            val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let {
                    scope.launch {
                        val stream = context.contentResolver.openInputStream(it)
                        val sheet = BitmapFactory.decodeStream(stream)
                        if (sheet != null) scanSheetToMap(sheet, glyphMap)
                    }
                }
            }

            // Caret Blink Effect
            LaunchedEffect(Unit) { while(true) { delay(500); cursorVisible = !cursorVisible } }

            Column(Modifier.fillMaxSize().padding(16.dp)) {
                TextField(
                    value = typedText, 
                    onValueChange = { typedText = it }, 
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Type Myanmar text here...") }
                )
                Row(Modifier.padding(vertical = 8.dp)) {
                    Button(onClick = { pickerLauncher.launch("image/*") }) { Text("Scan Sheet") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        scope.launch {
                            val exportBmp = renderToBitmap(typedText, glyphMap, 1080, 1920)
                            saveToGallery(context, exportBmp)
                        }
                    }) { Text("Save Gallery") }
                }
                Box(Modifier.weight(1f)) { 
                    HandwritingPage(typedText, glyphMap, cursorVisible) 
                }
            }
        }
    }
}

/**
 * 2. SCANNING ENGINE: Slices the image based on a 5x4 grid
 */
suspend fun scanSheetToMap(sheet: Bitmap, map: MutableMap<String, Glyph>) = withContext(Dispatchers.Default) {
    val alphabet = listOf("က", "ခ", "ဂ", "င", "စ", "ဆ", "ဇ", "ဈ", "ည", "ဋ", "ဌ", "ဍ", "ဎ", "ဏ", "တ", "ထ", "ဒ", "ဓ", "န", "ပ") 
    val cols = 5
    val cellW = sheet.width / cols
    val cellH = sheet.height / 4

    alphabet.forEachIndexed { i, char ->
        val x = (i % cols) * cellW
        val y = (i / cols) * cellH
        val cell = Bitmap.createBitmap(sheet, x, y, cellW, cellH)
        val glyph = extractGlyph(cell)
        if (glyph != null) map[char] = glyph
    }
}

/**
 * 3. INK EXTRACTION: Improved luminance check & loop index fix
 */
fun extractGlyph(source: Bitmap): Glyph? {
    val w = source.width
    val h = source.height
    val pixels = IntArray(w * h)
    source.getPixels(pixels, 0, w, 0, 0, w, h)
    
    var lowestInkY = 0
    var foundInk = false

    for (y in 0 until h) {
        for (x in 0 until w) {
            val idx = y * w + x
            val p = pixels[idx]
            val luminance = (AndroidColor.red(p) + AndroidColor.green(p) + AndroidColor.blue(p)) / 3
            
            // Detect ink (anything darker than light gray)
            if (luminance < 160) { 
                lowestInkY = maxOf(lowestInkY, y)
                foundInk = true
                pixels[idx] = AndroidColor.argb(255, 30, 60, 150) // Force clean pen-blue
            } else {
                pixels[idx] = AndroidColor.TRANSPARENT
            }
        }
    }
    if (!foundInk) return null
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    out.setPixels(pixels, 0, w, 0, 0, w, h)
    return Glyph(out, (h - lowestInkY).toFloat())
}

/**
 * 4. CENTRAL RENDERING: Shared by both Screen (UI) and Export (Bitmap)
 */
fun drawAllHandwriting(
    canvas: AndroidCanvas, 
    text: String, 
    glyphMap: Map<String, Glyph>, 
    canvasWidth: Float
): LayoutResult {
    val tokens = tokenizeMyanmar(text)
    var xPos = 100f
    var yPos = 250f
    val lineSpacing = 120f
    val fontSize = 90f
    val paint = Paint().apply { isAntiAlias = true }

    tokens.forEach { token ->
        val glyph = glyphMap[token]
        if (glyph != null) {
            val scale = fontSize / glyph.bitmap.height
            val w = glyph.bitmap.width * scale
            
            if (xPos + w > canvasWidth - 60f) { xPos = 100f; yPos += lineSpacing }

            // BASELINE ALIGNMENT MATH:
            // Normalize all characters to sit on yPos
            val top = yPos - (glyph.bitmap.height * scale) + (glyph.baselineOffset * scale)
            canvas.drawBitmap(glyph.bitmap, null, RectF(xPos, top, xPos + w, top + (glyph.bitmap.height * scale)), paint)
            xPos += w + 4f
        } else if (token == " ") {
            xPos += 50f 
        } else if (token == "\n") {
            xPos = 100f; yPos += lineSpacing
        }
    }
    return LayoutResult(xPos, yPos)
}

@Composable
fun HandwritingPage(text: String, glyphMap: Map<String, Glyph>, cursorVisible: Boolean) {
    var caretPos by remember { mutableStateOf(LayoutResult(100f, 250f)) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawIntoCanvas {
            caretPos = drawAllHandwriting(it.nativeCanvas, text, glyphMap, size.width)
        }
        
        // Render Cursor
        if (cursorVisible) {
            drawLine(
                color = Color(0xFF0A84FF),
                start = Offset(caretPos.x + 5f, caretPos.y - 75f),
                end = Offset(caretPos.x + 5f, caretPos.y + 5f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * 5. EXPORT PIPELINE
 */
fun renderToBitmap(text: String, glyphMap: Map<String, Glyph>, w: Int, h: Int): Bitmap {
    val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(b)
    c.drawColor(AndroidColor.rgb(255, 253, 245)) // Background color
    drawAllHandwriting(c, text, glyphMap, w.toFloat())
    return b
}

suspend fun saveToGallery(context: android.content.Context, bmp: Bitmap) = withContext(Dispatchers.IO) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "Homework_${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let { 
        context.contentResolver.openOutputStream(it)?.use { s -> 
            bmp.compress(Bitmap.CompressFormat.PNG, 100, s) 
        } 
    }
}

fun tokenizeMyanmar(text: String): List<String> {
    val regex = Regex("[\u1000-\u1021](?:\u1039[\u1000-\u1021])?[\u102B-\u103E]*|.")
    return regex.findAll(text).map { it.value }.toList()
}