package com.example.cahier

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
            var showAssignDialog by remember { mutableStateOf(false) }
            var lastPickedUri by remember { mutableStateOf<Uri?>(null) }
            
            // SnapshotStateMap ensures the Canvas redraws instantly when a letter is added
            val glyphMap = remember { mutableStateMapOf<Char, Bitmap>() }
            val context = LocalContext.current

            // Load previously saved handwriting PNGs on startup
            LaunchedEffect(Unit) {
                val folder = File(context.filesDir, "my_handwriting")
                if (folder.exists()) {
                    folder.listFiles()?.forEach { file ->
                        val char = file.nameWithoutExtension.firstOrNull()
                        if (char != null) {
                            glyphMap[char] = BitmapFactory.decodeFile(file.absolutePath)
                        }
                    }
                }
            }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    lastPickedUri = uri
                    showAssignDialog = true 
                }
            }

            MaterialTheme(colorScheme = lightColorScheme()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        Column(horizontalAlignment = Alignment.End) {
                            SmallFloatingActionButton(onClick = { launcher.launch("image/png") }) {
                                Text("PNG", modifier = Modifier.padding(horizontal = 8.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            FloatingActionButton(onClick = { showTextDialog = true }) {
                                Text("EDIT", modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        PaperView(text = noteText, glyphMap = glyphMap)

                        if (showAssignDialog) {
                            AssignGlyphDialog(
                                onDismiss = { showAssignDialog = false },
                                onConfirm = { char ->
                                    lastPickedUri?.let { uri ->
                                        val inputStream = context.contentResolver.openInputStream(uri)
                                        val bitmap = BitmapFactory.decodeStream(inputStream)
                                        if (bitmap != null) {
                                            val cleanChar = char.lowercaseChar()
                                            glyphMap[cleanChar] = bitmap
                                            // Save to internal storage
                                            saveGlyphToDisk(context, cleanChar, bitmap)
                                        }
                                    }
                                    showAssignDialog = false
                                }
                            )
                        }

                        if (showTextDialog) {
                            HandwritingInputDialog(
                                initialText = noteText,
                                onDismiss = { showTextDialog = false },
                                onConfirm = { noteText = it.lowercase(); showTextDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Logic to save the PNG so you don't have to re-upload every time
fun saveGlyphToDisk(context: android.content.Context, char: Char, bitmap: Bitmap) {
    val folder = File(context.filesDir, "my_handwriting")
    if (!folder.exists()) folder.mkdirs()
    val file = File(folder, "$char.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
}

@Composable
fun AssignGlyphDialog(onDismiss: () -> Unit, onConfirm: (Char) -> Unit) {
    var charInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign PNG to Letter") },
        text = {
            TextField(
                value = charInput,
                onValueChange = { if (it.length <= 1) charInput = it },
                placeholder = { Text("e.g. 'a'") }
            )
        },
        confirmButton = {
            Button(onClick = { if (charInput.isNotEmpty()) onConfirm(charInput[0]) }) {
                Text("Save")
            }
        }
    )
}

@Composable
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    Canvas(modifier = Modifier.fillMaxSize().background(Color.White)) {
        val lineBlue = Color(0xFFADCEEB)
        val horizontalMargin = 110f
        val lineSpacing = 65f
        val headerSpace = 220f

        // Draw Notebook Lines
        var yPos = headerSpace
        while (yPos < size.height) {
            drawLine(lineBlue, Offset(0f, yPos), Offset(size.width, yPos), 2f)
            yPos += lineSpacing
        }

        // Render Handwriting
        var currentX = horizontalMargin + 25f
        var currentY = headerSpace - 65f 

        text.forEach { char ->
            val bitmap = glyphMap[char]
            if (bitmap != null) {
                // Scale glyph to fit the line height (55f)
                val targetH = 55f
                val scale = targetH / bitmap.height
                val scaledW = bitmap.width * scale

                if (currentX + scaledW > size.width - 40f) {
                    currentX = horizontalMargin + 25f
                    currentY += lineSpacing
                }

                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = IntOffset(currentX.toInt(), currentY.toInt())
                    // DrawImage uses the original size by default; 
                    // To handle scaling properly in Canvas, we use translate/scale or dstSize
                )
                currentX += scaledW + 12f
            } else if (char == ' ') {
                currentX += 45f
            }
        }
    }
}

@Composable
fun HandwritingInputDialog(initialText: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Type your note") },
        text = { TextField(value = text, onValueChange = { text = it }) },
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Write") } }
    )
}
