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
package net.ottercloud.sliderschrank.util

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.pager.PagerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.ottercloud.sliderschrank.data.AppDatabase
import net.ottercloud.sliderschrank.data.dao.OutfitDao
import net.ottercloud.sliderschrank.data.dao.PieceDao
import net.ottercloud.sliderschrank.data.model.Colour
import net.ottercloud.sliderschrank.data.model.OutfitWithPieces
import net.ottercloud.sliderschrank.data.model.Piece
import net.ottercloud.sliderschrank.data.model.PieceWithDetails
import net.ottercloud.sliderschrank.data.model.Slot
import net.ottercloud.sliderschrank.ui.homescreen.HomeScreenState

// Helper function to create FavoriteUtil
fun createFavoriteUtil(context: Context, outfitDao: Any?, pieceDao: Any?): FavoriteUtil? =
    if (outfitDao is OutfitDao && pieceDao is PieceDao) {
        FavoriteUtil(context, outfitDao, pieceDao)
    } else {
        null
    }

// Helper function to group pieces with empty HEAD option
fun createGroupedPieces(pieces: List<PieceWithDetails>): Map<Slot, List<PieceWithDetails>> {
    if (pieces.isEmpty()) {
        return emptyMap()
    }
    val grouped = pieces.groupBy { it.piece.slot }.toMutableMap()
    val headPieces = grouped[Slot.HEAD].orEmpty().toMutableList()

    val emptyHeadPiece = PieceWithDetails(
        piece = Piece(
            id = -1L,
            imageUrl = "",
            isFavorite = false,
            colour = Colour.BLACK,
            slot = Slot.HEAD
        ),
        category = null,
        tags = emptyList()
    )
    headPieces.add(0, emptyHeadPiece)
    grouped[Slot.HEAD] = headPieces

    return grouped.toMap()
}

// Helper function to create toggle favorite callback
fun createToggleFavoriteCallback(
    context: Context,
    scope: CoroutineScope,
    favoriteUtil: FavoriteUtil?,
    failedMessage: String
): (List<OutfitWithPieces>, Set<Long>) -> Unit = { existing, current ->
    scope.launch {
        val success = favoriteUtil?.toggleFavorite(existing, current) ?: false
        if (!success) {
            Toast.makeText(context, failedMessage, Toast.LENGTH_LONG).show()
        }
    }
}

// Helper function to check if outfit loading conditions are met
fun shouldLoadOutfit(
    loadOutfitId: Long?,
    database: AppDatabase?,
    pagerStates: Map<Slot, PagerState>,
    pieces: List<PieceWithDetails>
): Boolean = loadOutfitId != null && loadOutfitId > 0 && database != null &&
    pagerStates.isNotEmpty() && pieces.isNotEmpty()

// Helper function to load and display outfit pieces
suspend fun loadOutfitPieces(outfit: OutfitWithPieces, state: HomeScreenState) {
    val slotsInOutfit = outfit.pieces.map { it.slot }.toSet()

    outfit.pieces.forEach { piece ->
        val slot = piece.slot
        val piecesForSlot = state.groupedPieces[slot]
        val pagerState = state.pagerStates[slot]

        if (piecesForSlot != null && pagerState != null) {
            val targetIndex = piecesForSlot.indexOfFirst { it.piece.id == piece.id }
            if (targetIndex >= 0) {
                pagerState.scrollToPage(targetIndex)
            }
        }
    }

    if (Slot.HEAD !in slotsInOutfit) {
        state.pagerStates[Slot.HEAD]?.scrollToPage(0)
    }
}