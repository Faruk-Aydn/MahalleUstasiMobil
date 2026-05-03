package com.example.mahalleustasi.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mahalleustasi.presentation.screens.auth.LoginScreen
import com.example.mahalleustasi.presentation.screens.auth.RegisterScreen
import com.example.mahalleustasi.presentation.screens.home.HomeScreen
import com.example.mahalleustasi.presentation.screens.profile.ProfileScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
@Composable
fun NavGraph(
    navController: NavHostController,
    isLoggedIn: Boolean
) {
    NavHost(
        navController  = navController,
        startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
    ) {

        // ── Auth ─────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Main ─────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCreateJob = { navController.navigate(Screen.JobCreate.createRoute()) },
                onNavigateToOffers    = { navController.navigate(Screen.Offers.route)   },
                onNavigateToProfile   = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                },
                onNavigateToJobDetail = { jobId ->
                    navController.navigate(Screen.JobDetail.createRoute(jobId))
                }
            )
        }

        composable(Screen.Offers.route) {
            com.example.mahalleustasi.presentation.screens.offers.OffersScreen(
                onNavigateToJobDetail = { jobId ->
                    navController.navigate(Screen.JobDetail.createRoute(jobId))
                }
            )
        }

        composable(Screen.Camera.route) {
            com.example.mahalleustasi.presentation.screens.camera.CameraScreen(
                onAnalysisSuccess = { result ->
                    val route = Screen.JobCreate.createRoute(
                        title = result.title,
                        desc = result.description,
                        cat = result.category.name,
                        budget = result.estimatedCost
                    )
                    navController.navigate(route) {
                        popUpTo(Screen.JobCreate.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.JobCreate.route,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("desc") { type = NavType.StringType; defaultValue = "" },
                navArgument("cat") { type = NavType.StringType; defaultValue = "" },
                navArgument("budget") { type = NavType.StringType; defaultValue = "" }
            )
        ) {
            com.example.mahalleustasi.presentation.screens.job.JobCreateScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAiAssistant = { navController.navigate(Screen.Camera.route) }
            )
        }

        composable(
            route = Screen.JobDetail.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) {
            com.example.mahalleustasi.presentation.screens.job.JobDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(
                userId         = userId,
                onNavigateBack = { navController.popBackStack() },
                onLogout       = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
