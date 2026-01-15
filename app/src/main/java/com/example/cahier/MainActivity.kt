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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var noteText by remember { mutableStateOf("") }
            var showTextDialog by remember { mutableStateOf(false) }
            val glyphMap = remember { mutableStateMapOf<Char, Bitmap>() }
            val context = LocalContext.current

            val sheetLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val fullSheet = BitmapFactory.decodeStream(inputStream)
                    if (fullSheet != null) {
                        processPairsSheet(fullSheet, glyphMap)
                    }
                }
            }

            MaterialTheme {
                Scaffold(
                    floatingActionButton = {
                        Column(horizontalAlignment = Alignment.End) {
                            ExtendedFloatingActionButton(
                                text = { Text("UPLOAD SHEET") },
                                icon = { Text("📄") },
                                onClick = { sheetLauncher.launch("image/*") }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FloatingActionButton(onClick = { showTextDialog = true }) {
                                Text("EDIT")
                            }
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        PaperView(text = noteText, glyphMap = glyphMap)
                        if (showTextDialog) {
                            HandwritingInputDialog(noteText, { showTextDialog = false }, { noteText = it; showTextDialog = false })
                        }
                    }
                }
            }
        }
    }
}

/**
 * Splits "Aa" blocks into separate 'A' and 'a' bitmaps
 */
fun processPairsSheet(sheet: Bitmap, glyphMap: MutableMap<Char, Bitmap>) {
    val rows = 3
    val cols = 10 
    val cellW = sheet.width / cols
    val cellH = sheet.height / rows
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    var charIdx = 0

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (charIdx >= alphabet.length) break
            
            val rawCell = Bitmap.createBitmap(sheet, c * cellW, r * cellH, cellW, cellH)
            
            // Find the two separate ink clusters (Upper and Lower)
            val inkRects = findSeparateInkClusters(rawCell)
            
            if (inkRects.size >= 2) {
                // First cluster is Uppercase, Second is Lowercase
                glyphMap[alphabet[charIdx].uppercaseChar()] = cleanAndCrop(rawCell, inkRects[0])
                glyphMap[alphabet[charIdx].lowercaseChar()] = cleanAndCrop(rawCell, inkRects[1])
            } else if (inkRects.size == 1) {
                // If they are touching, map to both as a fallback
                val bmp = cleanAndCrop(rawCell, inkRects[0])
                glyphMap[alphabet[charIdx].uppercaseChar()] = bmp
                glyphMap[alphabet[charIdx].lowercaseChar()] = bmp
            }
            charIdx++
        }
    }
}

fun findSeparateInkClusters(cell: Bitmap): List<Rect> {
    val rects = mutableListOf<Rect>()
    var inCluster = false
    var startX = 0
    
    // Scan horizontally to find the "gap" between Uppercase and Lowercase
    for (x in 0 until cell.width) {
        var columnHasInk = false
        for (y in 0 until cell.height) {
            val p = cell.getPixel(x, y)
            if (AndroidColor.red(p) < 170) { // Detecting Red/Dark ink
                columnHasInk = true
                break
            }
        }
        
        if (columnHasInk && !inCluster) {
            startX = x
            inCluster = true
        } else if (!columnHasInk && inCluster) {
            rects.add(getVerticalInkBounds(cell, startX, x))
            inCluster = false
        }
    }
    return rects
}

fun getVerticalInkBounds(cell: Bitmap, xStart: Int, xEnd: Int): Rect {
    var minY = cell.height; var maxY = 0
    for (x in xStart until xEnd) {
        for (y in 0 until cell.height) {
            val p = cell.getPixel(x, y)
            if (AndroidColor.red(p) < 170) {
                if (y < minY) minY = y; if (y > maxY) maxY = y
            }
        }
    }
    return Rect(xStart, (minY-2).coerceAtLeast(0), xEnd, (maxY+2).coerceAtMost(cell.height))
}

fun cleanAndCrop(source: Bitmap, rect: Rect): Bitmap {
    val cropped = Bitmap.createBitmap(source, rect.left, rect.top, rect.width(), rect.height())
    val clean = cropped.copy(Bitmap.Config.ARGB_8888, true)
    for (y in 0 until clean.height) {
        for (x in 0 until clean.width) {
            val p = clean.getPixel(x, y)
            // Remove gray paper shadows (Thresholding)
            if (AndroidColor.red(p) > 160 && AndroidColor.green(p) > 160) {
                clean.setPixel(x, y, AndroidColor.TRANSPARENT)
            }
        }
    }
    return clean
}

@Composable
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    val paint = android.graphics.Paint().apply {
        color = AndroidColor.GRAY
        textSize = 40f
    }

    Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBFA))) {
        drawNotebookLines()
        var curX = 135f
        var curY = 175f

        text.forEach { char ->
            val bitmap = glyphMap[char]
            if (bitmap != null) {
                val scale = 45f / bitmap.height
                val w = bitmap.width * scale
                if (curX + w > size.width - 50f) { curX = 135f; curY += 65f }
                drawImage(bitmap.asImageBitmap(), IntOffset(curX.toInt(), curY.toInt()), 
                    IntSize(w.toInt(), 45), blendMode = BlendMode.Multiply, alpha = 0.9f)
                curX += w + 4f
            } else {
                // KEYBOARD FALLBACK: Natural spacing for untracked letters
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

fun DrawScope.drawNotebookLines() {
    for (i in 0..size.height.toInt() step 65) {
        drawLine(Color(0xFFADCEEB).copy(0.4f), Offset(0f, 220f + i), Offset(size.width, 220f + i), 1.5f)
    }
    drawLine(Color(0xFFFFB3B3).copy(0.6f), Offset(115f, 0f), Offset(115f, size.height), 2.5f)
}

@Composable
fun HandwritingInputDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, 
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Write") } },
        text = { TextField(text, { text = it }) })
}