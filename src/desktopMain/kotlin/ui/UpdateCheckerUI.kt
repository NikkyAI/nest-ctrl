package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrowserUpdated
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.platformtools.appmanager.getAppInstaller
import io.github.kdroidfilter.platformtools.permissionhandler.hasInstallPermission
import io.github.kdroidfilter.platformtools.permissionhandler.requestInstallPermission
import io.github.kdroidfilter.platformtools.releasefetcher.downloader.Downloader
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.components.WithTooltipAbove
import java.io.File
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

@Composable
fun UpdateCheckerUI(fetcher: GitHubReleaseFetcher) {

    var latestVersion by remember { mutableStateOf<String?>(null) }
    var changelog by remember { mutableStateOf<String?>(null) }

    var isChecking by remember { mutableStateOf(false) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0.0) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    var isInstalling by remember { mutableStateOf(false) }
    var installMessage by remember { mutableStateOf<String?>(null) }

    var showUpdateAvailableDialog by remember { mutableStateOf(false) }
    var showNoUpdateDialog by remember { mutableStateOf(false) }
    var showDownloadProgressDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    val installer = getAppInstaller()

    fun checkForUpdates(fetcher: GitHubReleaseFetcher) {
        isChecking = true

        CoroutineScope(Dispatchers.IO).launch {
            fetcher.checkForUpdate { version, notes ->
                logger.info { "updateChecker: $version, $notes" }
                isChecking = false
                latestVersion = version
                changelog = notes
                showUpdateAvailableDialog = true
                showNoUpdateDialog = false
            }
            isChecking = false
        }
    }

//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {

//        Text(
//            text = "Application Update",
//            style = MaterialTheme.typography.h4,
//            color = MaterialTheme.colors.primary,
//            textAlign = TextAlign.Center
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))

        WithTooltipAbove(
            tooltip = { Text("Check for update") }
        ) {
            IconButton(
                onClick = {

                    if (!hasInstallPermission()) {
                        showPermissionDialog = true
                    } else {
                        checkForUpdates(fetcher)
                    }
                },
                enabled = !isChecking
            ) {
                Icon(Icons.Outlined.BrowserUpdated, "update")
            }
        }
//        Button(
//            onClick = {
//
//                if (!hasInstallPermission()) {
//                    showPermissionDialog = true
//                } else {
//                    checkForUpdates(fetcher)
//                }
//            },
//            enabled = !isChecking && !isInstalling && !isDownloading,
//        ) {
//            if (isChecking) {
//                CircularProgressIndicator(
//                    modifier = Modifier.size(24.dp),
//                    strokeWidth = 2.dp,
//                    color = MaterialTheme.colors.onPrimary
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text("Checking updates…", style = MaterialTheme.typography.body1)
//            } else {
//                Text("Check for updates", style = MaterialTheme.typography.body1)
//            }
//        }

        if (showPermissionDialog) {
            AlertDialog(
                backgroundColor = MaterialTheme.colors.background,
                onDismissRequest = {
                    showPermissionDialog = false
                },
                title = {
                    Text("Permission Required")
                },
                text = {
                    Text("This action requires install permissions. Please grant the necessary permissions to proceed.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionDialog = false
                            requestInstallPermission(
                                onGranted = {
                                    checkForUpdates(fetcher)
                                },
                                onDenied = {
                                    installMessage = "Permission denied. Unable to check for updates."
                                }
                            )
                        }
                    ) {
                        Text("Grant Permission")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showPermissionDialog = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showUpdateAvailableDialog && latestVersion != null) {
            AlertDialog(
//                backgroundColor = MaterialTheme.colors.background,
                onDismissRequest = {
                    showUpdateAvailableDialog = false
                },
                title = {
                    Text("New version available: $latestVersion")
                },
                text = {
                    Text("Changelog:\n$changelog")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showUpdateAvailableDialog = false
                            showDownloadProgressDialog = true
                            CoroutineScope(Dispatchers.IO).launch {
                                isDownloading = true
                                val release = fetcher.getLatestRelease()
                                val downloadLink = release?.let { fetcher.getDownloadLinkForPlatform(it) }
                                if (downloadLink != null) {
                                    val downloader = Downloader()
                                    downloader.downloadApp(downloadLink) { progress, file ->
                                        downloadProgress = progress
                                        if (progress >= 100.0) {
                                            downloadedFile = file
                                            isDownloading = false
                                        }
                                    }
                                } else {
                                    isDownloading = false
                                }
                            }
                        },
                        enabled = !isDownloading && !isInstalling
                    ) {
                        Text("Download & Install")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showUpdateAvailableDialog = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showNoUpdateDialog) {
            AlertDialog(
                onDismissRequest = {
                    showNoUpdateDialog = false
                },
                title = {
                    Text("No updates available")
                },
                text = {
                    Text("You are already using the latest version of the application.")
                },
                confirmButton = {
                    Button(onClick = {
                        showNoUpdateDialog = false
                    }) {
                        Text("OK")
                    }
                }
            )
        }

        if (showDownloadProgressDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDownloadProgressDialog = false
                },
                title = {
                    Text(if (isDownloading) "Downloading…" else "Download Complete")
                },
                text = {
                    if (isDownloading) {
                        Column {
                            Text("Downloading update, please wait.")
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (downloadProgress / 100).toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Progress: ${downloadProgress.toInt()}%")
                        }
                    } else {
                        Text("The update has been downloaded and is ready to install.")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDownloadProgressDialog = false
                            CoroutineScope(Dispatchers.IO).launch {
                                isInstalling = true
                                downloadedFile?.let { file ->
                                    installer.installApp(file) { success, message ->
                                        installMessage = if (success) {
                                            showRestartDialog = true
                                            null
                                        } else {
                                            "Installation failed: $message"
                                        }
                                        isInstalling = false
                                    }
                                }
                            }
                        },
                        enabled = downloadedFile != null && !isInstalling
                    ) {
                        Text("Install Now")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showDownloadProgressDialog = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showRestartDialog) {
            AlertDialog(
                onDismissRequest = {
                    showRestartDialog = false
                },
                title = {
                    Text("Restart Application")
                },
                text = {
                    Text("The application needs to be restarted to apply the update.")
                },
                confirmButton = {
                    Button(onClick = {
                        exitProcess(0)
                        //TODO The restartApplication() function not work after an update
                    }) {
                        Text("Quit Now")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showRestartDialog = false
                    }) {
                        Text("Later")
                    }
                }
            )
        }

        installMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, style = MaterialTheme.typography.body2, color = MaterialTheme.colors.secondary)
        }

        if (isInstalling) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator(color = MaterialTheme.colors.secondary)
        }
}