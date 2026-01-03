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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.ottercloud.sliderschrank.ui.theme.SliderSchrankTheme
import net.ottercloud.sliderschrank.util.LikeUtil
import net.ottercloud.sliderschrank.util.SettingsManager

private val categoryOrder = listOf(
    GarmentType.HEAD,
    GarmentType.TOP,
    GarmentType.BOTTOM,
    GarmentType.FEET
)

@OptIn(ExperimentalFoundationApi::class)
private class HomeScreenState(
    val groupedGarments: Map<GarmentType, List<Garment>>,
    val pagerStates: Map<GarmentType, PagerState>
) {
    var lockedGarmentIds by mutableStateOf(emptySet<Int>())
        private set
    val currentOutfitIds by derivedStateOf {
        pagerStates.mapNotNull { (category, pagerState) ->
            groupedGarments[category]?.getOrNull(pagerState.currentPage)?.id
        }.toSet()
    }

    val isCurrentOutfitSaved by derivedStateOf {
        LikeUtil.isFavorite(currentOutfitIds)
    }

    fun onLockClick(garmentId: Int) {
        lockedGarmentIds = if (lockedGarmentIds.contains(garmentId)) {
            lockedGarmentIds - garmentId
        } else {
            lockedGarmentIds + garmentId
        }
    }

    fun onGarmentClick(category: GarmentType) {
        println("Category $category was clicked")
    }

    fun onFavoriteClick() {
        LikeUtil.toggleFavorite(currentOutfitIds)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberHomeScreenState(
    groupedGarments: Map<GarmentType, List<Garment>> =
        remember { dummyGarments.groupBy { it.type } }
): HomeScreenState {
    val pagerStates = categoryOrder.associateWith { category ->
        val garmentsForCategory = groupedGarments[category].orEmpty()
        rememberPagerState(pageCount = { garmentsForCategory.size })
    }

    @OptIn(ExperimentalFoundationApi::class)
    return remember(groupedGarments, pagerStates) {
        HomeScreenState(
            groupedGarments = groupedGarments,
            pagerStates = pagerStates
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val background by settingsManager.background.collectAsState(initial = "Kork")

    rememberCoroutineScope()
    val state = rememberHomeScreenState()

    val scope = rememberCoroutineScope()

    val onShuffleClick: () -> Unit =
        remember(scope, state) {
            {
                performUiShuffle(
                    scope = scope,
                    pagerStates = state.pagerStates,
                    groupedGarments = state.groupedGarments,
                    lockedGarmentIds = state.lockedGarmentIds
                )
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        HomeScreenTopBar(
            isOutfitSaved = state.isCurrentOutfitSaved,
            onShuffleClick = onShuffleClick,
            onFavoriteClick = state::onFavoriteClick
        )

        Column(
            modifier = Modifier
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            categoryOrder.forEach { category ->
                val garmentsForCategory = state.groupedGarments[category].orEmpty()
                val pagerState = state.pagerStates[category]
                val weight = when (category) {
                    GarmentType.HEAD, GarmentType.FEET -> 0.2f
                    else -> 0.3f
                }

                if (garmentsForCategory.isNotEmpty() && pagerState != null) {
                    val currentGarment =
                        garmentsForCategory.getOrNull(pagerState.currentPage)
                    val isCurrentItemLocked = currentGarment?.id in state.lockedGarmentIds

                    GarmentSlider(
                        garments = garmentsForCategory,
                        pagerState = pagerState,
                        isSwipeEnabled = !isCurrentItemLocked,
                        lockedGarmentIds = state.lockedGarmentIds,
                        onLockClick = state::onLockClick,
                        onGarmentClick = state::onGarmentClick,
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreenTopBar(
    isOutfitSaved: Boolean,
    onShuffleClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(R.string.your_outfit), style = MaterialTheme.typography.titleLarge)
        Row {
            IconButton(onClick = onShuffleClick) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = stringResource(R.string.random_outfit)
                )
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isOutfitSaved) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = stringResource(R.string.save_outfit),
                    tint = if (isOutfitSaved) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
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
    onGarmentClick: (GarmentType) -> Unit,
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

            val itemOnGarmentClick = remember(garment.type) {
                { onGarmentClick(garment.type) }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GarmentItem(
                    garment = garment,
                    isLocked = lockedGarmentIds.contains(garment.id),
                    onLockClick = itemOnLockClick,
                    onGarmentClick = itemOnGarmentClick
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
    onGarmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onGarmentClick)
        ) {
            Image(
                painter = painterResource(id = garment.imageResId),
                contentDescription = garment.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
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

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SliderSchrankTheme {
        HomeScreen()
    }
}