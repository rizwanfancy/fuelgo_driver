package pk.fuelgo.driver.config

/**
 * App-wide constants that are NOT expected to change per install.
 *
 * The FuelGo API server address and tenant slug are deliberately NOT here — they're
 * configured at runtime (persisted via DataStore) so the app works against whichever
 * backend the person testing it is running, without needing a rebuild. See
 * [pk.fuelgo.driver.data.local.ApiConfigManager] and the "API Configuration" screen,
 * reachable from the ⋮ menu on the Login screen or the app's top bar.
 */
object AppConfig {

    const val REQUEST_TIMEOUT_SECONDS: Long = 30
    /** How often the job queue / active job re-fetch while the screen is open. */
    const val JOB_POLL_INTERVAL_MS: Long = 15_000L
}
