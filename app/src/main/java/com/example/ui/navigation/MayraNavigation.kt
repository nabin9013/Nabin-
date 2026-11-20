package com.example.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.components.CosmicBackdrop
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AdvancedSettingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceHubScreen
import com.example.viewmodel.MayraUiState
import com.example.viewmodel.MayraViewModel
import kotlinx.serialization.Serializable

@Serializable
data object VoiceRoute

@Serializable
data object SettingsRoute

@Serializable
data object AdvancedSettingsRoute

@Serializable
data object AboutRoute

private data class TopLevelTab<T : Any>(
    val route: T,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun MayraAppShell(
    uiState: MayraUiState,
    viewModel: MayraViewModel,
    hasMicPermission: Boolean,
    hasContactsPermission: Boolean,
    hasCallPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestPermissions: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isAdvancedScreen = currentDestination?.hasRoute<AdvancedSettingsRoute>() == true

    val tabs = listOf(
        TopLevelTab(
            route = VoiceRoute,
            labelRes = R.string.nav_voice,
            selectedIcon = Icons.Rounded.Mic,
            unselectedIcon = Icons.Outlined.MicNone,
            testTag = "nav_voice"
        ),
        TopLevelTab(
            route = SettingsRoute,
            labelRes = R.string.nav_settings,
            selectedIcon = Icons.Rounded.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            testTag = "nav_settings"
        ),
        TopLevelTab(
            route = AboutRoute,
            labelRes = R.string.nav_about,
            selectedIcon = Icons.Rounded.Info,
            unselectedIcon = Icons.Outlined.Info,
            testTag = "nav_about"
        )
    )

    val navigateToTab: (Any) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    CosmicBackdrop(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useRail && !isAdvancedScreen) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            tonalElevation = 4.dp
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentDestination?.hasRoute(tab.route::class) == true
                                val label = stringResource(id = tab.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag(tab.testTag),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail && !isAdvancedScreen) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentDestination?.hasRoute(tab.route::class) == true
                                val label = stringResource(id = tab.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag(tab.testTag),
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = VoiceRoute,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable<VoiceRoute> {
                            VoiceHubScreen(
                                uiState = uiState,
                                hasMicPermission = hasMicPermission,
                                hasAllDevicePermissions = hasContactsPermission && hasCallPermission,
                                onRequestPermissions = onRequestPermissions,
                                onToggleAssistant = { enabled ->
                                    viewModel.toggleAssistantEnabled(enabled, hasMicPermission)
                                },
                                onOrbPrimaryAction = {
                                    viewModel.onOrbPrimaryAction(hasMicPermission)
                                },
                                onStopLiveSession = {
                                    viewModel.stopLiveSession()
                                },
                                onInterruptPlayback = {
                                    viewModel.interruptPlayback()
                                },
                                onOpenSettings = {
                                    navigateToTab(SettingsRoute)
                                },
                                onOpenAbout = {
                                    navigateToTab(AboutRoute)
                                }
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(
                                uiState = uiState,
                                hasMicPermission = hasMicPermission,
                                onRequestPermissions = onRequestPermissions,
                                onNavigateBack = {
                                    navigateToTab(VoiceRoute)
                                },
                                onToggleAssistant = { enabled ->
                                    viewModel.toggleAssistantEnabled(enabled, hasMicPermission)
                                },
                                onToggleNotifications = viewModel::setNotificationsEnabled,
                                onSelectVoice = viewModel::setVoiceName,
                                onSelectLanguage = viewModel::setLanguageStyle,
                                onSelectSpeedPreset = viewModel::setSpeechSpeedPreset,
                                onSelectTheme = viewModel::setThemeVariant,
                                onOpenAdvancedSettings = {
                                    navController.navigate(AdvancedSettingsRoute)
                                },
                                onOpenAbout = {
                                    navigateToTab(AboutRoute)
                                },
                                onRateApp = viewModel::shareMayraApp
                            )
                        }

                        composable<AdvancedSettingsRoute> {
                            AdvancedSettingsScreen(
                                uiState = uiState,
                                hasMicPermission = hasMicPermission,
                                onRequestPermissions = onRequestPermissions,
                                onSelectModel = viewModel::setModelName,
                                onSelectMaxTokens = viewModel::setMaxOutputTokens,
                                onSelectVoice = viewModel::setVoiceName,
                                onChangeSpeechRate = viewModel::setSpeechRateFloat,
                                onToggleInstantMode = viewModel::setInstantModeEnabled,
                                onToggleWakeWord = { enabled ->
                                    viewModel.toggleWakeWord(enabled, hasMicPermission)
                                },
                                onToggleEchoCancellation = viewModel::setEchoCancellationEnabled,
                                onChangeSensitivity = viewModel::setWakeSensitivity,
                                onOpenBatterySettings = viewModel::openBatteryOptimizationSettings,
                                onResetDefaults = viewModel::resetSettingsToDefaults,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable<AboutRoute> {
                            AboutScreen(
                                hasMicPermission = hasMicPermission,
                                hasContactsPermission = hasContactsPermission,
                                hasCallPermission = hasCallPermission,
                                hasNotificationPermission = hasNotificationPermission,
                                onRequestPermissions = onRequestPermissions,
                                onOpenSystemAppSettings = viewModel::openAppSettings
                            )
                        }
                    }
                }
            }
        }
    }
}
