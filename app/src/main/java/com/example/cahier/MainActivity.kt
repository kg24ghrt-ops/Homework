package com.example.cahier

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
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
import androidx.compose.ui.platform.LocalContext // FIXED: Added this import
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
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

            // Launcher for the SINGLE alphabet sheet
            val sheetLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val fullSheet = BitmapFactory.decodeStream(inputStream)
                    if (fullSheet != null) {
                        processAlphabetSheet(context, fullSheet, glyphMap)
                    }
                }
            }

            MaterialTheme(colorScheme = lightColorScheme()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        Column(horizontalAlignment = Alignment.End) {
                            // FIXED: Corrected the ExtendedFloatingActionButton parameters
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

fun removeBackground(source: Bitmap): Bitmap {
    val width = source.width
    val height = source.height
    val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    for (i in pixels.indices) {
        val color = pixels[i]
        val r = AndroidColor.red(color)
        val g = AndroidColor.green(color)
        val b = AndroidColor.blue(color)
        val luminance = (0.21 * r + 0.72 * g + 0.07 * b).toInt()
        if (luminance > 160) { pixels[i] = AndroidColor.TRANSPARENT }
    }
    newBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return newBitmap
}

fun processAlphabetSheet(context: android.content.Context, sheet: Bitmap, glyphMap: MutableMap<Char, Bitmap>) {
    val cleanSheet = removeBackground(sheet)
    val rows = 3
    val cols = 10 
    val cellW = cleanSheet.width / cols
    val cellH = cleanSheet.height / rows
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    var charIndex = 0

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (charIndex >= alphabet.length) break
            val char = alphabet[charIndex]
            val letterBitmap = Bitmap.createBitmap(cleanSheet, c * cellW, r * cellH, cellW, cellH)
            glyphMap[char.lowercaseChar()] = letterBitmap
            glyphMap[char.uppercaseChar()] = letterBitmap
            charIndex++
        }
    }
}

@Composable
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    val paperColor = Color(0xFFF9F6F0) 

    Canvas(modifier = Modifier.fillMaxSize().background(paperColor)) {
        drawNotebookLines()
        drawPaperWrinkles()

        var curX = 130f
        var curY = 165f
        val lineSpacing = 65f

        text.forEach { char ->
            val bitmap = glyphMap[char]
            if (bitmap != null) {
                val targetH = 50f
                val scale = targetH / bitmap.height
                val finalW = bitmap.width * scale

                if (curX + finalW > size.width - 40f) {
                    curX = 130f
                    curY += lineSpacing
                }

                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = IntOffset(curX.toInt(), curY.toInt()),
                    dstSize = IntSize(finalW.toInt(), targetH.toInt()),
                    blendMode = BlendMode.Multiply,
                    alpha = 0.9f
                )
                curX += finalW + 4f
            } else if (char == ' ') { curX += 45f }
        }
    }
}

fun DrawScope.drawNotebookLines() {
    val lineBlue = Color(0xFFADCEEB).copy(alpha = 0.6f)
    val lineSpacing = 65f
    val headerSpace = 220f
    for (i in 0..size.height.toInt() step lineSpacing.toInt()) {
        val y = headerSpace + i
        drawLine(lineBlue, Offset(0f, y), Offset(size.width, y), 1.5f)
    }
    drawLine(Color(0xFFFFB3B3), Offset(110f, 0f), Offset(110f, size.height), 2f)
}

fun DrawScope.drawPaperWrinkles() {
    val random = Random(42) 
    for (i in 0..5) {
        val startX = random.nextFloat() * size.width
        val startY = random.nextFloat() * size.height
        drawLine(
            color = Color.Black.copy(alpha = 0.03f),
            start = Offset(startX, startY),
            end = Offset(startX + 300f, startY + 200f),
            strokeWidth = 40f
        )
    }
}

@Composable
fun HandwritingInputDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Write") } },
        title = { Text("Type your note") },
        text = { TextField(value = text, onValueChange = { text = it }) }
    )
}
