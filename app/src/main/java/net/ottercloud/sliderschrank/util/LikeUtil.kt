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
import android.util.Log
import java.util.Date
import kotlinx.coroutines.flow.Flow
import net.ottercloud.sliderschrank.data.dao.OutfitDao
import net.ottercloud.sliderschrank.data.dao.PieceDao
import net.ottercloud.sliderschrank.data.model.Outfit
import net.ottercloud.sliderschrank.data.model.OutfitWithPieces

class LikeUtil(
    private val context: Context,
    private val outfitDao: OutfitDao,
    private val pieceDao: PieceDao
) {

    val favoriteOutfitsWithPieces: Flow<List<OutfitWithPieces>> =
        outfitDao.getAllOutfitsWithPieces()

    suspend fun toggleFavorite(existingOutfits: List<OutfitWithPieces>, pieceIds: Set<Long>) {
        val matchingOutfit = existingOutfits.find { outfitWithPieces ->
            val outfitPieceIds = outfitWithPieces.pieces.map { it.id }.toSet()
            outfitPieceIds == pieceIds
        }

        if (matchingOutfit != null) {
            outfitDao.deleteOutfit(matchingOutfit.outfit)
            Log.d("LikeUtil", "Outfit removed: $pieceIds")
        } else {
            // Fetch the actual pieces to generate the composite image
            val pieces = pieceIds.mapNotNull { pieceId ->
                pieceDao.getPieceById(pieceId)
            }

            // Generate composite image from all pieces
            val imageUrl = OutfitImageGenerator.generateOutfitImage(context, pieces)

            // Create new outfit with the generated image
            val newOutfit = Outfit(
                imageUrl = imageUrl,
                isFavorite = true,
                createdAt = Date()
            )
            outfitDao.insertOutfitWithDetails(newOutfit, pieceIds.toList(), emptyList())
            Log.d("LikeUtil", "Outfit added: $pieceIds with image: $imageUrl")
        }
    }
}