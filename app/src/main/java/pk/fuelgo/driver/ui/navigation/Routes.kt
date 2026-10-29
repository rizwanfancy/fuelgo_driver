package pk.fuelgo.driver.ui.navigation

/** Central route registry for both the root nav graph and the Home bottom-nav graph. */
object Routes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val HOME = "home"

    // Bottom-nav destinations, nested inside HOME.
    const val JOBS = "jobs"
    const val ACTIVE = "active"
    const val SHIFT = "shift"
    const val PROFILE = "profile"

    // Pushed destinations (no bottom-nav highlight).
    const val NOTIFICATIONS = "notifications"
    const val JOB_DETAIL = "job/{orderId}"
    const val PROOF = "proof/{orderId}"

    fun jobDetail(orderId: String) = "job/$orderId"
    fun proof(orderId: String) = "proof/$orderId"
}
