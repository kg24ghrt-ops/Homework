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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var noteText by remember { mutableStateOf("") }
            var showTextDialog by remember { mutableStateOf(false) }
            val glyphMap = remember { mutableStateMapOf<Char, Bitmap>() }
            val context = LocalContext.current

            // Load saved font on startup
            LaunchedEffect(Unit) {
                val folder = File(context.filesDir, "auto_font")
                if (folder.exists()) {
                    folder.listFiles()?.forEach { file ->
                        val char = file.nameWithoutExtension.firstOrNull()
                        if (char != null) glyphMap[char] = BitmapFactory.decodeFile(file.absolutePath)
                    }
                }
            }

            // Launcher for the SINGLE alphabet sheet
            val sheetLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val fullSheet = BitmapFactory.decodeStream(inputStream)
                    if (fullSheet != null) {
                        // Automatically slice the sheet into 26 letters
                        processAlphabetSheet(context, fullSheet, glyphMap)
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
                                Text("EDIT", modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
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
 * Slices one image into 26 blocks and removes background
 */
fun processAlphabetSheet(context: android.content.Context, sheet: Bitmap, glyphMap: MutableMap<Char, Bitmap>) {
    val cleanSheet = removeBackground(sheet)
    
    // We assume 3 rows and 9-10 letters per row based on your photo
    val rows = 3
    val cols = 10 
    val cellWidth = cleanSheet.width / cols
    val cellHeight = cleanSheet.height / rows
    
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    var charIndex = 0

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (charIndex >= alphabet.length) break
            
            val char = alphabet[charIndex]
            val x = c * cellWidth
            val y = r * cellHeight
            
            // Create a small bitmap for this specific letter
            val letterBitmap = Bitmap.createBitmap(cleanSheet, x, y, cellWidth, cellHeight)
            
            glyphMap[char] = letterBitmap
            saveToDisk(context, char, letterBitmap)
            charIndex++
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
        // Remove white background (RGB > 180)
        if (r > 180 && g > 180 && b > 180) {
            pixels[i] = AndroidColor.TRANSPARENT
        }
    }
    newBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return newBitmap
}

fun saveToDisk(context: android.content.Context, char: Char, bitmap: Bitmap) {
    val folder = File(context.filesDir, "auto_font")
    if (!folder.exists()) folder.mkdirs()
    FileOutputStream(File(folder, "$char.png")).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
}

@Composable
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    Canvas(modifier = Modifier.fillMaxSize().background(Color.White)) {
        val lineBlue = Color(0xFFADCEEB)
        val lineSpacing = 65f
        val headerSpace = 220f
        val marginX = 110f

        for (i in 0..size.height.toInt() step lineSpacing.toInt()) {
            val y = headerSpace + i
            drawLine(lineBlue, Offset(0f, y), Offset(size.width, y), 2f)
        }

        var curX = marginX + 20f
        var curY = headerSpace - 55f 

        text.forEach { char ->
            val cleanChar = char.lowercaseChar()
            val bitmap = glyphMap[cleanChar]
            if (bitmap != null) {
                val targetH = 55f 
                val scale = targetH / bitmap.height
                val finalW = bitmap.width * scale

                if (curX + finalW > size.width - 40f) {
                    curX = marginX + 20f
                    curY += lineSpacing
                }

                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = IntOffset(curX.toInt(), curY.toInt()),
                    dstSize = IntSize(finalW.toInt(), targetH.toInt())
                )
                curX += finalW + 5f
            } else if (char == ' ') { curX += 45f }
        }
    }
}

@Composable
fun HandwritingInputDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write Note") },
        text = { TextField(value = text, onValueChange = { text = it }) },
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Write") } }
    )
}
