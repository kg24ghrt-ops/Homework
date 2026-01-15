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
import androidx.compose.ui.geometry.Size // FIXED: Added this import
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
                            processHandwritingSheet(fullSheet, glyphMap) { progress = it }
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
                                    Text("Extracting Ink: ${(progress * 100).toInt()}%", color = Color.White)
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

suspend fun processHandwritingSheet(sheet: Bitmap, glyphMap: MutableMap<Char, Bitmap>, onProgress: (Float) -> Unit) = withContext(Dispatchers.Default) {
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    val rows = 3; val cols = 10
    val cellW = sheet.width / cols; val cellH = sheet.height / rows

    for (i in 0 until alphabet.length) {
        val r = i / cols; val c = i % cols
        val cell = Bitmap.createBitmap(sheet, c * cellW, r * cellH, cellW, cellH)
        val clusters = findInkClusters(cell)
        
        if (clusters.size >= 2) {
            glyphMap[alphabet[i].uppercaseChar()] = cleanAndCrop(cell, clusters[0])
            glyphMap[alphabet[i].lowercaseChar()] = cleanAndCrop(cell, clusters[1])
        } else if (clusters.isNotEmpty()) {
            val bmp = cleanAndCrop(cell, clusters[0])
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
            if (AndroidColor.red(p) > 100 && AndroidColor.green(p) < 140 && AndroidColor.blue(p) < 140) {
                hasInk = true; break
            }
        }
        if (hasInk && !inInk) { startX = x; inInk = true }
        else if (!hasInk && inInk) { if (x - startX > 8) clusters.add(Rect(startX, 0, x, cell.height)); inInk = false }
    }
    return clusters
}

fun cleanAndCrop(source: Bitmap, rect: Rect): Bitmap {
    var minY = source.height; var maxY = 0
    for (x in rect.left until rect.right) {
        for (y in 0 until source.height) {
            val p = source.getPixel(x, y)
            if (AndroidColor.red(p) > 100 && AndroidColor.green(p) < 140) {
                if (y < minY) minY = y; if (y > maxY) maxY = y
            }
        }
    }
    val cropH = (maxY - minY).coerceAtLeast(10)
    val cropped = Bitmap.createBitmap(source, rect.left, minY, rect.width(), cropH)
    val result = cropped.copy(Bitmap.Config.ARGB_8888, true)
    for (y in 0 until result.height) {
        for (x in 0 until result.width) {
            val p = result.getPixel(x, y)
            if (AndroidColor.red(p) > 165 && AndroidColor.green(p) > 165) result.setPixel(x, y, AndroidColor.TRANSPARENT)
        }
    }
    return result
}

@Composable
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    val paintFallback = android.graphics.Paint().apply {
        color = AndroidColor.LTGRAY; textSize = 65f; typeface = android.graphics.Typeface.SERIF
    }

    Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F7F0))) {
        drawNotebookLines()
        drawPaperRealisticEffects() 

        var curX = 140f
        var curY = 225f 
        val lineSpacing = 85f
        val letterHeight = 78f // Large, readable scale

        text.forEach { char ->
            val bmp = glyphMap[char]
            if (bmp != null) {
                val scale = letterHeight / bmp.height
                val w = bmp.width * scale
                if (curX + w > size.width - 50f) { curX = 140f; curY += lineSpacing }

                rotate(degrees = Random.nextFloat() * 4f - 2f, pivot = Offset(curX, curY)) {
                    drawImage(
                        image = bmp.asImageBitmap(),
                        dstOffset = IntOffset(curX.toInt(), (curY - letterHeight).toInt()),
                        dstSize = IntSize(w.toInt(), letterHeight.toInt()),
                        blendMode = BlendMode.Multiply,
                        alpha = 0.94f
                    )
                }
                curX += w + 8f
            } else {
                when (char) {
                    ' ' -> curX += 40f
                    '\n' -> { curX = 140f; curY += lineSpacing }
                    else -> {
                        drawContext.canvas.nativeCanvas.drawText(char.toString(), curX, curY, paintFallback)
                        curX += paintFallback.measureText(char.toString()) + 5f
                    }
                }
            }
        }
    }
}

fun DrawScope.drawNotebookLines() {
    val blueLine = Color(0xFF8DA9C4).copy(0.3f)
    for (i in 0..size.height.toInt() step 85) {
        val y = 225f + i
        drawLine(blueLine, Offset(0f, y), Offset(size.width, y), 2.5f)
    }
    drawLine(Color(0xFFEEB4B4).copy(0.5f), Offset(120f, 0f), Offset(120f, size.height), 4f)
}

fun DrawScope.drawPaperRealisticEffects() {
    val random = Random(42)
    // 1. Soft Vertical "Waves" (Wrinkles)
    for (i in 0..6) {
        val startY = random.nextFloat() * size.height
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to Color.Black.copy(0.02f),
                1f to Color.Transparent
            ),
            topLeft = Offset(0f, startY),
            size = Size(size.width, 250f)
        )
    }
    // 2. Paper Texture Noise
    for (i in 0..300) {
        drawCircle(
            Color.Black.copy(0.015f), 
            random.nextFloat() * 2.5f, 
            Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
        )
    }
}

@Composable
fun HandwritingInputDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, 
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Write") } },
        text = { TextField(text, { text = it }, label = { Text("Note content") }) })
}