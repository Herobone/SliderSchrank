/*
 * Copyright (c) 2025 OtterCloud
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.ottercloud.sliderschrank

import android.Manifest
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import net.ottercloud.sliderschrank.ui.camera.BackgroundRemovalErrorDialog
import net.ottercloud.sliderschrank.ui.camera.CameraInitErrorDialog
import net.ottercloud.sliderschrank.ui.camera.CaptureErrorDialog
import net.ottercloud.sliderschrank.ui.camera.FullscreenCameraView
import net.ottercloud.sliderschrank.ui.camera.PermissionDeniedDialog
import net.ottercloud.sliderschrank.ui.camera.PermissionPermanentlyDeniedDialog
import net.ottercloud.sliderschrank.ui.camera.SaveErrorDialog
import net.ottercloud.sliderschrank.ui.theme.SliderSchrankTheme

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(onImageSave: (android.net.Uri) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("camera_prefs", Context.MODE_PRIVATE)

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showPermissionPermanentlyDeniedDialog by remember { mutableStateOf(false) }
    var showSaveErrorDialog by remember { mutableStateOf(false) }
    var showCameraInitErrorDialog by remember { mutableStateOf(false) }
    var showCaptureErrorDialog by remember { mutableStateOf(false) }
    var showBackgroundRemovalErrorDialog by remember { mutableStateOf(false) }
    var showFullscreenCamera by remember { mutableStateOf(false) }
    var permissionRequestLaunched by remember { mutableStateOf(false) }
    var permissionPermanentlyDenied by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            resetPermissionFlag(context)
        }
    }

    LaunchedEffect(cameraPermissionState.status) {
        val hasAskedBefore = prefs.getBoolean("camera_permission_asked", false)
        permissionPermanentlyDenied = hasAskedBefore &&
            !cameraPermissionState.status.isGranted &&
            !cameraPermissionState.status.shouldShowRationale

        if (!permissionRequestLaunched) return@LaunchedEffect

        when {
            cameraPermissionState.status.isGranted -> {
                permissionRequestLaunched = false
            }
            cameraPermissionState.status.shouldShowRationale -> {
                showPermissionDeniedDialog = true
                permissionRequestLaunched = false
            }
            else -> {
                showPermissionPermanentlyDeniedDialog = true
                permissionRequestLaunched = false
            }
        }
    }

    // Dialogs
    if (showPermissionDeniedDialog) {
        PermissionDeniedDialog(onDismiss = { showPermissionDeniedDialog = false })
    }

    if (showPermissionPermanentlyDeniedDialog) {
        PermissionPermanentlyDeniedDialog(
            context = context,
            onDismiss = { showPermissionPermanentlyDeniedDialog = false }
        )
    }

    if (showSaveErrorDialog) {
        SaveErrorDialog(onDismiss = { showSaveErrorDialog = false })
    }

    if (showCameraInitErrorDialog) {
        CameraInitErrorDialog(onDismiss = { showCameraInitErrorDialog = false })
    }

    if (showCaptureErrorDialog) {
        CaptureErrorDialog(onDismiss = { showCaptureErrorDialog = false })
    }

    if (showBackgroundRemovalErrorDialog) {
        BackgroundRemovalErrorDialog(onDismiss = { showBackgroundRemovalErrorDialog = false })
    }

    // Fullscreen Camera Dialog
    if (showFullscreenCamera) {
        FullscreenCameraView(
            onClose = { showFullscreenCamera = false },
            onImageSave = { uri ->
                onImageSave(uri)
            },
            onSaveError = { showSaveErrorDialog = true },
            onCameraInitError = {
                showFullscreenCamera = false
                showCameraInitErrorDialog = true
            },
            onCaptureError = { showCaptureErrorDialog = true },
            onBackgroundRemovalError = { showBackgroundRemovalErrorDialog = true }
        )
    }

    // Main Camera Screen
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Tutorial Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.camera_tutorial_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = stringResource(R.string.camera_tutorial_description),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Tip Cards
            PhotoTipCard(
                icon = Icons.Default.LightMode,
                title = stringResource(R.string.tip_lighting_title),
                description = stringResource(R.string.tip_lighting_description)
            )

            Spacer(modifier = Modifier.height(12.dp))

            PhotoTipCard(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.tip_background_title),
                description = stringResource(R.string.tip_background_description)
            )

            Spacer(modifier = Modifier.height(12.dp))

            PhotoTipCard(
                icon = Icons.Default.CameraAlt,
                title = stringResource(R.string.tip_camera_position_title),
                description = stringResource(R.string.tip_camera_position_description)
            )

            Spacer(modifier = Modifier.height(12.dp))

            PhotoTipCard(
                icon = Icons.Default.ViewDay,
                title = stringResource(R.string.tip_clothing_prep_title),
                description = stringResource(R.string.tip_clothing_prep_description)
            )

            Spacer(modifier = Modifier.height(12.dp))

            PhotoTipCard(
                icon = Icons.Default.CheckCircle,
                title = stringResource(R.string.tip_contrast_title),
                description = stringResource(R.string.tip_contrast_description)
            )

            Spacer(modifier = Modifier.height(12.dp))

            PhotoTipCard(
                icon = Icons.Default.CenterFocusStrong,
                title = stringResource(R.string.tip_centering_title),
                description = stringResource(R.string.tip_centering_description)
            )
        }

        // Camera Button
        Button(
            onClick = {
                if (cameraPermissionState.status.isGranted) {
                    showFullscreenCamera = true
                } else if (permissionPermanentlyDenied) {
                    showPermissionPermanentlyDeniedDialog = true
                } else if (cameraPermissionState.status.shouldShowRationale) {
                    permissionRequestLaunched = true
                    cameraPermissionState.launchPermissionRequest()
                } else {
                    val hasAskedBefore = prefs.getBoolean("camera_permission_asked", false)

                    if (!hasAskedBefore) {
                        prefs.edit { putBoolean("camera_permission_asked", true) }
                        permissionRequestLaunched = true
                        cameraPermissionState.launchPermissionRequest()
                    } else {
                        showPermissionPermanentlyDeniedDialog = true
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(top = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.take_picture),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun PhotoTipCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun resetPermissionFlag(context: Context) {
    context.getSharedPreferences("camera_prefs", Context.MODE_PRIVATE).edit {
        putBoolean("camera_permission_asked", false)
    }
}

@ComposePreview(showBackground = true)
@Composable
private fun CameraScreenPreview() {
    SliderSchrankTheme {
        CameraScreen({})
    }
}