package com.example.cahier

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

enum class InkType(val displayName: String) {
    BLUE("Blue"), 
    RED("Red"), 
    BLACK("Black")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var selectedInk by remember { mutableStateOf(InkType.BLUE) }
            var tolerance by remember { mutableStateOf(0.3f) }
            var handwritingBitmap by remember { mutableStateOf<Bitmap?>(null) }
            var isProcessing by remember { mutableStateOf(false) }
            var processingProgress by remember { mutableStateOf(0f) }
            var scale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            val sheetLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    isProcessing = true
                    processingProgress = 0f
                    scope.launch {
                        try {
                            val bitmap = withContext(Dispatchers.IO) {
                                val inputStream = context.contentResolver.openInputStream(it)
                                val options = BitmapFactory.Options().apply {
                                    inSampleSize = 1 // Full quality for realistic look
                                }
                                BitmapFactory.decodeStream(inputStream, null, options)
                            }
                            
                            if (bitmap != null) {
                                val processed = processHandwriting(bitmap, selectedInk, tolerance) { progress ->
                                    processingProgress = progress
                                }
                                handwritingBitmap?.recycle()
                                handwritingBitmap = processed
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            }

            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2C2C2E)) // Dark desk surface
                ) {
                    // Realistic Paper Sheet with Shadow
                    RealisticPaperView(
                        handwritingBitmap = handwritingBitmap,
                        scale = scale,
                        offset = offset,
                        onTransform = { scaleChange, offsetChange ->
                            scale = (scale * scaleChange).coerceIn(0.5f, 3f)
                            offset += offsetChange
                        }
                    )

                    // Floating Control Panel
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .shadow(16.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1C1C1E).copy(alpha = 0.95f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Your Handwriting Notebook",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Color Selection
                            Text(
                                "Ink Color:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                InkColorButton(
                                    color = Color(0xFF1E3A8A),
                                    isSelected = selectedInk == InkType.BLUE,
                                    label = "Blue"
                                ) { selectedInk = InkType.BLUE }
                                
                                InkColorButton(
                                    color = Color(0xFFDC2626),
                                    isSelected = selectedInk == InkType.RED,
                                    label = "Red"
                                ) { selectedInk = InkType.RED }
                                
                                InkColorButton(
                                    color = Color(0xFF1F2937),
                                    isSelected = selectedInk == InkType.BLACK,
                                    label = "Black"
                                ) { selectedInk = InkType.BLACK }
                            }

                            // Sensitivity Slider
                            Text(
                                "Sensitivity: ${(tolerance * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Slider(
                                value = tolerance,
                                onValueChange = { tolerance = it },
                                valueRange = 0.1f..0.6f,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF0A84FF),
                                    activeTrackColor = Color(0xFF0A84FF)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Scan Button
                            Button(
                                onClick = { sheetLauncher.launch("image/*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = !isProcessing,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0A84FF)
                                )
                            ) {
                                Text(
                                    if (isProcessing) 
                                        "Processing... ${(processingProgress * 100).toInt()}%"
                                    else 
                                        "SCAN MY HANDWRITING",
                                    fontSize = 16.sp
                                )
                            }

                            if (isProcessing) {
                                LinearProgressIndicator(
                                    progress = processingProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    color = Color(0xFF0A84FF)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InkColorButton(
    color: Color,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(
                    elevation = if (isSelected) 8.dp else 2.dp,
                    shape = CircleShape
                )
                .background(color, CircleShape)
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = Color.White,
                    shape = CircleShape
                )
                .clickable { onClick() }
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) Color.White else Color.Gray,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun RealisticPaperView(
    handwritingBitmap: Bitmap?,
    scale: Float,
    offset: Offset,
    onTransform: (Float, Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Paper Sheet with realistic shadow and texture
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(4.dp),
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.4f)
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFDF7), // Warm paper white
                            Color(0xFFFFFBF0)  // Slightly cream at bottom
                        )
                    ),
                    shape = RoundedCornerShape(4.dp)
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        onTransform(zoom, pan)
                    }
                }
        ) {
            val paperWidth = size.width
            val paperHeight = size.height

            // Draw subtle paper texture with noise
            for (i in 0..100) {
                val x = (Math.random() * paperWidth).toFloat()
                val y = (Math.random() * paperHeight).toFloat()
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.02f),
                    radius = 1f,
                    center = Offset(x, y)
                )
            }

            // Draw ruled lines (college-ruled style)
            val lineSpacing = 32f
            val marginLeft = 80f
            var yPos = 60f
            
            while (yPos < paperHeight - 40) {
                // Horizontal ruled lines
                drawLine(
                    color = Color(0xFFB8D4E8).copy(alpha = 0.4f),
                    start = Offset(marginLeft + 20, yPos),
                    end = Offset(paperWidth - 60, yPos),
                    strokeWidth = 1f
                )
                yPos += lineSpacing
            }

            // Red margin line (left side)
            drawLine(
                color = Color(0xFFE57373).copy(alpha = 0.5f),
                start = Offset(marginLeft, 40f),
                end = Offset(marginLeft, paperHeight - 40),
                strokeWidth = 1.5f
            )

            // Subtle paper edge shadows
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    startX = 0f,
                    endX = 20f
                )
            )

            // Draw the scanned handwriting
            handwritingBitmap?.let { bitmap ->
                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.translate(offset.x, offset.y)
                    canvas.scale(scale, scale)
                    
                    // Center the handwriting on paper
                    val x = (paperWidth - bitmap.width) / 2
                    val y = (paperHeight - bitmap.height) / 2
                    
                    canvas.nativeCanvas.drawBitmap(
                        bitmap,
                        x,
                        y,
                        android.graphics.Paint().apply {
                            isAntiAlias = true
                            isDither = true
                        }
                    )
                    canvas.restore()
                }
            }
        }
    }
}

