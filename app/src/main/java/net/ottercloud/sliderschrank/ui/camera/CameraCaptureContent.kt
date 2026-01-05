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

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import net.ottercloud.sliderschrank.R
import net.ottercloud.sliderschrank.ui.controls.CloseButton

@Composable
internal fun CameraCaptureContent(
    onPreviewViewCreate: (PreviewView) -> Unit,
    isFlashEnabled: Boolean,
    onFlashToggle: () -> Unit,
    isCapturing: Boolean,
    onCapture: () -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                }.also { onPreviewViewCreate(it) }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 70.dp)
        )

        CloseButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Toggle Button
            IconButton(
                onClick = onFlashToggle,
                modifier = Modifier
                    .size(56.dp)
                    .background(color = Color.Black.copy(alpha = 0.5f), shape = CircleShape)
            ) {
                Icon(
                    imageVector = if (isFlashEnabled) {
                        Icons.Default.FlashOn
                    } else {
                        Icons.Default.FlashOff
                    },
                    contentDescription = if (isFlashEnabled) {
                        stringResource(R.string.flash_on)
                    } else {
                        stringResource(R.string.flash_off)
                    },
                    tint = if (isFlashEnabled) Color.Yellow else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Capture Button
            IconButton(
                onClick = onCapture,
                enabled = !isCapturing,
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = if (isCapturing) Color.Gray else Color.White,
                        shape = CircleShape
                    )
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = stringResource(R.string.take_picture),
                    tint = if (isCapturing) Color.Gray else Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = if (isCapturing) Color.Gray else Color.White,
                            shape = CircleShape
                        )
                )
            }

            // Placeholder for symmetry
            Box(modifier = Modifier.size(56.dp))
        }
    }
}