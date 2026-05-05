package com.example.mahalleustasi.presentation.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    // ── Auth ──────────────────────────────────────────────────────────────────
    data object Login    : Screen("login")
    data object Register : Screen("register")

    // ── Main ──────────────────────────────────────────────────────────────────
    data object Home    : Screen("home")
    data object Offers  : Screen("offers")

    data object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }

    // ── Detay ─────────────────────────────────────────────────────────────────
    data object Camera : Screen("camera")

    data object JobCreate : Screen("job_create?title={title}&desc={desc}&cat={cat}&budget={budget}") {
        fun createRoute(
            title: String? = null,
            desc: String? = null,
            cat: String? = null,
            budget: String? = null
        ): String {
            val params = buildList {
                title?.let  { add("title=${Uri.encode(it)}")  }
                desc?.let   { add("desc=${Uri.encode(it)}")   }
                cat?.let    { add("cat=${Uri.encode(it)}")    }
                budget?.let { add("budget=${Uri.encode(it)}") }
            }
            return if (params.isEmpty()) "job_create" else "job_create?${params.joinToString("&")}"
        }
    }

    data object JobDetail : Screen("job_detail/{jobId}") {
        fun createRoute(jobId: String) = "job_detail/$jobId"
    }

    // ── Chat ──────────────────────────────────────────────────────────────────
    data object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/${Uri.encode(chatId)}"
    }

    // ── Review ────────────────────────────────────────────────────────────────
    data object Review : Screen("review/{jobId}/{revieweeId}/{revieweeName}/{role}") {
        fun createRoute(
            jobId: String,
            revieweeId: String,
            revieweeName: String,
            role: String
        ) = "review/${Uri.encode(jobId)}/${Uri.encode(revieweeId)}/${Uri.encode(revieweeName)}/${Uri.encode(role)}"
    }
}
