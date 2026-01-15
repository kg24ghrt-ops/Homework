package com.example.cahier

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.Rect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var noteText by remember { mutableStateOf("") }
            var isProcessing by remember { mutableStateOf(false) }
            val glyphMap = remember { mutableStateMapOf<Char, Bitmap>() }
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            val sheetLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    isProcessing = true
                    scope.launch {
                        try {
                            val inputStream = context.contentResolver.openInputStream(it)
                            // Memory Guard: Scale down large high-res photos to prevent app death
                            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                            val fullSheet = BitmapFactory.decodeStream(inputStream, null, options)
                            
                            if (fullSheet != null) {
                                traceAndSaveAlphabet(fullSheet, glyphMap)
                            }
                        } catch (e: Exception) {
                            // Silently catch memory errors
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            }

            MaterialTheme {
                Scaffold { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        // Drawing the paper and the traced strokes
                        PaperView(text = noteText, glyphMap = glyphMap)

                        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)) {
                            if (isProcessing) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text("Tracing ink flow...", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                            } else {
                                TextField(
                                    value = noteText,
                                    onValueChange = { noteText = it },
                                    placeholder = { Text("Write something...") },
                                    modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.7f))
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(onClick = { sheetLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                                    Text("SCAN HANDWRITING")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * AUTOMATIC RECOGNITION: Maps uppercase and lowercase based on grid position.
 */
suspend fun traceAndSaveAlphabet(sheet: Bitmap, glyphMap: MutableMap<Char, Bitmap>) = withContext(Dispatchers.Default) {
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    val cellW = sheet.width / 10
    val cellH = sheet.height / 3

    for (i in 0 until alphabet.length) {
        val r = i / 10
        val c = i % 10
        val x = (c * cellW).coerceAtMost(sheet.width - cellW)
        val y = (r * cellH).coerceAtMost(sheet.height - cellH)
        
        val cell = Bitmap.createBitmap(sheet, x, y, cellW, cellH)
        val strokes = findStrokes(cell)
        
        if (strokes.size >= 2) {
            // Automatically assigns based on your sheet layout
            glyphMap[alphabet[i].uppercaseChar()] = renderInkOverlay(cell, strokes[0])
            glyphMap[alphabet[i].lowercaseChar()] = renderInkOverlay(cell, strokes[1])
        } else if (strokes.isNotEmpty()) {
            val bmp = renderInkOverlay(cell, strokes[0])
            glyphMap[alphabet[i].lowercaseChar()] = bmp
            glyphMap[alphabet[i].uppercaseChar()] = bmp
        }
    }
}

fun findStrokes(cell: Bitmap): List<Rect> {
    val clusters = mutableListOf<Rect>()
    var inStroke = false; var startX = 0
    for (x in 0 until cell.width) {
        var hasInk = false
        for (y in 0 until cell.height) {
            val p = cell.getPixel(x, y)
            // Detection: Specifically looking for your Red/Dark pen against paper
            if (AndroidColor.red(p) > 100 && AndroidColor.green(p) < 160) {
                hasInk = true; break
            }
        }
        if (hasInk && !inStroke) { startX = x; inStroke = true }
        else if (!hasInk && inStroke) { 
            if (x - startX > 4) clusters.add(Rect(startX, 0, x, cell.height))
            inStroke = false 
        }
    }
    return clusters
}

/**
 * INK BLEED RENDERER: Removes the photo background and makes it look like real blue ink.
 */
fun renderInkOverlay(source: Bitmap, rect: Rect): Bitmap {
    val result = Bitmap.createBitmap(rect.width(), source.height, Bitmap.Config.ARGB_8888)
    for (x in 0 until rect.width()) {
        for (y in 0 until source.height) {
            val p = source.getPixel(rect.left + x, y)
            val r = AndroidColor.red(p)
            val g = AndroidColor.green(p)
            
            // Extract only the stroke
            if (r > 90 && r > g + 15) {
                val alpha = (255 - r).coerceIn(120, 255)
                // Royal Blue Ink with transparency for "Bleed" effect
                result.setPixel(x, y, AndroidColor.argb(alpha, 10, 40, 150))
            } else {
                result.setPixel(x, y, AndroidColor.TRANSPARENT)
            }
        }
    }
    return result
}

@Composable
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRealLifePaper()

        var curX = 140f
        var curY = 240f
        val lineSpacing = 88f
        val letterSize = 80f

        text.forEach { char ->
            val bmp = glyphMap[char]
            if (bmp != null) {
                val scale = letterSize / bmp.height
                val w = bmp.width * scale
                if (curX + w > size.width - 50f) { curX = 140f; curY += lineSpacing }

                // Slight rotation jitter for realism
                rotate(degrees = Random.nextFloat() * 3f - 1.5f, pivot = Offset(curX, curY)) {
                    drawImage(
                        image = bmp.asImageBitmap(),
                        dstOffset = IntOffset(curX.toInt(), (curY - letterSize + 12).toInt()),
                        dstSize = IntSize(w.toInt(), letterSize.toInt()),
                        blendMode = BlendMode.Multiply // Essential: makes ink look "absorbed"
                    )
                }
                curX += w + 6f
            } else if (char == ' ') { curX += 45f }
            else if (char == '\n') { curX = 140f; curY += lineSpacing }
        }
    }
}

fun DrawScope.drawRealLifePaper() {
    val random = Random(77)
    // 1. Natural Paper Tone
    drawRect(Color(0xFFFCF9F2))

    // 2. Paper Grain & Texture
    for (i in 0..800) {
        drawCircle(
            color = Color.Black.copy(0.012f),
            radius = random.nextFloat() * 1.8f,
            center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
        )
    }

    // 3. Ink-Friendly Notebook Lines
    val lineCol = Color(0xFF7B95B1).copy(0.25f)
    for (i in 0..size.height.toInt() step 88) {
        val y = 240f + i
        drawLine(lineCol, Offset(0f, y), Offset(size.width, y), 2.5f)
    }
    // Margin Line
    drawLine(Color(0xFFD69494).copy(0.4f), Offset(120f, 0f), Offset(120f, size.height), 4f)
}