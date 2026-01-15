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
import androidx.compose.ui.geometry.Size
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
            var isProcessing by remember { mutableStateOf(false) }
            val glyphMap = remember { mutableStateMapOf<Char, Bitmap>() }
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            val sheetLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    isProcessing = true
                    scope.launch {
                        try {
                            val inputStream = context.contentResolver.openInputStream(it)
                            // "inSampleSize = 2" prevents the app from crashing on high-res photos
                            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                            val fullSheet = BitmapFactory.decodeStream(inputStream, null, options)
                            
                            if (fullSheet != null) {
                                traceAndExtractInk(fullSheet, glyphMap)
                            }
                        } catch (e: Exception) {
                            // Error handling
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            }

            MaterialTheme {
                Scaffold { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        // This draws the "Real Life" paper and the "Traced" handwriting
                        PaperView(text = noteText, glyphMap = glyphMap)

                        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                            if (isProcessing) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1A73E8))
                                Text("Tracing Pen Strokes...", modifier = Modifier.padding(8.dp))
                            } else {
                                TextField(
                                    value = noteText,
                                    onValueChange = { noteText = it },
                                    placeholder = { Text("Start writing...") },
                                    modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.9f)),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.White)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { sheetLauncher.launch("image/*") }, 
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                                ) {
                                    Text("UPLOAD HANDWRITING PHOTO")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * TRACING LOGIC: Scans for your pen strokes and saves them as pure transparent overlays.
 */
suspend fun traceAndExtractInk(sheet: Bitmap, glyphMap: MutableMap<Char, Bitmap>) = withContext(Dispatchers.Default) {
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    val cellW = sheet.width / 10
    val cellH = sheet.height / 3

    for (i in 0 until alphabet.length) {
        val r = i / 10
        val c = i % 10
        val x = (c * cellW).coerceAtMost(sheet.width - cellW)
        val y = (r * cellH).coerceAtMost(sheet.height - cellH)
        
        val cell = Bitmap.createBitmap(sheet, x, y, cellW, cellH)
        val clusters = findInkClusters(cell)
        
        if (clusters.size >= 2) {
            glyphMap[alphabet[i].uppercaseChar()] = createTransparentStroke(cell, clusters[0])
            glyphMap[alphabet[i].lowercaseChar()] = createTransparentStroke(cell, clusters[1])
        } else if (clusters.isNotEmpty()) {
            val bmp = createTransparentStroke(cell, clusters[0])
            glyphMap[alphabet[i].lowercaseChar()] = bmp
            glyphMap[alphabet[i].uppercaseChar()] = bmp
        }
    }
}

fun findInkClusters(cell: Bitmap): List<Rect> {
    val clusters = mutableListOf<Rect>()
    var inInk = false; var startX = 0
    for (x in 0 until cell.width) {
        var hasInk = false
        for (y in 0 until cell.height) {
            val p = cell.getPixel(x, y)
            // Targeted detection for Red/Dark ink vs light paper
            if (AndroidColor.red(p) > 100 && AndroidColor.green(p) < 160) {
                hasInk = true; break
            }
        }
        if (hasInk && !inInk) { startX = x; inInk = true }
        else if (!hasInk && inInk) { 
            if (x - startX > 5) clusters.add(Rect(startX, 0, x, cell.height))
            inInk = false 
        }
    }
    return clusters
}

/**
 * PURGE LOGIC: Deletes all paper background and converts red to a blue pen look.
 */
fun createTransparentStroke(source: Bitmap, rect: Rect): Bitmap {
    val result = Bitmap.createBitmap(rect.width(), source.height, Bitmap.Config.ARGB_8888)
    for (x in 0 until rect.width()) {
        for (y in 0 until source.height) {
            val p = source.getPixel(rect.left + x, y)
            val r = AndroidColor.red(p)
            val g = AndroidColor.green(p)
            
            // If it's ink (Red is higher than Green, and it's not too bright)
            if (r > 90 && r > g + 20 && r < 200) {
                val intensity = (255 - r).coerceIn(100, 255)
                // Result: A rich blue ballpoint ink
                result.setPixel(x, y, AndroidColor.argb(intensity, 20, 50, 160))
            } else {
                // Total transparency for anything that isn't ink
                result.setPixel(x, y, AndroidColor.TRANSPARENT)
            }
        }
    }
    return result
}

@Composable
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRealisticPaperTexture()

        var curX = 145f
        var curY = 250f
        val lineSpacing = 90f
        val letterSize = 85f

        text.forEach { char ->
            val bmp = glyphMap[char]
            if (bmp != null) {
                val scale = letterSize / bmp.height
                val w = bmp.width * scale
                if (curX + w > size.width - 60f) { curX = 145f; curY += lineSpacing }

                // The "Multiply" blend mode makes the blue ink sink into the paper texture
                rotate(degrees = Random.nextFloat() * 4f - 2f, pivot = Offset(curX, curY)) {
                    drawImage(
                        image = bmp.asImageBitmap(),
                        dstOffset = IntOffset(curX.toInt(), (curY - letterSize + 15).toInt()),
                        dstSize = IntSize(w.toInt(), letterSize.toInt()),
                        blendMode = BlendMode.Multiply,
                        alpha = 0.95f
                    )
                }
                curX += w + 5f
            } else if (char == ' ') {
                curX += 42f
            } else if (char == '\n') {
                curX = 145f; curY += lineSpacing
            }
        }
    }
}

fun DrawScope.drawRealisticPaperTexture() {
    val random = Random(42)
    // 1. Physical Paper Base (Creamy off-white)
    drawRect(Color(0xFFFEFAF2))

    // 2. Realistic Lighting (Slightly darker at the corners)
    drawRect(
        brush = Brush.radialGradient(
            0.0f to Color.Transparent,
            1.0f to Color.Black.copy(alpha = 0.04f),
            center = center,
            radius = size.maxDimension
        )
    )

    // 3. Soft Notebook Lines
    val lineCol = Color(0xFF94B1C9).copy(alpha = 0.35f)
    for (i in 0..size.height.toInt() step 90) {
        val y = 250f + i
        drawLine(lineCol, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.5f)
    }
    drawLine(Color(0xFFD9A5A5).copy(alpha = 0.5f), Offset(120f, 0f), Offset(120f, size.height), strokeWidth = 3.5f)

    // 4. Paper Fiber Grain (600 tiny particles for realism)
    for (i in 0..600) {
        drawCircle(
            color = Color.Black.copy(alpha = 0.02f),
            radius = random.nextFloat() * 2.5f,
            center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
        )
    }
}