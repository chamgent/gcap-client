package com.gcap.client.ui.navigation

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gcap.client.ui.chat.ChatScreen
import com.gcap.client.ui.image.ImageChatScreen
import com.gcap.client.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object ChatRoute

@Serializable
object ImageChatRoute

@Serializable
object SettingsRoute

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isImeVisible = WindowInsets.isImeVisible

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isImeVisible) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Text("💬") },
                        label = { Text("聊天") },
                        selected = currentDestination?.hierarchy?.any { it.route == ChatRoute::class.qualifiedName } == true,
                        onClick = {
                            navController.navigate(ChatRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Text("🖼️") },
                        label = { Text("图片生成") },
                        selected = currentDestination?.hierarchy?.any { it.route == ImageChatRoute::class.qualifiedName } == true,
                        onClick = {
                            navController.navigate(ImageChatRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Text("⚙️") },
                        label = { Text("设置") },
                        selected = currentDestination?.hierarchy?.any { it.route == SettingsRoute::class.qualifiedName } == true,
                        onClick = {
                            navController.navigate(SettingsRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ChatRoute,
            modifier = Modifier.padding(bottom = if (isImeVisible) 0.dp else innerPadding.calculateBottomPadding())
        ) {
            composable<ChatRoute> {
                ChatScreen()
            }
            composable<ImageChatRoute> {
                ImageChatScreen()
            }
            composable<SettingsRoute> {
                SettingsScreen()
            }
        }
    }
}
