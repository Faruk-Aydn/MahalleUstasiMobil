package com.example.mahalleustasi.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mahalleustasi.presentation.screens.auth.LoginScreen
import com.example.mahalleustasi.presentation.screens.auth.RegisterScreen
import com.example.mahalleustasi.presentation.screens.camera.CameraScreen
import com.example.mahalleustasi.presentation.screens.chat.ChatScreen
import com.example.mahalleustasi.presentation.screens.home.HomeScreen
import com.example.mahalleustasi.presentation.screens.job.JobCreateScreen
import com.example.mahalleustasi.presentation.screens.job.JobDetailScreen
import com.example.mahalleustasi.presentation.screens.offers.OffersScreen
import com.example.mahalleustasi.presentation.screens.profile.ProfileScreen
import com.example.mahalleustasi.presentation.screens.review.ReviewScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController  = navController,
        startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route,
        modifier = modifier
    ) {

        // ── Auth ──────────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
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

        // ── Ana Ekran ─────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCreateJob = { navController.navigate(Screen.JobCreate.createRoute()) },
                onNavigateToProfile   = { userId -> navController.navigate(Screen.Profile.createRoute(userId)) },
                onNavigateToJobDetail = { jobId -> navController.navigate(Screen.JobDetail.createRoute(jobId)) }
            )
        }

        // ── Teklifler ─────────────────────────────────────────────────────────
        composable(Screen.Offers.route) {
            OffersScreen(
                onNavigateToJobDetail = { jobId ->
                    navController.navigate(Screen.JobDetail.createRoute(jobId))
                },
                onNavigateToChat = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId))
                }
            )
        }

        // ── Kamera / AI Asistan ───────────────────────────────────────────────
        composable(Screen.Camera.route) {
            CameraScreen(
                onAnalysisSuccess = { result ->
                    val route = Screen.JobCreate.createRoute(
                        title  = result.title,
                        desc   = result.description,
                        cat    = result.category.name,
                        budget = result.estimatedCost
                    )
                    navController.navigate(route) {
                        popUpTo(Screen.Camera.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── İlan Oluştur ──────────────────────────────────────────────────────
        composable(
            route = Screen.JobCreate.route,
            arguments = listOf(
                navArgument("title")  { type = NavType.StringType; defaultValue = "" },
                navArgument("desc")   { type = NavType.StringType; defaultValue = "" },
                navArgument("cat")    { type = NavType.StringType; defaultValue = "" },
                navArgument("budget") { type = NavType.StringType; defaultValue = "" }
            )
        ) {
            JobCreateScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAiAssistant = { navController.navigate(Screen.Camera.route) }
            )
        }

        // ── İlan Detayı ───────────────────────────────────────────────────────
        composable(
            route = Screen.JobDetail.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) {
            JobDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId))
                },
                onNavigateToReview = { jobId, revieweeId, revieweeName, role ->
                    navController.navigate(Screen.Review.createRoute(jobId, revieweeId, revieweeName, role))
                }
            )
        }

        // ── Sohbet ────────────────────────────────────────────────────────────
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Değerlendirme ─────────────────────────────────────────────────────
        composable(
            route = Screen.Review.route,
            arguments = listOf(
                navArgument("jobId")        { type = NavType.StringType },
                navArgument("revieweeId")   { type = NavType.StringType },
                navArgument("revieweeName") { type = NavType.StringType },
                navArgument("role")         { type = NavType.StringType }
            )
        ) {
            ReviewScreen(
                onReviewSubmitted = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onSkip = { navController.popBackStack() }
            )
        }

        // ── Profil ────────────────────────────────────────────────────────────
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
