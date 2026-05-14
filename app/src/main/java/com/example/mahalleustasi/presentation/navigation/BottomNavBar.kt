package com.example.mahalleustasi.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home    : BottomNavItem(Screen.Home.route,    "Ana Sayfa",  Icons.Default.Home)
    data object Offers  : BottomNavItem(Screen.Offers.route,  "Teklifler",  Icons.Default.LocalOffer)
    data object Rentals : BottomNavItem(Screen.Rentals.route, "Kiralık",    Icons.Default.Inventory2)
    data object Profile : BottomNavItem("profile/me",         "Profilim",   Icons.Default.Person)
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Offers,
    BottomNavItem.Rentals,
    BottomNavItem.Profile
)

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

fun shouldShowBottomBar(route: String?): Boolean {
    return route in listOf(
        Screen.Home.route,
        Screen.Offers.route,
        Screen.Rentals.route,
        "profile/me"
    )
}
