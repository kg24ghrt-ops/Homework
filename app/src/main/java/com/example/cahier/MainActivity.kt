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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
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
                            processWithPixelScan(fullSheet, glyphMap) { progress = it }
                            isProcessing = false
                        }
                    }
                }
            }

            MaterialTheme {
                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showTextDialog = true }) { Text("EDIT") }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        PaperView(text = noteText, glyphMap = glyphMap)

                        if (isProcessing) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(progress = progress, color = Color.White)
                                    Text("Processing: ${(progress * 100).toInt()}%", color = Color.White)
                                }
                            }
                        }

                        if (showTextDialog) {
                            HandwritingInputDialog(noteText, { showTextDialog = false }, { noteText = it; showTextDialog = false })
                        }
                        
                        Button(onClick = { sheetLauncher.launch("image/*") }, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                            Text("UPLOAD SHEET")
                        }
                    }
                }
            }
        }
    }
}

// ... [Keep processWithPixelScan, findInkClusters, and extractAndClean from previous version] ...
suspend fun processWithPixelScan(sheet: Bitmap, glyphMap: MutableMap<Char, Bitmap>, onProgress: (Float) -> Unit) = withContext(Dispatchers.Default) {
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    val rows = 3; val cols = 10
    val cellW = sheet.width / cols; val cellH = sheet.height / rows
    for (i in 0 until alphabet.length) {
        val r = i / cols; val c = i % cols
        val cell = Bitmap.createBitmap(sheet, c * cellW, r * cellH, cellW, cellH)
        val clusters = findInkClusters(cell)
        if (clusters.size >= 2) {
            glyphMap[alphabet[i].uppercaseChar()] = extractAndClean(cell, clusters[0])
            glyphMap[alphabet[i].lowercaseChar()] = extractAndClean(cell, clusters[1])
        } else if (clusters.isNotEmpty()) {
            val bmp = extractAndClean(cell, clusters[0])
            glyphMap[alphabet[i].lowercaseChar()] = bmp; glyphMap[alphabet[i].uppercaseChar()] = bmp
        }
        withContext(Dispatchers.Main) { onProgress((i + 1).toFloat() / alphabet.length) }
    }
}

fun findInkClusters(cell: Bitmap): List<Rect> {
    val clusters = mutableListOf<Rect>(); var inInk = false; var startX = 0
    for (x in 0 until cell.width) {
        var columnHasInk = false
        for (y in 0 until cell.height) {
            val p = cell.getPixel(x, y)
            if (AndroidColor.red(p) < 185 && AndroidColor.green(p) < 170) { columnHasInk = true; break }
        }
        if (columnHasInk && !inInk) { startX = x; inInk = true }
        else if (!columnHasInk && inInk) { if (x - startX > 4) clusters.add(Rect(startX, 0, x, cell.height)); inInk = false }
    }
    return clusters
}

fun extractAndClean(source: Bitmap, rect: Rect): Bitmap {
    val cropped = Bitmap.createBitmap(source, rect.left, 0, rect.width(), source.height)
    val result = cropped.copy(Bitmap.Config.ARGB_8888, true)
    for (y in 0 until result.height) {
        for (x in 0 until result.width) {
            val p = result.getPixel(x, y)
            if (AndroidColor.red(p) > 175 && AndroidColor.green(p) > 175) result.setPixel(x, y, AndroidColor.TRANSPARENT)
        }
    }
    return result
}

@Composable
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    val paint = android.graphics.Paint().apply {
        color = AndroidColor.GRAY
        textSize = 55f // INCREASED SIZE
        typeface = android.graphics.Typeface.SERIF
        alpha = 140
    }

    Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F7F2))) {
        drawNotebookLines()
        drawRealisticNoise() // NEW: Adds wrinkles and paper grain

        var curX = 135f
        var curY = 160f // Adjusted start height
        val lineSpacing = 65f
        val letterHeight = 60f // INCREASED HEIGHT to match lines

        text.forEach { char ->
            val bmp = glyphMap[char]
            if (bmp != null) {
                val scale = letterHeight / bmp.height
                val w = bmp.width * scale
                
                if (curX + w > size.width - 40f) { curX = 135f; curY += lineSpacing }

                // Added small random rotation for "natural" look
                drawContext.canvas.nativeCanvas.save()
                drawContext.canvas.nativeCanvas.rotate(Random.nextFloat() * 2f - 1f, curX, curY)
                
                drawImage(
                    bmp.asImageBitmap(),
                    IntOffset(curX.toInt(), curY.toInt()),
                    IntSize(w.toInt(), letterHeight.toInt()),
                    blendMode = BlendMode.Multiply,
                    alpha = 0.92f
                )
                drawContext.canvas.nativeCanvas.restore()
                curX += w + 2f
            } else {
                if (char == ' ') curX += 40f
                else if (char == '\n') { curX = 135f; curY += lineSpacing }
                else {
                    drawContext.canvas.nativeCanvas.drawText(char.toString(), curX, curY + 50f, paint)
                    curX += paint.measureText(char.toString()) + 5f
                }
            }
        }
    }
}

fun DrawScope.drawNotebookLines() {
    val blueLines = Color(0xFFADCEEB).copy(0.35f)
    for (i in 0..size.height.toInt() step 65) {
        drawLine(blueLines, Offset(0f, 220f + i), Offset(size.width, 220f + i), 2f)
    }
    drawLine(Color(0xFFFFB3B3).copy(0.5f), Offset(115f, 0f), Offset(115f, size.height), 3f)
}

fun DrawScope.drawRealisticNoise() {
    val random = Random(System.currentTimeMillis())
    // Large "Wrinkles"
    for (i in 0..5) {
        val startX = random.nextFloat() * size.width
        val startY = random.nextFloat() * size.height
        drawLine(
            color = Color.Black.copy(alpha = 0.03f),
            start = Offset(startX, startY),
            end = Offset(startX + 600f, startY + 200f),
            strokeWidth = 80f
        )
    }
    // Fine Paper Grain/Noise
    for (i in 0..50) {
        drawCircle(
            color = Color.Black.copy(alpha = 0.02f),
            radius = random.nextFloat() * 5f,
            center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
        )
    }
}

@Composable
fun HandwritingInputDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, 
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Write") } },
        text = { TextField(text, { text = it }, label = { Text("Enter Text") }) })
}