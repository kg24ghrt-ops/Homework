package com.meticha.jetpackboilerplate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
// CORRECTED IMPORT:
import androidx.lifecycle.viewmodel.compose.viewModel 
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.meticha.jetpackboilerplate.ui.CommanderDashboard
import com.meticha.jetpackboilerplate.ui.VectorViewModel
import com.meticha.jetpackboilerplate.ui.theme.CallBudyTheme
import com.meticha.jetpackboilerplate.ui.theme.CommandBlack
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            // This call now links correctly to the lifecycle-viewmodel-compose library
            val vViewModel: VectorViewModel = viewModel()
            
            CallBudyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CommandBlack 
                ) {
                    CommanderDashboard(viewModel = vViewModel)
                }
            }
        }
    }
}
