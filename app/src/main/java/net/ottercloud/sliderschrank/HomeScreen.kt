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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.firstOrNull
import net.ottercloud.sliderschrank.data.AppDatabase
import net.ottercloud.sliderschrank.data.model.Slot
import net.ottercloud.sliderschrank.ui.homescreen.BackgroundContent
import net.ottercloud.sliderschrank.ui.homescreen.GarmentSliders
import net.ottercloud.sliderschrank.ui.homescreen.HomeScreenTopBar
import net.ottercloud.sliderschrank.ui.homescreen.PiecePickerDialog
import net.ottercloud.sliderschrank.ui.homescreen.rememberHomeScreenState
import net.ottercloud.sliderschrank.ui.theme.SliderSchrankTheme
import net.ottercloud.sliderschrank.util.SettingsManager
import net.ottercloud.sliderschrank.util.createFavoriteUtil
import net.ottercloud.sliderschrank.util.createGroupedPieces
import net.ottercloud.sliderschrank.util.createToggleFavoriteCallback
import net.ottercloud.sliderschrank.util.loadOutfitPieces
import net.ottercloud.sliderschrank.util.performUiShuffle
import net.ottercloud.sliderschrank.util.shouldLoadOutfit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier, loadOutfitId: Long? = null) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val settingsManager = remember { SettingsManager(context) }
    val background by settingsManager.background.collectAsState(initial = AppBackground.WHITE)
    val database = remember(context, isInPreview) {
        if (isInPreview) null else AppDatabase.getDatabase(context)
    }
    val scope = rememberCoroutineScope()

    val outfitDao = database?.outfitDao()
    val pieceDao = database?.pieceDao()
    val favoriteUtil = remember(outfitDao, pieceDao) {
        createFavoriteUtil(context, outfitDao, pieceDao)
    }

    val savedOutfits by favoriteUtil?.favoriteOutfitsWithPieces?.collectAsState(
        initial = emptyList()
    )
        ?: remember { mutableStateOf(emptyList()) }

    val pieces by database?.pieceDao()?.getAllPiecesWithDetails()?.collectAsState(
        initial = emptyList()
    )
        ?: remember { mutableStateOf(emptyList()) }

    val groupedPieces = remember(pieces) {
        createGroupedPieces(pieces)
    }

    val failedToSaveOutfitMessage = stringResource(R.string.failed_to_save_outfit_please_try_again)

    val onToggleFavorite = remember(scope, favoriteUtil) {
        createToggleFavoriteCallback(context, scope, favoriteUtil, failedToSaveOutfitMessage)
    }

    val state = rememberHomeScreenState(groupedPieces, savedOutfits)

    LaunchedEffect(loadOutfitId, pieces.isNotEmpty()) {
        if (shouldLoadOutfit(loadOutfitId, database, state.pagerStates, pieces)) {
            val outfitWithPieces = database!!.outfitDao().getOutfitWithPiecesById(
                loadOutfitId!!
            ).firstOrNull()

            outfitWithPieces?.let { outfit ->
                loadOutfitPieces(outfit, state)
            }
        }
    }

    val onShuffleClick = remember(scope, state) {
        {
            performUiShuffle(
                scope = scope,
                pagerStates = state.pagerStates,
                groupedPieces = state.groupedPieces,
                lockedPieceIds = state.lockedPieceIds
            )
        }
    }

    var selectedSlotForPicker by remember { mutableStateOf<Slot?>(null) }
    var showLayerDialog by remember { mutableStateOf(false) }
    var editingLayerIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        BackgroundContent(background)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            HomeScreenTopBar(
                isOutfitSaved = state.isCurrentOutfitSaved,
                onShuffleClick = onShuffleClick,
                onFavoriteClick = { state.onFavoriteClick(onToggleFavorite) }
            )

            GarmentSliders(
                state,
                onSelectSlot = { slot ->
                    if (slot.supportsLayers()) {
                        showLayerDialog = true
                    } else {
                        selectedSlotForPicker = slot
                    }
                }
            )
        }

        if (showLayerDialog) {
            net.ottercloud.sliderschrank.ui.homescreen.LayerView(
                layers = state.getVisibleLayers(),
                onReorder = state::reorderLayers,
                onDelete = state::removeLayer,
                onAdd = state::addLayer,
                onSelectLayer = { index ->
                    editingLayerIndex = index
                    selectedSlotForPicker =
                        Slot.entries.firstOrNull { it.supportsLayers() } ?: Slot.TOP
                },
                onDismiss = { showLayerDialog = false }
            )
        }

        selectedSlotForPicker?.let { slot ->
            PiecePickerDialog(
                slot = slot,
                pieces = pieces,
                state = state,
                scope = scope,
                database = database,
                onDismiss = {
                    selectedSlotForPicker = null
                    editingLayerIndex = null
                },
                onPieceSelect = if (slot.supportsLayers() && editingLayerIndex != null) {
                    { piece ->
                        editingLayerIndex?.let { state.updateLayerPiece(it, piece) }
                    }
                } else {
                    null
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