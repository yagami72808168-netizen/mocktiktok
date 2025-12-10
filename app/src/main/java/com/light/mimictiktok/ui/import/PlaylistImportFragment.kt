package com.light.mimictiktok.ui.import

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.light.mimictiktok.ui.theme.MimictiktokTheme
import com.light.mimictiktok.util.MediaPicker

/**
 * Fragment for importing videos into playlists using Jetpack Compose.
 */
class PlaylistImportFragment : Fragment() {

    private lateinit var viewModel: PlaylistImportViewModel
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var videoPickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[PlaylistImportViewModel::class.java]

        // Register permission launcher
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                launchVideoPicker()
            }
        }

        // Register video picker launcher
        videoPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            result.data?.let { intent ->
                val uris = MediaPicker.extractUrisFromResult(intent)
                viewModel.onVideosSelected(uris)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MimictiktokTheme {
                    ImportScreen(
                        viewModel = viewModel,
                        onSelectVideosClick = { checkPermissionsAndLaunchPicker() },
                        onScanAllClick = { checkPermissionsAndScanAll() }
                    )
                }
            }
        }
    }

    private fun checkPermissionsAndLaunchPicker() {
        if (viewModel.hasPermissions()) {
            launchVideoPicker()
        } else {
            permissionLauncher.launch(viewModel.getRequiredPermissions())
        }
    }

    private fun checkPermissionsAndScanAll() {
        if (viewModel.hasPermissions()) {
            viewModel.scanAndImportAllVideos()
        } else {
            permissionLauncher.launch(viewModel.getRequiredPermissions())
        }
    }

    private fun launchVideoPicker() {
        val intent = MediaPicker.createPickerIntent()
        videoPickerLauncher.launch(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: PlaylistImportViewModel,
    onSelectVideosClick: () -> Unit,
    onScanAllClick: () -> Unit
) {
    val importState by viewModel.importState.collectAsState()
    val selectedVideos by viewModel.selectedVideos.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    var playlistName by remember { mutableStateOf("Imported Videos") }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Videos") },
                actions = {
                    if (selectedVideos.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear selection")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedVideos.isNotEmpty() && importState !is PlaylistImportViewModel.ImportState.Loading) {
                FloatingActionButton(
                    onClick = { showPlaylistDialog = true }
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Import")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSelectVideosClick,
                    modifier = Modifier.weight(1f),
                    enabled = importState !is PlaylistImportViewModel.ImportState.Loading
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Videos")
                }

                Button(
                    onClick = onScanAllClick,
                    modifier = Modifier.weight(1f),
                    enabled = importState !is PlaylistImportViewModel.ImportState.Loading
                ) {
                    Text("Scan All")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // State display
            when (val state = importState) {
                is PlaylistImportViewModel.ImportState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (importProgress.total > 0) {
                            "Processing ${importProgress.current} of ${importProgress.total} (${importProgress.percentage}%)"
                        } else {
                            "Loading..."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (importProgress.total > 0) {
                        LinearProgressIndicator(
                            progress = { importProgress.percentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
                is PlaylistImportViewModel.ImportState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = state.message,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                is PlaylistImportViewModel.ImportState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = state.message,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                is PlaylistImportViewModel.ImportState.PartialSuccess -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = state.message,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                is PlaylistImportViewModel.ImportState.Idle -> {
                    // Show nothing
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected videos list
            if (selectedVideos.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Selected Videos (${selectedVideos.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(selectedVideos) { uri ->
                                VideoItem(uri = uri)
                            }
                        }
                    }
                }
            }
        }

        // Playlist name dialog
        if (showPlaylistDialog) {
            PlaylistNameDialog(
                currentName = playlistName,
                onNameChange = { playlistName = it },
                onDismiss = { showPlaylistDialog = false },
                onConfirm = {
                    showPlaylistDialog = false
                    viewModel.importVideos(playlistName)
                }
            )
        }
    }
}

@Composable
fun VideoItem(uri: Uri) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = uri.lastPathSegment ?: "Unknown",
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun PlaylistNameDialog(
    currentName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Playlist Name") },
        text = {
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text("Playlist Name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = currentName.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
