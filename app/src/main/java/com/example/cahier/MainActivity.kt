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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var noteText by remember { mutableStateOf("") }
            var showTextDialog by remember { mutableStateOf(false) }
            var showAssignDialog by remember { mutableStateOf(false) }
            var selectedUri by remember { mutableStateOf<Uri?>(null) }
            
            val glyphMap = remember { mutableStateMapOf<Char, Bitmap>() }
            val context = LocalContext.current

            // Launcher to pick the PNG
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    selectedUri = uri
                    showAssignDialog = true // Open dialog to ask "Which letter is this?"
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

                        // Dialog 1: Assigning the PNG to a letter
                        if (showAssignDialog) {
                            AssignGlyphDialog(
                                onDismiss = { showAssignDialog = false },
                                onConfirm = { char ->
                                    selectedUri?.let { uri ->
                                        val bitmap = BitmapFactory.decodeStream(
                                            context.contentResolver.openInputStream(uri)
                                        )
                                        glyphMap[char.lowercaseChar()] = bitmap
                                    }
                                    showAssignDialog = false
                                }
                            )
                        }

                        // Dialog 2: Typing the note
                        if (showTextDialog) {
                            HandwritingInputDialog(
                                initialText = noteText,
                                onDismiss = { showTextDialog = false },
                                onConfirm = { 
                                    noteText = it.lowercase() 
                                    showTextDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssignGlyphDialog(onDismiss: () -> Unit, onConfirm: (Char) -> Unit) {
    var charInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Which letter is this PNG?") },
        text = {
            TextField(
                value = charInput,
                onValueChange = { if (it.length <= 1) charInput = it },
                placeholder = { Text("e.g. 'a'") }
            )
        },
        confirmButton = {
            Button(onClick = { if (charInput.isNotEmpty()) onConfirm(charInput[0]) }) {
                Text("Assign")
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

        // Draw Lines
        var yPos = headerSpace
        while (yPos < size.height) {
            drawLine(lineBlue, Offset(0f, yPos), Offset(size.width, yPos), 2f)
            yPos += lineSpacing
        }

        // Render mapped PNGs
        var currentX = horizontalMargin + 25f
        var currentY = headerSpace - 70f 

        text.forEach { char ->
            val bitmap = glyphMap[char]
            if (bitmap != null) {
                // Determine scaling to fit line height
                val scale = 50f / bitmap.height
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = IntOffset(currentX.toInt(), currentY.toInt())
                )
                currentX += (bitmap.width * scale) + 10f
                
                if (currentX > size.width - 60f) {
                    currentX = horizontalMargin + 25f
                    currentY += lineSpacing
                }
            } else if (char == ' ') {
                currentX += 40f
            }
        }
    }
}

@Composable
fun HandwritingInputDialog(initialText: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Type to use your PNGs") },
        text = { TextField(value = text, onValueChange = { text = it }) },
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Write") } }
    )
}