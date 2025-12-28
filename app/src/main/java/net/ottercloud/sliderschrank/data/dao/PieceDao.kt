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
package net.ottercloud.sliderschrank.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.ottercloud.sliderschrank.data.model.Piece
import net.ottercloud.sliderschrank.data.model.PieceTagCrossRef
import net.ottercloud.sliderschrank.data.model.PieceWithDetails

@Dao
interface PieceDao {
    @Transaction
    @Query("SELECT * FROM pieces ORDER BY created_at DESC")
    fun getAllPiecesWithDetails(): Flow<List<PieceWithDetails>>

    @Transaction
    @Query("SELECT * FROM pieces WHERE id = :id")
    fun getPieceWithDetailsById(id: Long): Flow<PieceWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPiece(piece: Piece): Long

    @Update
    suspend fun updatePiece(piece: Piece)

    @Delete
    suspend fun deletePiece(piece: Piece)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPieceTagCrossRef(crossRef: PieceTagCrossRef)

    @Query("DELETE FROM piece_tag_cross_ref WHERE piece_id = :pieceId")
    suspend fun deleteTagsForPiece(pieceId: Long)

    @Transaction
    suspend fun updatePieceWithTags(piece: Piece, tagIds: List<Long>) {
        updatePiece(piece)
        deleteTagsForPiece(piece.id)
        tagIds.forEach { tagId ->
            insertPieceTagCrossRef(PieceTagCrossRef(piece.id, tagId))
        }
    }
}