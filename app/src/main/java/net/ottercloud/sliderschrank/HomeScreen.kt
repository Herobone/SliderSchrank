/*
 * Copyright (c) 2025 OtterCloud
 *
 * Redistribution and use in source and binary forms, with or without * modification, are permitted provided that the following conditions are met:
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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.ottercloud.sliderschrank.ui.theme.SliderSchrankTheme

private val categoryOrder = listOf(
    GarmentType.HEAD,
    GarmentType.TOP,
    GarmentType.BOTTOM,
    GarmentType.FEET
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val groupedGarments = remember { dummyGarments.groupBy { it.type } }

    val scope = rememberCoroutineScope()

    val pagerStates = categoryOrder.associateWith { category ->
        val garmentsForCategory = groupedGarments[category].orEmpty()
        rememberPagerState(pageCount = { garmentsForCategory.size })
    }

    var lockedGarmentIds by remember { mutableStateOf(emptySet<Int>()) }

    val onLockClick: (Int) -> Unit = remember {
        { garmentId ->
            lockedGarmentIds = if (lockedGarmentIds.contains(garmentId)) {
                lockedGarmentIds - garmentId
            } else {
                lockedGarmentIds + garmentId
            }
        }
    }

    val onShuffleClick: () -> Unit =
        remember(scope, pagerStates, groupedGarments, lockedGarmentIds) {
            {
                performUiShuffle(
                    scope = scope,
                    pagerStates = pagerStates,
                    groupedGarments = groupedGarments,
                    lockedGarmentIds = lockedGarmentIds
                )
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Dein Outfit", style = MaterialTheme.typography.titleLarge)
            Row {
                IconButton(onClick = onShuffleClick) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Zufälliges Outfit")
                }
                IconButton(onClick = { /* TODO: Save as Favourite Action */ }) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Outfit speichern")
                }
            }
        }

        // Garment sliders
        Column(
            modifier = Modifier
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            categoryOrder.forEach { category ->
                val garmentsForCategory = groupedGarments[category].orEmpty()
                val pagerState = pagerStates[category]
                val weight = when (category) {
                    GarmentType.HEAD, GarmentType.FEET -> 0.2f // 20%
                    else -> 0.3f // 30%
                }

                if (garmentsForCategory.isNotEmpty() && pagerState != null) {
                    val currentGarment = garmentsForCategory.getOrNull(pagerState.currentPage)
                    val isCurrentItemLocked = currentGarment?.id in lockedGarmentIds

                    GarmentSlider(
                        garments = garmentsForCategory,
                        pagerState = pagerState,
                        isSwipeEnabled = !isCurrentItemLocked,
                        lockedGarmentIds = lockedGarmentIds,
                        onLockClick = onLockClick,
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GarmentSlider(
    garments: List<Garment>,
    pagerState: PagerState,
    isSwipeEnabled: Boolean,
    lockedGarmentIds: Set<Int>,
    onLockClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = isSwipeEnabled,
            key = { page -> garments[page].id },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) { page ->
            val garment = garments[page]

            val itemOnLockClick = remember(garment.id) {
                { onLockClick(garment.id) }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GarmentItem(
                    garment = garment,
                    isLocked = lockedGarmentIds.contains(garment.id),
                    onLockClick = itemOnLockClick
                )
            }
        }
    }
}

@Composable
fun GarmentItem(
    garment: Garment,
    isLocked: Boolean,
    onLockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalPadding = when (garment.type) {
        GarmentType.HEAD, GarmentType.FEET -> 32.dp
        else -> 16.dp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = verticalPadding)
    ) {
        Card(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(garment.name)
            }
        }
        IconButton(
            onClick = onLockClick,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Teil sperren",
                tint = if (isLocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SliderSchrankTheme {
        HomeScreen()
    }
}