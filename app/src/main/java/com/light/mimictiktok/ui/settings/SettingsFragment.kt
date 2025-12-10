package com.light.mimictiktok.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.light.mimictiktok.data.preferences.SettingsPreferences
import com.light.mimictiktok.util.PermissionManager

class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var permissionManager: PermissionManager
    private val permissionGrantedState = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionGrantedState.value = permissionManager.hasStoragePermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = PermissionManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    SettingsScreen(
                        viewModel = viewModel,
                        hasPermission = permissionGrantedState.value,
                        onRequestPermission = { requestPermissions() },
                        onOpenSettings = { openAppSettings() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionGrantedState.value = permissionManager.hasStoragePermission()
    }
    
    private fun requestPermissions() {
        requestPermissionLauncher.launch(permissionManager.getRequiredPermissions())
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val videoQuality by viewModel.videoQuality.collectAsState()
    val cacheSize by viewModel.cacheSizeLimit.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Video Quality", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
             listOf(SettingsPreferences.QUALITY_HIGH, SettingsPreferences.QUALITY_MEDIUM, SettingsPreferences.QUALITY_LOW).forEach { quality ->
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     RadioButton(
                         selected = (quality == videoQuality),
                         onClick = { viewModel.setVideoQuality(quality) }
                     )
                     Text(quality)
                 }
             }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Cache Size Limit: ${cacheSize}MB", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = cacheSize.toFloat(),
            onValueChange = { viewModel.setCacheSizeLimit(it.toLong()) },
            valueRange = 100f..2000f,
            steps = 19
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Permissions", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (hasPermission) {
            Text("Storage Permission Granted ✅", color = MaterialTheme.colorScheme.primary)
        } else {
            Text("Storage Permission Required ❌", color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = onRequestPermission) {
                    Text("Grant Permission")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onOpenSettings) {
                    Text("Open Settings")
                }
            }
        }
    }
}
