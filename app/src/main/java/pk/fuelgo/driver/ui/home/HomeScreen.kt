package pk.fuelgo.driver.ui.home

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import pk.fuelgo.driver.data.local.ThemeMode
import pk.fuelgo.driver.ui.config.ApiConfigDialog
import pk.fuelgo.driver.ui.fuelGoViewModel
import pk.fuelgo.driver.ui.jobs.JobDetailScreen
import pk.fuelgo.driver.ui.jobs.JobsScreen
import pk.fuelgo.driver.ui.navigation.Routes
import pk.fuelgo.driver.ui.notifications.NotificationViewModel
import pk.fuelgo.driver.ui.notifications.NotificationsScreen
import pk.fuelgo.driver.ui.profile.ProfileScreen
import pk.fuelgo.driver.ui.proof.ProofScreen
import pk.fuelgo.driver.ui.rememberAppContainer
import pk.fuelgo.driver.ui.shift.ShiftScreen
import pk.fuelgo.driver.ui.theme.FuelAction
import pk.fuelgo.driver.ui.theme.FuelPrimary

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.JOBS, "Jobs", Icons.Filled.Route),
    BottomTab(Routes.ACTIVE, "Active", Icons.Filled.PinDrop),
    BottomTab(Routes.SHIFT, "Shift", Icons.Filled.AccessTime),
    BottomTab(Routes.PROFILE, "Profile", Icons.Filled.Person),
)

private const val ORDER_ID_ARG = "orderId"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onLoggedOut: () -> Unit) {
    val navController = rememberNavController()
    var showApiConfigDialog by remember { mutableStateOf(false) }

    val container = rememberAppContainer()
    val scope = rememberCoroutineScope()
    val themeMode by container.themePreferenceManager.modeFlow.collectAsState(initial = ThemeMode.System)
    val isDark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val notificationViewModel = fuelGoViewModel { NotificationViewModel(it.notificationRepository) }
    val notificationState by notificationViewModel.uiState.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = bottomTabs.any { it.route == currentRoute }

    fun goToJobs() {
        navController.navigate(Routes.JOBS) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        topBar = {
            val title = when (currentRoute) {
                Routes.NOTIFICATIONS -> "Notifications"
                Routes.JOB_DETAIL -> "Job details"
                Routes.PROOF -> "Proof of delivery"
                else -> bottomTabs.firstOrNull { it.route == currentRoute }?.label ?: "FuelGo Driver"
            }
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FuelPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                actions = {
                    IconButton(onClick = {
                        scope.launch { container.themePreferenceManager.setMode(if (isDark) ThemeMode.Light else ThemeMode.Dark) }
                    }) {
                        Icon(
                            if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (isDark) "Switch to light theme" else "Switch to dark theme",
                        )
                    }
                    if (currentRoute != Routes.NOTIFICATIONS) {
                        IconButton(onClick = { navController.navigate(Routes.NOTIFICATIONS) { launchSingleTop = true } }) {
                            BadgedBox(badge = {
                                if (notificationState.unreadCount > 0) {
                                    Badge(containerColor = FuelAction) { Text(notificationState.unreadCount.coerceAtMost(99).toString()) }
                                }
                            }) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                bottomTabs.forEach { tab ->
                    val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FuelAction,
                            selectedTextColor = FuelAction,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.JOBS,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.JOBS) {
                JobsScreen(
                    onOpenJob = { navController.navigate(Routes.jobDetail(it)) },
                    onOpenProof = { navController.navigate(Routes.proof(it)) },
                )
            }
            composable(Routes.ACTIVE) {
                JobDetailScreen(
                    orderId = null,
                    onOpenProof = { navController.navigate(Routes.proof(it)) },
                    onGoToJobs = ::goToJobs,
                )
            }
            composable(Routes.SHIFT) { ShiftScreen() }
            composable(Routes.PROFILE) {
                ProfileScreen(onLoggedOut = onLoggedOut, onOpenServerSettings = { showApiConfigDialog = true })
            }
            composable(Routes.NOTIFICATIONS) { NotificationsScreen(notificationViewModel, notificationState) }
            composable(Routes.JOB_DETAIL, arguments = listOf(navArgument(ORDER_ID_ARG) { type = NavType.StringType })) { entry ->
                JobDetailScreen(
                    orderId = entry.arguments?.getString(ORDER_ID_ARG),
                    onOpenProof = { navController.navigate(Routes.proof(it)) },
                    onGoToJobs = { navController.popBackStack() },
                )
            }
            composable(Routes.PROOF, arguments = listOf(navArgument(ORDER_ID_ARG) { type = NavType.StringType })) { entry ->
                ProofScreen(
                    orderId = entry.arguments?.getString(ORDER_ID_ARG).orEmpty(),
                    onDone = { navController.popBackStack(Routes.JOBS, inclusive = false) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }

    if (showApiConfigDialog) {
        ApiConfigDialog(onDismiss = { showApiConfigDialog = false })
    }
}
