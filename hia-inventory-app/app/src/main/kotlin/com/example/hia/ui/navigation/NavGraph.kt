package com.example.hia.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.NavHostController
import com.example.hia.TaskViewModel
import com.example.hia.ui.screens.InventoryScreen
import com.example.hia.ui.screens.PhotosScreen
import com.example.hia.ui.screens.SettingsScreen

fun NavGraphBuilder.appNavGraph(navController: NavHostController, taskViewModel: TaskViewModel) {
    navigation(startDestination = "inventory", route = "app_graph") {
        composable("inventory") { InventoryScreen(navController, taskViewModel) }
        composable("photos") { PhotosScreen(navController, taskViewModel) }
        composable("settings") { SettingsScreen(navController, taskViewModel) }
    }
}