package com.example.mahalleustasi.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Bottom Navigation için her sekmenin tanımı.
 */
sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home    : BottomNavItem(Screen.Home.route,   "Ana Sayfa",   Icons.Default.Home)
    data object Offers  : BottomNavItem(Screen.Offers.route, "Tekliflerim", Icons.Default.LocalOffer)
    data object Profile : BottomNavItem("profile/me",        "Profilim",    Icons.Default.Person)
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Offers,
    BottomNavItem.Profile
)

/**
 * NE: Ana navigasyon çubuğu (Home / Tekliflerim / Profilim).
 * NEDEN: Kullanıcıların Tekliflerim ekranını kolayca bulabilmesi için
 * bottom bar standart bir UX pattern'dır.
 */
@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon     = { Icon(item.icon, contentDescription = item.label) },
                label    = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick  = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Geri stack'i temizle — tek bir Home instance'ı kalsın
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                }
            )
        }
    }
}

/**
 * Bottom bar'ın gösterileceği route'lar.
 * Auth ve detay sayfalarında bottom bar gizlenir.
 */
fun shouldShowBottomBar(route: String?): Boolean {
    return route in listOf(
        Screen.Home.route,
        Screen.Offers.route,
        "profile/me"
    )
}
