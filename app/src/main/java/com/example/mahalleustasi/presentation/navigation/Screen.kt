package com.example.mahalleustasi.presentation.navigation

sealed class Screen(val route: String) {
    // Auth
    data object Login    : Screen("login")
    data object Register : Screen("register")

    // Main (BottomNav)
    data object Home    : Screen("home")
    data object Offers  : Screen("offers")
    data object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }

    // Detail
    data object Camera : Screen("camera")
    data object JobCreate : Screen("job_create?title={title}&desc={desc}&cat={cat}&budget={budget}") {
        fun createRoute(title: String? = null, desc: String? = null, cat: String? = null, budget: String? = null): String {
            val qTitle = title?.let { "title=${android.net.Uri.encode(it)}" } ?: ""
            val qDesc = desc?.let { "desc=${android.net.Uri.encode(it)}" } ?: ""
            val qCat = cat?.let { "cat=${android.net.Uri.encode(it)}" } ?: ""
            val qBudget = budget?.let { "budget=${android.net.Uri.encode(it)}" } ?: ""
            
            val query = listOf(qTitle, qDesc, qCat, qBudget).filter { it.isNotEmpty() }.joinToString("&")
            return if (query.isEmpty()) "job_create" else "job_create?$query"
        }
    }
    data object JobDetail : Screen("job_detail/{jobId}") {
        fun createRoute(jobId: String) = "job_detail/$jobId"
    }
    data object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
}
