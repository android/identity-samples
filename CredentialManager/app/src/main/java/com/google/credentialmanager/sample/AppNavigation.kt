package com.google.credentialmanager.sample

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// 1. Define navigation routes
sealed class Screen(val route: String) {
    object Main : Screen("main")
    object SignUp : Screen("signup")
    object SignIn : Screen("signin")
    object Home : Screen("home")
}

// 2. Create a Composable function for the navigation host
@Composable
fun AppNavHost(
    startDestination: String = Screen.Main.route
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Main.route) {
            MainScreen(navController = navController)
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(navController = navController)
        }
        composable(Screen.SignIn.route) {
            SignInScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
    }
}
