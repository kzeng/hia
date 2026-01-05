package com.example.hia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.hia.ui.navigation.appNavGraph
import com.example.hia.ui.theme.HiaInventoryAppTheme
import com.example.hia.ui.screens.InventoryScreen
import com.example.hia.ui.screens.PhotosScreen
import com.example.hia.ui.screens.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HiaInventoryAppTheme {
                val navController = rememberNavController()

                val bottomDestinations = listOf(
                    "inventory" to "盘点",
                    "photos" to "照片",
                    "settings" to "设置"
                )

                Scaffold(
                    bottomBar = {
                        val backStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = backStackEntry?.destination?.route
                        NavigationBar {
                            bottomDestinations.forEach { (route, label) ->
                                NavigationBarItem(
                                    selected = currentDestination == route,
                                    onClick = { navController.navigate(route) },
                                    label = { Text(label) },
                                    icon = {}
                                )
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = "inventory"
                    ) {
                        composable("inventory") { InventoryScreen(navController) }
                        composable("photos") { PhotosScreen(navController) }
                        composable("settings") { SettingsScreen(navController) }
                    }
                }
            }
        }
    }
}