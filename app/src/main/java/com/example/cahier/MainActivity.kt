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
                            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                            val fullSheet = BitmapFactory.decodeStream(inputStream, null, options)
                            if (fullSheet != null) {
                                traceAndSaveAlphabet(fullSheet, glyphMap)
                            }
                        } catch (e: Exception) {
                            // Memory safety
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            }

            MaterialTheme {
                Scaffold { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        PaperView(text = noteText, glyphMap = glyphMap)

                        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)) {
                            if (isProcessing) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text("Tracing ink flow...", color = Color.DarkGray, modifier = Modifier.padding(top = 8.dp))
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
            val r = AndroidColor.red(p)
            val g = AndroidColor.green(p)
            val b = AndroidColor.blue(p)
            // SENSITIVITY FIX: Detects red ink even in shadows
            if (r > g + 20 && r > b + 20) { hasInk = true; break }
        }
        if (hasInk && !inStroke) { startX = x; inStroke = true }
        else if (!hasInk && inStroke) { 
            if (x - startX > 4) clusters.add(Rect(startX, 0, x, cell.height))
            inStroke = false 
        }
    }
    return clusters
}

fun renderInkOverlay(source: Bitmap, rect: Rect): Bitmap {
    val result = Bitmap.createBitmap(rect.width(), source.height, Bitmap.Config.ARGB_8888)
    for (x in 0 until rect.width()) {
        for (y in 0 until source.height) {
            val p = source.getPixel(rect.left + x, y)
            val r = AndroidColor.red(p)
            val g = AndroidColor.green(p)
            val b = AndroidColor.blue(p)

            // Tracing Logic: Capture the ink, delete the rest
            if (r > g + 15 && r > b + 15) {
                val intensity = (255 - r).coerceIn(100, 255)
                // Real Blue Pen color
                result.setPixel(x, y, AndroidColor.argb(255, 25, 55, 155))
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

        var curX = 145f
        var curY = 250f
        val lineSpacing = 90f
        val letterSize = 85f

        text.forEach { char ->
            val bmp = glyphMap[char]
            if (bmp != null) {
                val scale = letterSize / bmp.height
                val w = bmp.width * scale
                if (curX + w > size.width - 60f) { curX = 145f; curY += lineSpacing }

                // Adding human "jitter" and "bounce"
                val jitter = Random.nextFloat() * 4f - 2f
                val bounce = Random.nextFloat() * 6f - 3f

                rotate(degrees = jitter, pivot = Offset(curX, curY)) {
                    drawImage(
                        image = bmp.asImageBitmap(),
                        dstOffset = IntOffset(curX.toInt(), (curY - letterSize + 15 + bounce).toInt()),
                        dstSize = IntSize(w.toInt(), letterSize.toInt()),
                        blendMode = BlendMode.Multiply,
                        alpha = 0.92f
                    )
                }
                curX += w + 6f
            } else if (char == ' ') { curX += 45f }
            else if (char == '\n') { curX = 145f; curY += lineSpacing }
        }
    }
}

fun DrawScope.drawRealLifePaper() {
    val random = Random(88)
    // 1. Warm "Off-White" base
    drawRect(Color(0xFFFEF9F0))

    // 2. Realistic Lighting (Vignette)
    drawRect(
        brush = Brush.radialGradient(
            0.5f to Color.Transparent,
            1.0f to Color.Black.copy(0.05f),
            center = center,
            radius = size.maxDimension
        )
    )

    // 3. Paper Texture Fiber
    for (i in 0..1000) {
        drawCircle(
            color = Color.Black.copy(0.015f),
            radius = random.nextFloat() * 2f,
            center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
        )
    }

    // 4. Softened Lines
    val lineCol = Color(0xFF8BA9C4).copy(0.3f)
    for (i in 0..size.height.toInt() step 90) {
        val y = 250f + i
        drawLine(lineCol, Offset(0f, y), Offset(size.width, y), 2.5f)
    }
    drawLine(Color(0xFFEBBBBB).copy(0.5f), Offset(120f, 0f), Offset(120f, size.height), 3.5f)
}