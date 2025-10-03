package com.google.credentialmanager.sample

sealed class NavigationEvent {
    data class NavigateToHome(val signedInWithPasskeys: Boolean) : NavigationEvent()
}
