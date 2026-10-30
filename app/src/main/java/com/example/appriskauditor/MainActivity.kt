package com.example.appriskauditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.appriskauditor.ui.detail.DetailScreen
import com.example.appriskauditor.ui.home.HomeScreen
import com.example.appriskauditor.ui.home.HomeViewModel
import com.example.appriskauditor.ui.theme.AppRiskAuditorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRiskAuditorTheme {
                AppRiskAuditorApp(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun AppRiskAuditorApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                onAppClick = { packageName ->
                    navController.navigate("detail/$packageName")
                }
            )
        }

        composable(
            route = "detail/{packageName}",
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            DetailScreen(
                packageName = packageName,
                viewModel = homeViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateHome = { navController.popBackStack("home", false) }
            )
        }
    }
}
