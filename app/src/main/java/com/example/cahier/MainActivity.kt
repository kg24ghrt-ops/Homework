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
            ) { uri ->
                uri?.let {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val fullSheet = BitmapFactory.decodeStream(inputStream)
                    if (fullSheet != null) {
                        // Slices and then TIGHTLY crops each letter
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
 * Finds the actual ink in the cell and crops it perfectly to remove blocks.
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
            
            // 1. Get the rough grid block
            val rawCell = Bitmap.createBitmap(sheet, c * cellW, r * cellH, cellW, cellH)
            
            // 2. Find the ink bounds to remove the "box" around letters
            val bounds = findInkBounds(rawCell)
            if (bounds != null) {
                val cropped = Bitmap.createBitmap(rawCell, bounds.left, bounds.top, bounds.width(), bounds.height())
                val clean = makeTransparent(cropped)
                
                glyphMap[alphabet[charIndex].lowercaseChar()] = clean
                glyphMap[alphabet[charIndex].uppercaseChar()] = clean
            }
            charIndex++
        }
    }
}

fun findInkBounds(cell: Bitmap): Rect? {
    var minX = cell.width; var maxX = 0
    var minY = cell.height; var maxY = 0
    var found = false

    for (y in 0 until cell.height) {
        for (x in 0 until cell.width) {
            val p = cell.getPixel(x, y)
            // If pixel is dark enough (ink), expand the crop box
            if (AndroidColor.red(p) < 160 || AndroidColor.green(p) < 160) {
                if (x < minX) minX = x; if (x > maxX) maxX = x
                if (y < minY) minY = y; if (y > maxY) maxY = y
                found = true
            }
        }
    }
    // Add a tiny 2-pixel padding so it's not too tight
    return if (found) Rect(
        (minX - 2).coerceAtLeast(0), 
        (minY - 2).coerceAtLeast(0), 
        (maxX + 2).coerceAtMost(cell.width), 
        (maxY + 2).coerceAtMost(cell.height)
    ) else null
}

fun makeTransparent(source: Bitmap): Bitmap {
    val result = source.copy(Bitmap.Config.ARGB_8888, true)
    for (y in 0 until result.height) {
        for (x in 0 until result.width) {
            val p = result.getPixel(x, y)
            val r = AndroidColor.red(p); val g = AndroidColor.green(p); val b = AndroidColor.blue(p)
            // Aggressive threshold to clean up photo shadows
            if (r > 150 && g > 150 && b > 150) {
                result.setPixel(x, y, AndroidColor.TRANSPARENT)
            }
        }
    }
    return result
}

@Composable
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    val paperColor = Color(0xFFFDFBFA) // Creamy paper
    val paint = android.graphics.Paint().apply {
        color = AndroidColor.DKGRAY
        textSize = 42f
        alpha = 180
    }

    Canvas(modifier = Modifier.fillMaxSize().background(paperColor)) {
        drawNotebookLines()
        drawPaperWrinkles()

        var curX = 135f
        var curY = 175f
        val lineH = 65f

        text.forEach { char ->
            val bitmap = glyphMap[char]
            
            if (bitmap != null) {
                val targetH = 45f
                val scale = targetH / bitmap.height
                val finalW = bitmap.width * scale

                if (curX + finalW > size.width - 50f) {
                    curX = 135f; curY += lineH
                }

                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = IntOffset(curX.toInt(), curY.toInt()),
                    dstSize = IntSize(finalW.toInt(), targetH.toInt()),
                    blendMode = BlendMode.Multiply, // Blends ink into the paper color
                    alpha = 0.9f
                )
                curX += finalW + 4f
            } else {
                // FALLBACK: If handwriting is missing, use keyboard font
                if (char == ' ') { curX += 35f }
                else if (char == '\n') { curX = 135f; curY += lineH }
                else {
                    drawContext.canvas.nativeCanvas.drawText(char.toString(), curX, curY + 35f, paint)
                    curX += paint.measureText(char.toString()) + 5f
                }
            }
        }
    }
}

fun DrawScope.drawNotebookLines() {
    val blueLines = Color(0xFFADCEEB).copy(alpha = 0.4f)
    for (i in 0..size.height.toInt() step 65) {
        val y = 220f + i
        drawLine(blueLines, Offset(0f, y), Offset(size.width, y), 1.5f)
    }
    drawLine(Color(0xFFFFB3B3).copy(alpha = 0.6f), Offset(115f, 0f), Offset(115f, size.height), 2f)
}

fun DrawScope.drawPaperWrinkles() {
    val random = Random(123) 
    for (i in 0..6) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        drawLine(
            color = Color.Black.copy(alpha = 0.02f),
            start = Offset(x, y),
            end = Offset(x + 500f, y + 100f),
            strokeWidth = 60f
        )
    }
}

@Composable
fun HandwritingInputDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Write") } },
        title = { Text("Note Content") },
        text = { TextField(value = text, onValueChange = { text = it }) }
    )
}
