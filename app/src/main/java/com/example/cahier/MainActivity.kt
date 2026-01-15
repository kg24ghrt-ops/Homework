package com.example.cahier

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
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
import java.io.File
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var noteText by remember { mutableStateOf("") }
            var showTextDialog by remember { mutableStateOf(false) }
            val glyphMap = remember { mutableStateMapOf<Char, Bitmap>() }
            val context = LocalContext.current

            val sheetLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val fullSheet = BitmapFactory.decodeStream(inputStream)
                    if (fullSheet != null) {
                        // NEW: Smarter slicing that finds the ink
                        processAlphabetSheetSmarter(fullSheet, glyphMap)
                    }
                }
            }

            MaterialTheme(colorScheme = lightColorScheme()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
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
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        PaperView(text = noteText, glyphMap = glyphMap)
                        
                        if (showTextDialog) {
                            HandwritingInputDialog(
                                initial = noteText, 
                                onDismiss = { showTextDialog = false }, 
                                onConfirm = { noteText = it; showTextDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Smarter Slicing: Detects the bounds of the actual handwriting ink.
 */
fun processAlphabetSheetSmarter(sheet: Bitmap, glyphMap: MutableMap<Char, Bitmap>) {
    val rows = 3
    val cols = 10 
    val cellW = sheet.width / cols
    val cellH = sheet.height / rows
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    var charIndex = 0

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (charIndex >= alphabet.length) break
            
            // Initial rough crop
            val rawCell = Bitmap.createBitmap(sheet, c * cellW, r * cellH, cellW, cellH)
            
            // Find actual ink boundaries within that cell
            val bounds = findInkBounds(rawCell)
            if (bounds != null) {
                val croppedLetter = Bitmap.createBitmap(rawCell, bounds.left, bounds.top, bounds.width(), bounds.height())
                val cleanLetter = removeBackgroundPro(croppedLetter)
                
                glyphMap[alphabet[charIndex].lowercaseChar()] = cleanLetter
                glyphMap[alphabet[charIndex].uppercaseChar()] = cleanLetter
            }
            charIndex++
        }
    }
}

/**
 * Finds the rectangle containing actual ink to prevent "blocky" spacing
 */
fun findInkBounds(cell: Bitmap): Rect? {
    var minX = cell.width; var maxX = 0
    var minY = cell.height; var maxY = 0
    var foundInk = false

    for (y in 0 until cell.height) {
        for (x in 0 until cell.width) {
            val color = cell.getPixel(x, y)
            val r = AndroidColor.red(color)
            val g = AndroidColor.green(color)
            val b = AndroidColor.blue(color)
            
            // Detect non-white pixels (the ink)
            if (r < 180 || g < 180 || b < 180) {
                if (x < minX) minX = x; if (x > maxX) maxX = x
                if (y < minY) minY = y; if (y > maxY) maxY = y
                foundInk = true
            }
        }
    }
    return if (foundInk) Rect(minX, minY, maxX, maxY) else null
}

fun removeBackgroundPro(source: Bitmap): Bitmap {
    val newBitmap = source.copy(Bitmap.Config.ARGB_8888, true)
    for (y in 0 until newBitmap.height) {
        for (x in 0 until newBitmap.width) {
            val color = newBitmap.getPixel(x, y)
            val r = AndroidColor.red(color); val g = AndroidColor.green(color); val b = AndroidColor.blue(color)
            val luminance = (0.21 * r + 0.72 * g + 0.07 * b).toInt()
            if (luminance > 165) newBitmap.setPixel(x, y, AndroidColor.TRANSPARENT)
        }
    }
    return newBitmap
}

@Composable
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    val paperColor = Color(0xFFF9F6F0) 
    val paint = android.graphics.Paint().apply {
        color = AndroidColor.BLACK
        textSize = 45f
        typeface = android.graphics.Typeface.SERIF
    }

    Canvas(modifier = Modifier.fillMaxSize().background(paperColor)) {
        drawNotebookLines()
        drawPaperWrinkles()

        var curX = 130f
        var curY = 175f
        val lineSpacing = 65f

        text.forEach { char ->
            val bitmap = glyphMap[char]
            
            if (bitmap != null) {
                val targetH = 48f
                val scale = targetH / bitmap.height
                val finalW = bitmap.width * scale

                if (curX + finalW > size.width - 40f) {
                    curX = 130f; curY += lineSpacing
                }

                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = IntOffset(curX.toInt(), curY.toInt()),
                    dstSize = IntSize(finalW.toInt(), targetH.toInt()),
                    blendMode = BlendMode.Multiply,
                    alpha = 0.95f
                )
                curX += finalW + 6f
            } else {
                // FALLBACK: Use keyboard font if handwriting is missing
                if (char == ' ') {
                    curX += 40f
                } else if (char == '\n') {
                    curX = 130f; curY += lineSpacing
                } else {
                    drawContext.canvas.nativeCanvas.drawText(char.toString(), curX, curY + 40f, paint)
                    curX += paint.measureText(char.toString()) + 4f
                }
            }
        }
    }
}

fun DrawScope.drawNotebookLines() {
    val lineBlue = Color(0xFFADCEEB).copy(alpha = 0.5f)
    for (i in 0..size.height.toInt() step 65) {
        val y = 220f + i
        drawLine(lineBlue, Offset(0f, y), Offset(size.width, y), 1.5f)
    }
    drawLine(Color(0xFFFFB3B3).copy(alpha = 0.7f), Offset(110f, 0f), Offset(110f, size.height), 2.5f)
}

fun DrawScope.drawPaperWrinkles() {
    val random = Random(42) 
    for (i in 0..8) {
        val startX = random.nextFloat() * size.width
        val startY = random.nextFloat() * size.height
        drawLine(
            color = Color.Black.copy(alpha = 0.025f),
            start = Offset(startX, startY),
            end = Offset(startX + 400f, startY + 150f),
            strokeWidth = 50f
        )
    }
}

@Composable
fun HandwritingInputDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Write") } },
        title = { Text("Write your homework") },
        text = { TextField(value = text, onValueChange = { text = it }) }
    )
}
