package com.example.mahalleustasi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.mahalleustasi.presentation.navigation.NavGraph
import com.example.mahalleustasi.presentation.screens.auth.AuthViewModel
import com.example.mahalleustasi.ui.theme.MahalleUstasiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MahalleUstasiTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()
                val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

                NavGraph(
                    navController = navController,
                    isLoggedIn    = currentUser != null
                )
            }
        }
    }
}