package com.meticha.jetpackboilerplate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewmodel.compose.viewModel
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
        
        // 1. Enable Edge-to-Edge for that modern "Full Screen" tactical look
        enableEdgeToEdge()
        
        setContent {
            // 2. Initialize the ViewModel (The Brain)
            // This stays alive even if the screen rotates
            val vViewModel: VectorViewModel = viewModel()
            
            // 3. Apply our Tactical Theme (Fire #1)
            CallBudyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CommandBlack // Force deep space black
                ) {
                    // 4. Inject the Dashboard (The Interface)
                    // We pass the ViewModel directly so it can control the UI
                    CommanderDashboard(viewModel = vViewModel)
                }
            }
        }
    }
}
