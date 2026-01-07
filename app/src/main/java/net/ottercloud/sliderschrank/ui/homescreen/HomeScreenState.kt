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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.ottercloud.sliderschrank.data.model.OutfitWithPieces
import net.ottercloud.sliderschrank.data.model.PieceWithDetails
import net.ottercloud.sliderschrank.data.model.Slot

@OptIn(ExperimentalFoundationApi::class)
class HomeScreenState(
    initialGroupedPieces: Map<Slot, List<PieceWithDetails>>,
    val pagerStates: Map<Slot, PagerState>,
    initialSavedOutfits: List<OutfitWithPieces>,
    private val scope: CoroutineScope
) {
    var groupedPieces by mutableStateOf(initialGroupedPieces)
    var savedOutfits by mutableStateOf(initialSavedOutfits)

    private val allPiecesLookup by derivedStateOf {
        groupedPieces.values.asSequence().flatten().associateBy { it.piece.id }
    }

    var lockedPieceIds by mutableStateOf(emptySet<Long>())

    // Layers for the supported slot (underneath the active one)
    private var backgroundLayerIds by mutableStateOf(emptyList<Long>())

    val backgroundLayers: List<PieceWithDetails> by derivedStateOf {
        backgroundLayerIds.mapNotNull { allPiecesLookup[it] }
    }

    val currentOutfitIds by derivedStateOf {
        val baseIds = pagerStates.mapNotNull { (slot, pagerState) ->
            val pieceId = groupedPieces[slot]?.getOrNull(pagerState.currentPage)?.piece?.id
            // Filter out the empty HEAD piece (id = -1)
            if (pieceId != null && pieceId != -1L) pieceId else null
        }.toSet()

        (baseIds + backgroundLayerIds).toSet()
    }

    val isCurrentOutfitSaved by derivedStateOf {
        val currentIds = currentOutfitIds
        savedOutfits.any { saved ->
            saved.pieces.map { it.id }.toSet() == currentIds
        }
    }

    fun onLockClick(pieceId: Long) {
        lockedPieceIds = if (lockedPieceIds.contains(pieceId)) {
            lockedPieceIds - pieceId
        } else {
            lockedPieceIds + pieceId
        }
    }

    fun onFavoriteClick(onToggleFavorite: (List<OutfitWithPieces>, Set<Long>) -> Unit) {
        onToggleFavorite(savedOutfits, currentOutfitIds)
    }

    fun getVisibleLayers(): List<PieceWithDetails> {
        val activePiece = getActiveLayeredPiece()
        return if (activePiece != null) backgroundLayers + activePiece else backgroundLayers
    }

    fun addLayer() {
        val activePiece = getActiveLayeredPiece()
        if (activePiece != null) {
            // Move current active to background, keep active as is (duplicate for moment until user slides)
            // Or pick a default?
            backgroundLayerIds = backgroundLayerIds + activePiece.piece.id
        }
    }

    fun removeLayer(index: Int) {
        val currentLayers = getVisibleLayers()
        if (index in currentLayers.indices) {
            val newLayers = currentLayers.toMutableList().apply { removeAt(index) }
            updateLayers(newLayers)
        }
    }

    fun reorderLayers(from: Int, to: Int) {
        val currentLayers = getVisibleLayers().toMutableList()
        if (from in currentLayers.indices && to in currentLayers.indices) {
            val item = currentLayers.removeAt(from)
            currentLayers.add(to, item)
            updateLayers(currentLayers)
        }
    }

    fun updateLayerPiece(index: Int, newPiece: PieceWithDetails) {
        val currentLayers = getVisibleLayers().toMutableList()
        if (index in currentLayers.indices) {
            currentLayers[index] = newPiece
            updateLayers(currentLayers)
        }
    }

    fun setLayers(layers: List<PieceWithDetails>) {
        updateLayers(layers)
    }

    internal fun getBackgroundLayerIds(): List<Long> = backgroundLayerIds

    internal fun setBackgroundLayerIds(ids: List<Long>) {
        backgroundLayerIds = ids
    }

    private fun getActiveLayeredPiece(): PieceWithDetails? {
        val slot = Slot.entries.firstOrNull { it.supportsLayers() } ?: return null
        return groupedPieces[slot]?.getOrNull(pagerStates[slot]?.currentPage ?: 0)
    }

    private fun updateLayers(layers: List<PieceWithDetails>) {
        if (layers.isEmpty()) {
            backgroundLayerIds = emptyList()
            return
        }

        val newActive = layers.last()
        backgroundLayerIds = layers.dropLast(1).map { it.piece.id }

        // Sync Pager
        val slot = Slot.entries.firstOrNull { it.supportsLayers() } ?: return
        val topPieces = groupedPieces[slot] ?: return
        val index = topPieces.indexOfFirst { it.piece.id == newActive.piece.id }
        if (index != -1) {
            scope.launch {
                pagerStates[slot]?.scrollToPage(index)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberHomeScreenState(
    groupedPieces: Map<Slot, List<PieceWithDetails>>,
    savedOutfits: List<OutfitWithPieces>,
    scope: CoroutineScope = rememberCoroutineScope()
): HomeScreenState {
    val pagerStates = getSlotOrder().associateWith { slot ->
        val piecesForSlot = groupedPieces[slot].orEmpty()
        rememberPagerState(pageCount = { piecesForSlot.size })
    }

    val saver = Saver<HomeScreenState, Map<String, Any>>(
        save = { state ->
            mapOf(
                "locked" to state.lockedPieceIds.toList(),
                "layers" to state.getBackgroundLayerIds()
            )
        },
        restore = { map ->
            // Restore IDs safely
            @Suppress("UNCHECKED_CAST")
            val lockedIds = (map["locked"] as? List<Long>)?.toSet() ?: emptySet()

            @Suppress("UNCHECKED_CAST")
            val layerIds = map["layers"] as? List<Long> ?: emptyList()

            HomeScreenState(
                initialGroupedPieces = groupedPieces,
                pagerStates = pagerStates,
                initialSavedOutfits = savedOutfits,
                scope = scope
            ).apply {
                this.lockedPieceIds = lockedIds
                this.setBackgroundLayerIds(layerIds)
            }
        }
    )

    val state = rememberSaveable(saver = saver) {
        HomeScreenState(
            initialGroupedPieces = groupedPieces,
            pagerStates = pagerStates,
            initialSavedOutfits = savedOutfits,
            scope = scope
        )
    }

    LaunchedEffect(groupedPieces) {
        state.groupedPieces = groupedPieces
    }

    LaunchedEffect(savedOutfits) {
        state.savedOutfits = savedOutfits
    }

    return state
}

fun getSlotOrder(): List<Slot> = listOf(Slot.HEAD, Slot.TOP, Slot.BOTTOM, Slot.FEET)