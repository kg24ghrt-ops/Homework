package com.example.cahier

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.text.TextStyle
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
            var typedText by remember { mutableStateOf("") }
            val glyphMap = remember { mutableStateMapOf<Char, Bitmap>() }
            var isProcessing by remember { mutableStateOf(false) }
            var processingProgress by remember { mutableStateOf(0f) }
            var scale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            var showToast by remember { mutableStateOf(false) }
            var toastMessage by remember { mutableStateOf("") }
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
                                val options = BitmapFactory.Options()
                                BitmapFactory.decodeStream(inputStream, null, options)
                            }
                            
                            if (bitmap != null) {
                                extractHandwritingGlyphs(
                                    bitmap, 
                                    selectedInk, 
                                    tolerance, 
                                    glyphMap
                                ) { progress ->
                                    processingProgress = progress
                                }
                                toastMessage = "Handwriting loaded! ${glyphMap.size} characters mapped"
                                showToast = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            toastMessage = "Error loading handwriting"
                            showToast = true
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
                        .background(Color(0xFF2C2C2E))
                ) {
                    // Realistic Paper with handwriting
                    RealisticPaperView(
                        text = typedText,
                        glyphMap = glyphMap,
                        scale = scale,
                        offset = offset,
                        onTransform = { scaleChange, offsetChange ->
                            scale = (scale * scaleChange).coerceIn(0.5f, 3f)
                            offset += offsetChange
                        },
                        onSaveAsPng = { paperBitmap ->
                            scope.launch {
                                val saved = savePaperAsPng(context, paperBitmap)
                                toastMessage = if (saved) "Saved to Gallery!" else "Failed to save"
                                showToast = true
                            }
                        }
                    )

                    // Toast notification
                    if (showToast) {
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(2000)
                            showToast = false
                        }
                        Snackbar(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                        ) {
                            Text(toastMessage)
                        }
                    }

                    // Control Panel
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
                            // Typing Area
                            BasicTextField(
                                value = typedText,
                                onValueChange = { typedText = it },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color(0xFF2C2C2E),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(16.dp)
                                    .height(80.dp),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (typedText.isEmpty()) {
                                            Text(
                                                "Type here and watch your handwriting appear...",
                                                color = Color.Gray,
                                                fontSize = 14.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Color Selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            }

                            // Sensitivity
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Sensitivity: ${(tolerance * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.weight(1f)
                                )
                            }
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

                            Spacer(modifier = Modifier.height(8.dp))

                            // Scan Button
                            Button(
                                onClick = { sheetLauncher.launch("image/*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                enabled = !isProcessing,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0A84FF)
                                )
                            ) {
                                Text(
                                    if (isProcessing) 
                                        "Learning... ${(processingProgress * 100).toInt()}%"
                                    else 
                                        "SCAN HANDWRITING SHEET",
                                    fontSize = 14.sp
                                )
                            }

                            if (isProcessing) {
                                LinearProgressIndicator(
                                    progress = processingProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    color = Color(0xFF0A84FF)
                                )
                            }

                            Text(
                                "Tip: Write A-Z, a-z, 0-9 and symbols on a white sheet, then scan",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 8.dp)
                            )
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
                .size(48.dp)
                .shadow(
                    elevation = if (isSelected) 6.dp else 2.dp,
                    shape = CircleShape
                )
                .background(color, CircleShape)
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = Color.White,
                    shape = CircleShape
                )
                .clickable { onClick() }
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) Color.White else Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun RealisticPaperView(
    text: String,
    glyphMap: Map<Char, Bitmap>,
    scale: Float,
    offset: Offset,
    onTransform: (Float, Offset) -> Unit,
    onSaveAsPng: (Bitmap) -> Unit
) {
    var paperBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
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
                            Color(0xFFFFFDF7),
                            Color(0xFFFFFBF0)
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

            // Create bitmap for export
            val bitmap = Bitmap.createBitmap(
                paperWidth.toInt(),
                paperHeight.toInt(),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)

            // Draw paper background
            canvas.drawColor(AndroidColor.rgb(255, 253, 247))

            // Paper texture
            for (i in 0..100) {
                val x = (Math.random() * paperWidth).toFloat()
                val y = (Math.random() * paperHeight).toFloat()
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.02f),
                    radius = 1f,
                    center = Offset(x, y)
                )
            }

            // Ruled lines
            val lineSpacing = 40f
            val marginLeft = 80f
            var yPos = 80f
            
            while (yPos < paperHeight - 40) {
                drawLine(
                    color = Color(0xFFB8D4E8).copy(alpha = 0.4f),
                    start = Offset(marginLeft + 20, yPos),
                    end = Offset(paperWidth - 60, yPos),
                    strokeWidth = 1f
                )
                canvas.drawLine(
                    marginLeft + 20, yPos,
                    paperWidth - 60, yPos,
                    android.graphics.Paint().apply {
                        color = AndroidColor.argb(100, 184, 212, 232)
                        strokeWidth = 1f
                    }
                )
                yPos += lineSpacing
            }

            // Margin line
            drawLine(
                color = Color(0xFFE57373).copy(alpha = 0.5f),
                start = Offset(marginLeft, 40f),
                end = Offset(marginLeft, paperHeight - 40),
                strokeWidth = 1.5f
            )
            canvas.drawLine(
                marginLeft, 40f,
                marginLeft, paperHeight - 40,
                android.graphics.Paint().apply {
                    color = AndroidColor.argb(128, 229, 115, 115)
                    strokeWidth = 1.5f
                }
            )

            // Render handwritten text
            var xPos = marginLeft + 30f
            var yPos2 = 80f
            val charSpacing = 4f
            
            text.forEach { char ->
                when (char) {
                    '\n' -> {
                        xPos = marginLeft + 30f
                        yPos2 += lineSpacing
                    }
                    ' ' -> {
                        xPos += 15f
                    }
                    else -> {
                        glyphMap[char]?.let { glyph ->
                            // Draw on Canvas (visible)
                            drawIntoCanvas { 
                                it.nativeCanvas.drawBitmap(
                                    glyph,
                                    xPos,
                                    yPos2 - glyph.height * 0.7f,
                                    android.graphics.Paint().apply {
                                        isAntiAlias = true
                                    }
                                )
                            }
                            // Draw on export bitmap
                            canvas.drawBitmap(
                                glyph,
                                xPos,
                                yPos2 - glyph.height * 0.7f,
                                android.graphics.Paint().apply {
                                    isAntiAlias = true
                                }
                            )
                            xPos += glyph.width + charSpacing
                        }
                    }
                }
                
                // Wrap to next line if needed
                if (xPos > paperWidth - 100) {
                    xPos = marginLeft + 30f
                    yPos2 += lineSpacing
                }
            }

            paperBitmap = bitmap
        }

        // Save button
        if (text.isNotEmpty() && glyphMap.isNotEmpty()) {
            FloatingActionButton(
                onClick = { paperBitmap?.let { onSaveAsPng(it) } },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                containerColor = Color(0xFF0A84FF)
            ) {
                Text(
                    text = "💾",
                    fontSize = 24.sp,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Extract individual character glyphs from handwriting sheet
 */
suspend fun extractHandwritingGlyphs(
    sheet: Bitmap,
    inkType: InkType,
    tolerance: Float,
    glyphMap: MutableMap<Char, Bitmap>,
    onProgress: (Float) -> Unit
) = withContext(Dispatchers.Default) {
    // Define expected character grid
    val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,!?;:-() "
    
    // Assume 10 columns layout
    val cols = 10
    val rows = (characters.length + cols - 1) / cols
    val cellWidth = sheet.width / cols
    val cellHeight = sheet.height / rows
    
    glyphMap.clear()
    
    characters.forEachIndexed { index, char ->
        val col = index % cols
        val row = index / cols
        
        val rect = Rect(
            col * cellWidth,
            row * cellHeight,
            (col + 1) * cellWidth,
            (row + 1) * cellHeight
        )
        
        val glyph = extractAndCleanGlyph(sheet, rect, inkType, tolerance)
        if (glyph != null) {
            glyphMap[char] = glyph
        }
        
        onProgress((index + 1).toFloat() / characters.length)
    }
}

/**
 * Extract a single glyph and crop to content
 */
fun extractAndCleanGlyph(
    source: Bitmap,
    rect: Rect,
    inkType: InkType,
    tolerance: Float
): Bitmap? {
    var minX = rect.width()
    var minY = rect.height()
    var maxX = 0
    var maxY = 0
    var hasInk = false

    // Find bounding box
    for (y in 0 until rect.height()) {
        for (x in 0 until rect.width()) {
            val pixel = source.getPixel(rect.left + x, rect.top + y)
            if (isInkColor(pixel, inkType, tolerance)) {
                hasInk = true
                minX = min(minX, x)
                minY = min(minY, y)
                maxX = max(maxX, x)
                maxY = max(maxY, y)
            }
        }
    }

    if (!hasInk) return null

    // Add padding
    val padding = 2
    minX = max(0, minX - padding)
    minY = max(0, minY - padding)
    maxX = min(rect.width() - 1, maxX + padding)
    maxY = min(rect.height() - 1, maxY + padding)

    val glyphWidth = maxX - minX + 1
    val glyphHeight = maxY - minY + 1

    val glyph = Bitmap.createBitmap(glyphWidth, glyphHeight, Bitmap.Config.ARGB_8888)

    // Extract ink pixels only
    for (y in minY..maxY) {
        for (x in minX..maxX) {
            val pixel = source.getPixel(rect.left + x, rect.top + y)
            if (isInkColor(pixel, inkType, tolerance)) {
                glyph.setPixel(x - minX, y - minY, enhanceInkPixel(pixel, inkType))
            }
        }
    }

    return glyph
}

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

fun enhanceInkPixel(pixel: Int, type: InkType): Int {
    val r = AndroidColor.red(pixel)
    val g = AndroidColor.green(pixel)
    val b = AndroidColor.blue(pixel)
    
    val brightness = (r + g + b) / 3
    val alpha = ((255 - brightness) * 1.3).toInt().coerceIn(180, 255)
    
    return when (type) {
        InkType.BLUE -> AndroidColor.argb(alpha, 25, 55, 135)
        InkType.RED -> AndroidColor.argb(alpha, 200, 30, 30)
        InkType.BLACK -> AndroidColor.argb(alpha, 20, 20, 25)
    }
}

suspend fun savePaperAsPng(context: android.content.Context, bitmap: Bitmap): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val filename = "handwriting_${System.currentTimeMillis()}.png"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
                
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    true
                } ?: false
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}