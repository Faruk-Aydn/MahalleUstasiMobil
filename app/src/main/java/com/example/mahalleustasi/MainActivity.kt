package com.example.mahalleustasi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mahalleustasi.presentation.navigation.BottomNavBar
import com.example.mahalleustasi.presentation.navigation.NavGraph
import com.example.mahalleustasi.presentation.navigation.shouldShowBottomBar
import com.example.mahalleustasi.presentation.screens.auth.AuthViewModel
import com.example.mahalleustasi.ui.theme.MahalleUstasiTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MahalleUstasiTheme {
                val navController  = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()
                val currentUser    by authViewModel.currentUser.collectAsStateWithLifecycle()

                // Mevcut route'u takip et — bottom bar görünürlüğü için
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Uygulama ilk açıldığında anlık "Login" ekranı gözükmemesi için
                // senkron olarak Firebase auth'u kontrol edelim.
                val isUserLoggedIn = FirebaseAuth.getInstance().currentUser != null

                Scaffold(
                    bottomBar = {
                        // Sadece giriş yapılmışsa ve ana ekranlardayken göster
                        if ((currentUser != null || isUserLoggedIn) && shouldShowBottomBar(currentRoute)) {
                            BottomNavBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        isLoggedIn    = isUserLoggedIn, // State yerine senkron durumu veriyoruz
                        modifier      = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}