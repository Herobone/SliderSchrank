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
package net.ottercloud.sliderschrank.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.ottercloud.sliderschrank.data.model.Outfit
import net.ottercloud.sliderschrank.data.model.OutfitPieceCrossRef
import net.ottercloud.sliderschrank.data.model.OutfitTagCrossRef
import net.ottercloud.sliderschrank.data.model.OutfitWithPieces

@Dao
interface OutfitDao {
    @Transaction
    @Query("SELECT * FROM outfits ORDER BY created_at DESC")
    fun getAllOutfitsWithPieces(): Flow<List<OutfitWithPieces>>

    @Transaction
    @Query("SELECT * FROM outfits WHERE id = :id")
    fun getOutfitWithPiecesById(id: Long): Flow<OutfitWithPieces?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutfit(outfit: Outfit): Long

    @Update
    suspend fun updateOutfit(outfit: Outfit)

    @Delete
    suspend fun deleteOutfit(outfit: Outfit)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOutfitPieceCrossRef(crossRef: OutfitPieceCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOutfitTagCrossRef(crossRef: OutfitTagCrossRef)

    @Query("DELETE FROM outfit_piece_cross_ref WHERE outfit_id = :outfitId")
    suspend fun deletePiecesForOutfit(outfitId: Long)

    @Query("DELETE FROM outfit_tag_cross_ref WHERE outfit_id = :outfitId")
    suspend fun deleteTagsForOutfit(outfitId: Long)

    @Transaction
    suspend fun insertOutfitWithDetails(outfit: Outfit, pieceIds: List<Long>, tagIds: List<Long>) {
        val outfitId = insertOutfit(outfit)
        pieceIds.forEach { pieceId ->
            insertOutfitPieceCrossRef(OutfitPieceCrossRef(outfitId, pieceId))
        }
        tagIds.forEach { tagId ->
            insertOutfitTagCrossRef(OutfitTagCrossRef(outfitId, tagId))
        }
    }

    @Transaction
    suspend fun updateOutfitWithDetails(outfit: Outfit, pieceIds: List<Long>, tagIds: List<Long>) {
        updateOutfit(outfit)
        deletePiecesForOutfit(outfit.id)
        deleteTagsForOutfit(outfit.id)
        pieceIds.forEach { pieceId ->
            insertOutfitPieceCrossRef(OutfitPieceCrossRef(outfit.id, pieceId))
        }
        tagIds.forEach { tagId ->
            insertOutfitTagCrossRef(OutfitTagCrossRef(outfit.id, tagId))
        }
    }
}