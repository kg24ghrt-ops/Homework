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
            val scope = rememberCoroutineScope() // FIXED: Use scope for background tasks

            val sheetLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val fullSheet = BitmapFactory.decodeStream(inputStream)
                    if (fullSheet != null) {
                        isProcessing = true
                        // FIXED: Launching the pixel-scan correctly
                        scope.launch {
                            processWithPixelScan(fullSheet, glyphMap) { currentProg ->
                                progress = currentProg
                            }
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
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(progress = progress, color = Color.White)
                                    Text("Scanning handwriting: ${(progress * 100).toInt()}%", color = Color.White)
                                }
                            }
                        }

                        if (showTextDialog) {
                            HandwritingInputDialog(noteText, { showTextDialog = false }, { noteText = it; showTextDialog = false })
                        }
                        
                        Button(
                            onClick = { sheetLauncher.launch("image/*") },
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                        ) { Text("UPLOAD SHEET") }
                    }
                }
            }
        }
    }
}

suspend fun processWithPixelScan(
    sheet: Bitmap, 
    glyphMap: MutableMap<Char, Bitmap>, 
    onProgress: (Float) -> Unit
) = withContext(Dispatchers.Default) {
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    val rows = 3
    val cols = 10
    val cellW = sheet.width / cols
    val cellH = sheet.height / rows

    for (i in 0 until alphabet.length) {
        val r = i / cols
        val c = i % cols
        val cell = Bitmap.createBitmap(sheet, c * cellW, r * cellH, cellW, cellH)
        
        // Scan for ink clusters to separate "A" from "a"
        val clusters = findInkClusters(cell)
        
        if (clusters.size >= 2) {
            glyphMap[alphabet[i].uppercaseChar()] = extractAndClean(cell, clusters[0])
            glyphMap[alphabet[i].lowercaseChar()] = extractAndClean(cell, clusters[1])
        } else if (clusters.isNotEmpty()) {
            val bmp = extractAndClean(cell, clusters[0])
            glyphMap[alphabet[i].lowercaseChar()] = bmp
            glyphMap[alphabet[i].uppercaseChar()] = bmp
        }
        
        withContext(Dispatchers.Main) {
            onProgress((i + 1).toFloat() / alphabet.length)
        }
    }
}

fun findInkClusters(cell: Bitmap): List<Rect> {
    val clusters = mutableListOf<Rect>()
    var inInk = false
    var startX = 0
    
    for (x in 0 until cell.width) {
        var columnHasInk = false
        for (y in 0 until cell.height) {
            val p = cell.getPixel(x, y)
            // Pixel target for red/dark ink
            if (AndroidColor.red(p) < 175 && AndroidColor.green(p) < 160) {
                columnHasInk = true; break
            }
        }
        if (columnHasInk && !inInk) { startX = x; inInk = true }
        else if (!columnHasInk && inInk) {
            if (x - startX > 5) { // Ignore tiny noise
                clusters.add(Rect(startX, 0, x, cell.height))
            }
            inInk = false
        }
    }
    return clusters
}

fun extractAndClean(source: Bitmap, rect: Rect): Bitmap {
    val cropped = Bitmap.createBitmap(source, rect.left, 0, rect.width(), source.height)
    val result = cropped.copy(Bitmap.Config.ARGB_8888, true)
    
    for (y in 0 until result.height) {
        for (x in 0 until result.width) {
            val p = result.getPixel(x, y)
            // Remove gray paper/shadows
            if (AndroidColor.red(p) > 165 && AndroidColor.green(p) > 165) {
                result.setPixel(x, y, AndroidColor.TRANSPARENT)
            }
        }
    }
    return result
}

@Composable
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    val paint = android.graphics.Paint().apply {
        color = AndroidColor.LTGRAY
        textSize = 38f
        typeface = android.graphics.Typeface.SERIF
    }
    // Subtle wrinkle texture simulation
    val paperColor = Color(0xFFFDFBFA)

    Canvas(modifier = Modifier.fillMaxSize().background(paperColor)) {
        // Draw lines
        for (i in 0..size.height.toInt() step 65) {
            val yPos = 220f + i
            drawLine(Color(0xFFADCEEB).copy(0.4f), Offset(0f, yPos), Offset(size.width, yPos), 1.5f)
        }
        
        var curX = 135f
        var curY = 175f
        text.forEach { char ->
            val bmp = glyphMap[char]
            if (bmp != null) {
                val scale = 45f / bmp.height
                val w = bmp.width * scale
                if (curX + w > size.width - 50f) { curX = 135f; curY += 65f }
                drawImage(bmp.asImageBitmap(), IntOffset(curX.toInt(), curY.toInt()), 
                    IntSize(w.toInt(), 45), blendMode = BlendMode.Multiply)
                curX += w + 6f
            } else {
                if (char == ' ') curX += 35f 
                else if (char == '\n') { curX = 135f; curY += 65f }
                else {
                    // System font fallback
                    drawContext.canvas.nativeCanvas.drawText(char.toString(), curX, curY + 40f, paint)
                    curX += paint.measureText(char.toString()) + 5f
                }
            }
        }
    }
}

@Composable
fun HandwritingInputDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, 
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Write") } },
        text = { TextField(text, { text = it }, label = { Text("Type your note") }) })
}