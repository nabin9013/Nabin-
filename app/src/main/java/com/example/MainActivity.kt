package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebase.configureAppCheck
import com.example.ui.navigation.MayraAppShell
import com.example.ui.theme.MayraTheme
import com.example.viewmodel.MayraViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or ViewModels.
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            val viewModel: MayraViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var permissionCheckTick by remember { mutableIntStateOf(0) }

            val requiredPermissions = remember {
                buildList {
                    add(Manifest.permission.RECORD_AUDIO)
                    add(Manifest.permission.READ_CONTACTS)
                    add(Manifest.permission.CALL_PHONE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) {
                permissionCheckTick++
            }

            val hasMicPermission = remember(permissionCheckTick) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
            }
            val hasContactsPermission = remember(permissionCheckTick) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED
            }
            val hasCallPermission = remember(permissionCheckTick) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) ==
                    PackageManager.PERMISSION_GRANTED
            }
            val hasNotificationPermission = remember(permissionCheckTick) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            }

            LaunchedEffect(hasMicPermission, uiState.settings.assistantEnabled) {
                if (hasMicPermission) {
                    viewModel.ensureBackgroundServiceRunning(true)
                }
            }

            MayraTheme(themeVariant = uiState.settings.themeVariant) {
                MayraAppShell(
                    uiState = uiState,
                    viewModel = viewModel,
                    hasMicPermission = hasMicPermission,
                    hasContactsPermission = hasContactsPermission,
                    hasCallPermission = hasCallPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestPermissions = {
                        permissionLauncher.launch(requiredPermissions)
                    }
                )
            }
        }
    }
}