/**
 * Process handwriting with realistic ink preservation
 */
suspend fun processHandwriting(
    source: Bitmap,
    inkType: InkType,
    tolerance: Float,
    onProgress: (Float) -> Unit = {}
): Bitmap = withContext(Dispatchers.Default) {
    val result = Bitmap.createBitmap(
        source.width,
        source.height,
        Bitmap.Config.ARGB_8888
    )
    
    val totalPixels = source.width * source.height
    var processedPixels = 0
    
    for (y in 0 until source.height) {
        for (x in 0 until source.width) {
            val pixel = source.getPixel(x, y)
            
            if (isInkColor(pixel, inkType, tolerance)) {
                // Preserve the exact darkness/pressure of the original stroke
                val enhancedPixel = enhanceInkPixel(pixel, inkType)
                result.setPixel(x, y, enhancedPixel)
            } else {
                result.setPixel(x, y, AndroidColor.TRANSPARENT)
            }
            
            processedPixels++
            if (processedPixels % 10000 == 0) {
                onProgress(processedPixels.toFloat() / totalPixels)
            }
        }
    }
    
    onProgress(1f)
    result
}

/**
 * Advanced HSV-based ink detection
 */
fun isInkColor(pixel: Int, type: InkType, tolerance: Float): Boolean {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(pixel, hsv)
    
    val hue = hsv[0]
    val saturation = hsv[1]
    val value = hsv[2]
    
    return when (type) {
        InkType.BLUE -> {
            hue in (210f - tolerance * 50)..(240f + tolerance * 50) &&
            saturation > max(0.25f - tolerance, 0.1f) &&
            value > 0.2f
        }
        InkType.RED -> {
            ((hue in 0f..(20f + tolerance * 30) || 
              hue in (340f - tolerance * 30)..360f) &&
            saturation > max(0.3f - tolerance, 0.15f) &&
            value > 0.2f)
        }
        InkType.BLACK -> {
            value < min(0.45f + tolerance, 0.7f)
        }
    }
}

/**
 * Enhance ink while preserving natural stroke variation
 */
fun enhanceInkPixel(pixel: Int, type: InkType): Int {
    val r = AndroidColor.red(pixel)
    val g = AndroidColor.green(pixel)
    val b = AndroidColor.blue(pixel)
    
    // Preserve exact pressure/darkness from original writing
    val brightness = (r + g + b) / 3
    val alpha = ((255 - brightness) * 1.3).toInt().coerceIn(180, 255)
    
    // Realistic ink colors
    return when (type) {
        InkType.BLUE -> AndroidColor.argb(alpha, 25, 55, 135)   // Classic blue ink
        InkType.RED -> AndroidColor.argb(alpha, 200, 30, 30)    // Red pen ink
        InkType.BLACK -> AndroidColor.argb(alpha, 20, 20, 25)   // Black ink
    }
}