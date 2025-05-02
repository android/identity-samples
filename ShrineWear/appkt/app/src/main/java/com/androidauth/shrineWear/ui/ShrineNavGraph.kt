package com.androidauth.shrineWear.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.horologist.auth.ui.googlesignin.signin.GoogleSignInScreen

@Composable
fun ShrineNavGraph(
  navController: NavHostController = rememberNavController(),
  startDestination: String = ShrineDestinations.HOME_ROUTE,
  navigationActions: ShrineNavActions,
) {
  NavHost(
    navController = navController,
    startDestination = startDestination,
  ) {
    val credentialManagerViewModel = CredentialManagerViewModel()
    composable(ShrineDestinations.HOME_ROUTE) {
      HomeScreen(
        credentialManagerViewModel = credentialManagerViewModel,
        navigateToLegacyLogin = navigationActions.navigateToLegacyLogin,
        navigateToSignOut = navigationActions.navigateToSignOut
      )
    }
    composable(ShrineDestinations.LEGACY_LOGIN_ROUTE) {
      LegacyLoginScreen(
        navigateToOAuth = navigationActions.navigateToOAuth,
        navigateToSignInWithGoogle = navigationActions.navigateToLegacySignInWithGoogle,
        navigateToHome = navigationActions.navigateToHome,
        isCredentialManagerGSI = isCredentialManagerAvailable()
        )
    }
    composable(ShrineDestinations.OAUTH_ROUTE) {
      OAuthScreen(
        oAuthViewModel = OAuthViewModel(LocalContext.current.applicationContext as Application),
        navigateToSignOut = navigationActions.navigateToSignOut,
        navigateToLegacyLogin = navigationActions.navigateToLegacyLogin
      )
    }
    composable(ShrineDestinations.LEGACY_SIWG_ROUTE) {
      assert(!isCredentialManagerAvailable())

      GoogleSignInScreen(
        onAuthCancelled = { navigationActions.navigateToLegacyLogin() },
        onAuthSucceed = { navigationActions.navigateToSignOut() },
        viewModel = viewModel(factory = LegacySignInWithGoogleViewModelFactory),
        )
    }
    composable(ShrineDestinations.SIGN_OUT_ROUTE) {
      SignOutScreen(navigateToHome = navigationActions.navigateToHome)
    }
  }
}

fun isCredentialManagerAvailable(): Boolean {
  return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM
}