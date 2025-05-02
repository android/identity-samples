package com.androidauth.shrineWear.ui

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder

object ShrineDestinations {
    const val HOME_ROUTE = "home"
    const val LEGACY_LOGIN_ROUTE = "legacy_login"
    const val OAUTH_ROUTE = "oauth_login"
    const val LEGACY_SIWG_ROUTE = "legacy_siwg"
    const val SIGN_OUT_ROUTE = "sign_out"
}

class ShrineNavActions(navController: NavHostController) {
    private fun NavOptionsBuilder.defaultNavOptions() {
        popUpTo(0)
        launchSingleTop = true
    }

    val navigateToHome: () -> Unit = {
        navController.navigate(ShrineDestinations.HOME_ROUTE) { defaultNavOptions() }
    }
    val navigateToLegacyLogin: () -> Unit = {
        navController.navigate(ShrineDestinations.LEGACY_LOGIN_ROUTE) { defaultNavOptions() }
    }
    val navigateToOAuth: () -> Unit = {
        navController.navigate(ShrineDestinations.OAUTH_ROUTE) { defaultNavOptions() }
    }
    val navigateToLegacySignInWithGoogle: () -> Unit = {
        navController.navigate(ShrineDestinations.LEGACY_SIWG_ROUTE) { defaultNavOptions() }
    }
    val navigateToSignOut: () -> Unit = {
        navController.navigate(ShrineDestinations.SIGN_OUT_ROUTE) { defaultNavOptions() }
    }
}