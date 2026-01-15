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
            var showDialog by remember { mutableStateOf(false) }
            
            // State to store mapped handwriting glyphs
            val glyphMap = remember { mutableStateMapOf<Char, Bitmap>() }
            val context = LocalContext.current

            // Picker to select your handwriting PNG
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    // Temporary: Map the selected PNG to the letter 'a'
                    // In a full implementation, you'd assign this to a specific character.
                    glyphMap['a'] = bitmap 
                }
            }

            MaterialTheme(colorScheme = lightColorScheme()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        Column(horizontalAlignment = Alignment.End) {
                            // PNG Button to add your handwriting
                            SmallFloatingActionButton(
                                onClick = { launcher.launch("image/png") },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text("PNG", modifier = Modifier.padding(horizontal = 8.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            // EDIT Button to type text
                            FloatingActionButton(onClick = { showDialog = true }) {
                                Text("EDIT", modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        PaperView(text = noteText, glyphMap = glyphMap)

                        if (showDialog) {
                            HandwritingInputDialog(
                                initialText = noteText,
                                onDismiss = { showDialog = false },
                                onConfirm = { 
                                    noteText = it.lowercase() 
                                    showDialog = false
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
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    Canvas(modifier = Modifier.fillMaxSize().background(Color.White)) {
        val width = size.width
        val height = size.height
        
        // Notebook Aesthetics matched to your image
        val lineBlue = Color(0xFFADCEEB)
        val horizontalMargin = 110f
        val lineSpacing = 65f
        val headerSpace = 220f

        // 1. Draw horizontal blue guidelines
        var yPos = headerSpace
        while (yPos < height) {
            drawLine(lineBlue, Offset(0f, yPos), Offset(width, yPos), 2f)
            yPos += lineSpacing
        }

        // 2. Render Handwriting Glyphs
        var currentX = horizontalMargin + 25f
        var currentY = headerSpace - 60f // Align bitmap with the blue line

        text.forEach { char ->
            val bitmap = glyphMap[char]
            if (bitmap != null) {
                // Draw your custom PNG
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = IntOffset(currentX.toInt(), currentY.toInt())
                )
                currentX += bitmap.width + 10f // Advance X by bitmap width
                
                // Wrap to next line if it hits the edge
                if (currentX > width - 40f) {
                    currentX = horizontalMargin + 25f
                    currentY += lineSpacing
                }
            } else if (char == ' ') {
                currentX += 40f // Space width
            }
        }
    }
}

@Composable
fun HandwritingInputDialog(
    initialText: String, 
    onDismiss: () -> Unit, 
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write your notes") },
        text = {
            TextField(
                value = text, 
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) { Text("Write") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}