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
package net.ottercloud.sliderschrank.ui.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.ottercloud.sliderschrank.R
import net.ottercloud.sliderschrank.ui.controls.CloseButton
import net.ottercloud.sliderschrank.ui.theme.KeepGreen
import net.ottercloud.sliderschrank.util.BackgroundRemovalProcessor

@Composable
internal fun PhotoPreviewContent(
    bitmap: Bitmap,
    onRetake: () -> Unit,
    onKeep: (originalBitmap: Bitmap, processedBitmap: Bitmap?) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backgroundRemovalProcessor = remember { BackgroundRemovalProcessor.getInstance() }

    var isBackgroundRemovalEnabled by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isOpenCVInitialized by remember { mutableStateOf(false) }

    // OpenCV initialisieren
    LaunchedEffect(Unit) {
        isOpenCVInitialized = backgroundRemovalProcessor.initializeOpenCV(context)
    }

    // Hintergrundentfernung verarbeiten wenn aktiviert
    LaunchedEffect(isBackgroundRemovalEnabled) {
        if (isBackgroundRemovalEnabled && isOpenCVInitialized) {
            isProcessing = true
            processedBitmap = backgroundRemovalProcessor.removeBackground(bitmap)
            isProcessing = false
        } else if (!isBackgroundRemovalEnabled) {
            processedBitmap = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Bildanzeige
        val displayBitmap = if (isBackgroundRemovalEnabled) {
            processedBitmap ?: bitmap
        } else {
            bitmap
        }
        Image(
            bitmap = displayBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 140.dp),
            contentScale = ContentScale.Fit
        )

        // Loading Overlay
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = KeepGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = stringResource(R.string.background_removal_processing),
                        color = Color.White,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }

        CloseButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )

        // Hintergrundentfernung Toggle
        if (isOpenCVInitialized) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.remove_background),
                    color = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = isBackgroundRemovalEnabled,
                    onCheckedChange = {
                        if (!isProcessing) {
                            isBackgroundRemovalEnabled = it
                        }
                    },
                    enabled = !isProcessing
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Retake Button
            IconButton(
                onClick = onRetake,
                modifier = Modifier
                    .size(64.dp)
                    .background(color = Color.White, shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.retake_photo),
                    tint = Color.Gray,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Keep Button - erweitert für duale Speicherung
            IconButton(
                onClick = {
                    onKeep(bitmap, processedBitmap)
                },
                modifier = Modifier
                    .size(64.dp)
                    .background(color = KeepGreen, shape = CircleShape),
                enabled = !isProcessing
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.keep_photo),
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}