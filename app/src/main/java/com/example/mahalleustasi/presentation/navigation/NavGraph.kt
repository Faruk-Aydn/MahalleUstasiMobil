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
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToOffers    = { navController.navigate(Screen.Offers.route)   },
                onNavigateToProfile   = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
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
                        popUpTo(Screen.Camera.route) { inclusive = true }
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
