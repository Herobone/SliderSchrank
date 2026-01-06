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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.ottercloud.sliderschrank.data.model.OutfitWithPieces
import net.ottercloud.sliderschrank.data.model.PieceWithDetails
import net.ottercloud.sliderschrank.data.model.Slot

@OptIn(ExperimentalFoundationApi::class)
class HomeScreenState(
    val groupedPieces: Map<Slot, List<PieceWithDetails>>,
    val pagerStates: Map<Slot, PagerState>,
    val savedOutfits: List<OutfitWithPieces>,
    private val onToggleFavorite: (List<OutfitWithPieces>, Set<Long>) -> Unit
) {
    var lockedPieceIds by mutableStateOf(emptySet<Long>())
        private set

    val currentOutfitIds by derivedStateOf {
        pagerStates.mapNotNull { (slot, pagerState) ->
            val pieceId = groupedPieces[slot]?.getOrNull(pagerState.currentPage)?.piece?.id
            // Filter out the empty HEAD piece (id = -1)
            if (pieceId != null && pieceId != -1L) pieceId else null
        }.toSet()
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

    fun onFavoriteClick() {
        onToggleFavorite(savedOutfits, currentOutfitIds)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberHomeScreenState(
    groupedPieces: Map<Slot, List<PieceWithDetails>>,
    savedOutfits: List<OutfitWithPieces>,
    onToggleFavorite: (List<OutfitWithPieces>, Set<Long>) -> Unit
): HomeScreenState {
    val pagerStates = getSlotOrder().associateWith { slot ->
        val piecesForSlot = groupedPieces[slot].orEmpty()
        rememberPagerState(pageCount = { piecesForSlot.size })
    }

    return remember(groupedPieces, pagerStates, savedOutfits, onToggleFavorite) {
        HomeScreenState(
            groupedPieces = groupedPieces,
            pagerStates = pagerStates,
            savedOutfits = savedOutfits,
            onToggleFavorite = onToggleFavorite
        )
    }
}

fun getSlotOrder(): List<Slot> = listOf(Slot.HEAD, Slot.TOP, Slot.BOTTOM, Slot.FEET)