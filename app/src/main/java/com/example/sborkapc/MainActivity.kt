/*
© Жиляков Д.Э., 2026. Все права защищены.
*/

package com.example.sborkapc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.LocalImageLoader
import com.example.sborkapc.network.buildCoilImageLoader
import com.example.sborkapc.ui.screens.BuildScreen
import com.example.sborkapc.ui.screens.CatalogScreen
import com.example.sborkapc.ui.screens.CategoryScreen
import com.example.sborkapc.ui.screens.SavedBuildsScreen
import com.example.sborkapc.ui.theme.AppTheme
import com.example.sborkapc.ui.viewmodel.BuildViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History

sealed class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    object Catalog : BottomTab("catalog", "Каталог", Icons.Default.GridView)
    object Build   : BottomTab("build",   "Сборка",  Icons.Default.Bolt)
    object History : BottomTab("history", "История", Icons.Default.History)
}

@Composable
fun AppBottomBar(
    currentRoute: String?,
    buildProgress: Float,
    onTabClick: (BottomTab) -> Unit
) {
    val tabs = listOf(BottomTab.Catalog, BottomTab.Build, BottomTab.History)
    Surface(
        color = AppTheme.CardBg,
        border = BorderStroke(1.dp, AppTheme.GlassBorder),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute?.startsWith(tab.route) == true
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabClick(tab) }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (tab is BottomTab.Build && buildProgress > 0f && buildProgress < 1f) {
                            CircularProgressIndicator(
                                progress = { buildProgress },
                                modifier = Modifier.size(34.dp),
                                color = AppTheme.NeonCyan,
                                trackColor = AppTheme.Divider,
                                strokeWidth = 2.dp
                            )
                        }
                        Icon(
                            tab.icon, null,
                            tint = if (selected) AppTheme.NeonCyan else AppTheme.TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tab.label,
                        color = if (selected) AppTheme.NeonCyan else AppTheme.TextMuted,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val view = LocalView.current
            
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as MainActivity).window
                    val controller = WindowCompat.getInsetsController(window, view)

                    window.statusBarColor = Color.Transparent.toArgb()
                    window.navigationBarColor = Color.Transparent.toArgb()

                    controller.isAppearanceLightStatusBars = false
                }
            }

            val imageLoader = remember { buildCoilImageLoader(context) }

            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                val navController = rememberNavController()
                val viewModel: BuildViewModel = viewModel(
                    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                )
                val uiState = viewModel.uiState
                val navBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStack?.destination?.route

                MaterialTheme {
                    Scaffold(
                        containerColor = AppTheme.DarkBg,
                        bottomBar = {
                            val showBar = currentRoute == "catalog" ||
                                    currentRoute == "build"   ||
                                    currentRoute == "history"
                            AnimatedVisibility(
                                visible = showBar,
                                enter = slideInVertically { it },
                                exit  = slideOutVertically { it }
                            ) {
                                AppBottomBar(
                                    currentRoute  = currentRoute,
                                    buildProgress = uiState.buildProgress,
                                    onTabClick    = { tab ->
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState    = true
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController    = navController,
                            startDestination = "catalog",
                            modifier         = Modifier.padding(innerPadding)
                        ) {
                            composable("catalog") {
                                CatalogScreen(
                                    uiState         = uiState,
                                    onCategoryClick = { cat -> navController.navigate("category/$cat") }
                                )
                            }
                            composable("category/{cat}") { back ->
                                val cat = back.arguments?.getString("cat") ?: return@composable
                                CategoryScreen(
                                    catName   = cat,
                                    viewModel = viewModel,
                                    onBack    = { navController.popBackStack() }
                                )
                            }
                            composable("build") {
                                BuildScreen(
                                    viewModel           = viewModel,
                                    onNavigateToCategory = { cat -> navController.navigate("category/$cat") },
                                    onFinish = {
                                        navController.navigate("history") {
                                            popUpTo("catalog") {
                                                inclusive = false
                                                saveState = false
                                            }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    }
                                )
                            }
                            composable("history") {
                                SavedBuildsScreen(
                                    viewModel   = viewModel,
                                    onEditClick = { navController.navigate("build") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}