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

            val sheetLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val fullSheet = BitmapFactory.decodeStream(inputStream)
                    if (fullSheet != null) {
                        isProcessing = true
                        // Run processing in background so the UI doesn't freeze
                        LaunchedEffect(fullSheet) {
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

                        // PROGRESS OVERLAY
                        if (isProcessing) {
                            Column(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.5f)),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(progress = progress, color = Color.White)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Processing Handwriting... ${(progress * 100).toInt()}%", color = Color.White)
                            }
                        }

                        if (showTextDialog) {
                            HandwritingInputDialog(noteText, { showTextDialog = false }, { noteText = it; showTextDialog = false })
                        }
                        
                        // Small Upload Button
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

/**
 * Recognizes letters by scanning for "Ink Pixels" (Pistol logic)
 */
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
        
        val x = c * cellW
        val y = r * cellH
        
        // Target the specific cell area
        val cell = Bitmap.createBitmap(sheet, x, y, cellW, cellH)
        
        // Find the "Pistol" (The actual ink clusters)
        val clusters = findInkClusters(cell)
        
        if (clusters.size >= 2) {
            glyphMap[alphabet[i].uppercaseChar()] = extractAndClean(cell, clusters[0])
            glyphMap[alphabet[i].lowercaseChar()] = extractAndClean(cell, clusters[1])
        } else if (clusters.isNotEmpty()) {
            val single = extractAndClean(cell, clusters[0])
            glyphMap[alphabet[i].lowercaseChar()] = single
            glyphMap[alphabet[i].uppercaseChar()] = single
        }
        
        onProgress((i + 1).toFloat() / alphabet.length)
    }
}

fun findInkClusters(cell: Bitmap): List<Rect> {
    val clusters = mutableListOf<Rect>()
    var inInk = false
    var startX = 0
    
    // Scan pixels for the red/dark pen color
    for (x in 0 until cell.width) {
        var hasInk = false
        for (y in 0 until cell.height) {
            val p = cell.getPixel(x, y)
            // Pixel target: Red ink is usually low in Green/Blue
            if (AndroidColor.red(p) < 180 && (AndroidColor.green(p) < 150 || AndroidColor.blue(p) < 150)) {
                hasInk = true; break
            }
        }
        if (hasInk && !inInk) { startX = x; inInk = true }
        else if (!hasInk && inInk) {
            clusters.add(Rect(startX, 0, x, cell.height))
            inInk = false
        }
    }
    return clusters
}

fun extractAndClean(source: Bitmap, rect: Rect): Bitmap {
    val cropped = Bitmap.createBitmap(source, rect.left, 0, rect.width(), source.height)
    val result = cropped.copy(Bitmap.Config.ARGB_8888, true)
    
    // Final Pixel Polish: Remove everything that isn't the pen ink
    for (y in 0 until result.height) {
        for (x in 0 until result.width) {
            val p = result.getPixel(x, y)
            if (AndroidColor.red(p) > 170 && AndroidColor.green(p) > 170) {
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
        textSize = 40f
    }
    Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBFA))) {
        // Draw blue lines
        for (i in 0..size.height.toInt() step 65) {
            drawLine(Color(0xFFADCEEB).copy(0.4f), Offset(0f, 220f + i), Offset(size.width, 220f + i), 1.5f)
        }
        
        var curX = 135f
        var curY = 175f
        text.forEach { char ->
            val bmp = glyphMap[char]
            if (bmp != null) {
                val w = bmp.width * (45f / bmp.height)
                if (curX + w > size.width - 50f) { curX = 135f; curY += 65f }
                drawImage(bmp.asImageBitmap(), IntOffset(curX.toInt(), curY.toInt()), IntSize(w.toInt(), 45), blendMode = BlendMode.Multiply)
                curX += w + 5f
            } else {
                if (char == ' ') curX += 30f 
                else if (char == '\n') { curX = 135f; curY += 65f }
                else {
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
        text = { TextField(text, { text = it }) })
}