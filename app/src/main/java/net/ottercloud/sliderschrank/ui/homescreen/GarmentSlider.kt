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
package net.ottercloud.sliderschrank.ui.homescreen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.ottercloud.sliderschrank.R
import net.ottercloud.sliderschrank.data.model.PieceWithDetails

private const val LAYER_SCALE_FACTOR = 0.15f
private const val LAYER_OFFSET_DP = 100

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GarmentSlider(
    garments: List<PieceWithDetails>,
    pagerState: PagerState,
    isSwipeEnabled: Boolean,
    lockedPieceIds: Set<Long>,
    onLockClick: (Long) -> Unit,
    onPieceClick: (PieceWithDetails) -> Unit,
    modifier: Modifier = Modifier,
    backgroundLayers: List<PieceWithDetails> = emptyList()
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val showCardBackground = backgroundLayers.isEmpty()

        // Render background layers
        backgroundLayers.forEachIndexed { index, layer ->
            val reverseIndex = backgroundLayers.size - index

            val scale = 1f - (reverseIndex * LAYER_SCALE_FACTOR)

            val translationOffset = (reverseIndex * LAYER_OFFSET_DP).dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        // Move left
                        translationX = -translationOffset.toPx()
                        translationY = 0f
                    }
            ) {
                // Background layers are just images (transparent background)
                GarmentContent(layer)
            }
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = isSwipeEnabled,
            key = { page ->
                val id = garments[page].piece.id
                if (id >= 0L) id else "empty-piece-$page"
            },
            modifier = Modifier
                .fillMaxSize()
        ) { page ->
            val garment = garments[page]

            val itemOnLockClick = remember(garment.piece.id) {
                { onLockClick(garment.piece.id) }
            }

            val itemOnPieceClick = remember(garment.piece.id) {
                { onPieceClick(garment) }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GarmentItem(
                    garment = garment,
                    isLocked = lockedPieceIds.contains(garment.piece.id),
                    onLockClick = itemOnLockClick,
                    onGarmentClick = itemOnPieceClick,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    showBackground = showCardBackground
                )
            }
        }
    }
}

@Composable
fun GarmentItem(
    garment: PieceWithDetails,
    isLocked: Boolean,
    onLockClick: () -> Unit,
    onGarmentClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
    showBackground: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .clickable(onClick = onGarmentClick)
    ) {
        if (showBackground) {
            Card(
                modifier = Modifier.fillMaxSize()
            ) {
                GarmentContent(garment)
            }
        } else {
            GarmentContent(garment)
        }

        IconButton(
            onClick = onLockClick,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = stringResource(R.string.lock_item),
                tint = if (isLocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                }
            )
        }
    }
}

@Composable
fun GarmentContent(garment: PieceWithDetails, modifier: Modifier = Modifier) {
    if (garment.piece.id == -1L) {
        // Display "None" for empty piece
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.none),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(garment.piece.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = garment.category?.name ?: garment.piece.id.toString(),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}