package com.example.hia.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.hia.R

@Composable
fun TopNavBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination?.route

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 16.dp) // 左边距和左边Card边距对齐
    ) {
        // App Logo - 直径适应顶部栏的高度
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(40.dp) // 顶部栏高度通常为64.dp，Logo直径设为40.dp适应高度
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(Modifier.width(12.dp))
        
        NavButton(
            selected = current == "inventory",
            icon = { Icon(Icons.Filled.LibraryBooks, contentDescription = "图书盘点") },
            label = "图书盘点",
            onClick = { navController.navigate("inventory") }
        )
        NavButton(
            selected = current == "photos",
            icon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = "照片管理") },
            label = "照片管理",
            onClick = { navController.navigate("photos") }
        )
        NavButton(
            selected = current == "settings",
            icon = { Icon(Icons.Filled.Settings, contentDescription = "系统设置") },
            label = "系统设置",
            onClick = { navController.navigate("settings") }
        )
    }
}

@Composable
private fun NavButton(selected: Boolean, icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
    val colors = if (selected) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = colors
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}
