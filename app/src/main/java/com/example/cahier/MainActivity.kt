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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var noteText by remember { mutableStateOf("") }
            var showTextDialog by remember { mutableStateOf(false) }
            var isProcessing by remember { mutableStateOf(false) }
            var progress by remember { mutableStateOf(0f) }
            val glyphMap = remember { mutableStateMapOf<Char, Bitmap>() }
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            val sheetLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val fullSheet = BitmapFactory.decodeStream(inputStream)
                    if (fullSheet != null) {
                        isProcessing = true
                        scope.launch {
                            // NEW: We are now extracting "Traced" strokes
                            processInkOverlay(fullSheet, glyphMap) { progress = it }
                            isProcessing = false
                        }
                    }
                }
            }

            MaterialTheme {
                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showTextDialog = true }) { Text("EDIT TEXT") }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        // CLEAN DIGITAL PAPER (No shadows/noise)
                        PaperView(text = noteText, glyphMap = glyphMap)
                        
                        if (isProcessing) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.6f)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(progress = progress, color = Color.Cyan)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Tracing Pen Strokes: ${(progress * 100).toInt()}%", color = Color.White)
                                }
                            }
                        }

                        if (showTextDialog) {
                            HandwritingInputDialog(noteText, { showTextDialog = false }, { noteText = it; showTextDialog = false })
                        }
                        
                        Button(onClick = { sheetLauncher.launch("image/*") }, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                            Text("SCAN RED INK")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Traces the RED pixels and creates a BLUE stroke overlay
 */
suspend fun processInkOverlay(sheet: Bitmap, glyphMap: MutableMap<Char, Bitmap>, onProgress: (Float) -> Unit) = withContext(Dispatchers.Default) {
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    val cellW = sheet.width / 10
    val cellH = sheet.height / 3

    for (i in 0 until alphabet.length) {
        val r = i / 10
        val c = i % 10
        val cell = Bitmap.createBitmap(sheet, c * cellW, r * cellH, cellW, cellH)
        
        val clusters = findInkClusters(cell)
        
        if (clusters.size >= 2) {
            glyphMap[alphabet[i].uppercaseChar()] = extractInkOnly(cell, clusters[0])
            glyphMap[alphabet[i].lowercaseChar()] = extractInkOnly(cell, clusters[1])
        } else if (clusters.isNotEmpty()) {
            val bmp = extractInkOnly(cell, clusters[0])
            glyphMap[alphabet[i].lowercaseChar()] = bmp
            glyphMap[alphabet[i].uppercaseChar()] = bmp
        }
        withContext(Dispatchers.Main) { onProgress((i + 1).toFloat() / alphabet.length) }
    }
}

fun findInkClusters(cell: Bitmap): List<Rect> {
    val clusters = mutableListOf<Rect>(); var inInk = false; var startX = 0
    for (x in 0 until cell.width) {
        var hasInk = false
        for (y in 0 until cell.height) {
            val p = cell.getPixel(x, y)
            // Detection: Focus on Red Pen strokes
            if (AndroidColor.red(p) > 120 && AndroidColor.green(p) < 140 && AndroidColor.blue(p) < 140) {
                hasInk = true; break
            }
        }
        if (hasInk && !inInk) { startX = x; inInk = true }
        else if (!hasInk && inInk) { if (x - startX > 5) clusters.add(Rect(startX, 0, x, cell.height)); inInk = false }
    }
    return clusters
}

/**
 * PURGE BACKGROUND: This keeps ONLY the ink and turns it BLUE
 */
fun extractInkOnly(source: Bitmap, rect: Rect): Bitmap {
    // 1. Determine tight vertical crop
    var minY = source.height; var maxY = 0
    for (x in rect.left until rect.right) {
        for (y in 0 until source.height) {
            val p = source.getPixel(x, y)
            if (AndroidColor.red(p) > 120 && AndroidColor.green(p) < 140) {
                if (y < minY) minY = y; if (y > maxY) maxY = y
            }
        }
    }
    val cropH = (maxY - minY).coerceAtLeast(10)
    val cropped = Bitmap.createBitmap(source, rect.left, minY, rect.width(), cropH)
    val result = cropped.copy(Bitmap.Config.ARGB_8888, true)
    
    // 2. Trace and Recolor
    for (y in 0 until result.height) {
        for (x in 0 until result.width) {
            val p = result.getPixel(x, y)
            val r = AndroidColor.red(p)
            val g = AndroidColor.green(p)
            val b = AndroidColor.blue(p)

            // IF it's likely the paper (bright/gray), make it 100% transparent
            if (r > 150 && g > 150 && b > 150) {
                result.setPixel(x, y, AndroidColor.TRANSPARENT)
            } 
            // IF it's our Red ink stroke
            else if (r > 90 && r > g + 20) {
                // Change to a clean Royal Blue pen color
                // We keep some original darkness for "pressure" effect
                val intensity = (255 - r).coerceIn(0, 255)
                val blueStroke = AndroidColor.argb(255, 20, 40, (intensity + 100).coerceAtMost(255))
                result.setPixel(x, y, blueStroke)
            } else {
                result.setPixel(x, y, AndroidColor.TRANSPARENT)
            }
        }
    }
    return result
}

@Composable
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    // Pure clean paper background
    Canvas(modifier = Modifier.fillMaxSize().background(Color.White)) {
        drawCleanNotebookLines()

        var curX = 140f
        var curY = 240f 
        val lineSpacing = 85f
        val letterSize = 80f 

        text.forEach { char ->
            val bmp = glyphMap[char]
            if (bmp != null) {
                val scale = letterSize / bmp.height
                val w = bmp.width * scale
                
                if (curX + w > size.width - 50f) { curX = 140f; curY += lineSpacing }

                // The extracted blue "overlay" sits on the clean digital paper
                drawImage(
                    image = bmp.asImageBitmap(),
                    dstOffset = IntOffset(curX.toInt(), (curY - letterSize + 10).toInt()),
                    dstSize = IntSize(w.toInt(), letterSize.toInt()),
                    filterQuality = FilterQuality.High // Keeps the strokes sharp
                )
                curX += w + 6f
            } else if (char == ' ') {
                curX += 40f
            } else if (char == '\n') {
                curX = 140f; curY += lineSpacing
            }
        }
    }
}

fun DrawScope.drawCleanNotebookLines() {
    val blueLine = Color(0xFFC0D6E4)
    for (i in 0..size.height.toInt() step 85) {
        val y = 240f + i
        drawLine(blueLine, Offset(0f, y), Offset(size.width, y), 2f)
    }
    // Margin line
    drawLine(Color(0xFFFFD1D1), Offset(120f, 0f), Offset(120f, size.height), 3f)
}

@Composable
fun HandwritingInputDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, 
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Apply Tracing") } },
        text = { TextField(text, { text = it }, label = { Text("Enter Text") }) })
}