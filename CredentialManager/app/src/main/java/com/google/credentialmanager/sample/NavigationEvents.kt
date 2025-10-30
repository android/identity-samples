package com.google.credentialmanager.sample

/**
 * A sealed class that represents navigation events in the application.
 */
sealed class NavigationEvent {
    /**
     * A navigation event that navigates to the home screen.
     *
     * @param signedInWithPasskeys True if the user signed in with passkeys, false otherwise.
     */
    data class NavigateToHome(val signedInWithPasskeys: Boolean) : NavigationEvent()
}
