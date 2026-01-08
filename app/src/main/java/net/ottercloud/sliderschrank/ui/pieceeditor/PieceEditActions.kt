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
package net.ottercloud.sliderschrank.ui.pieceeditor

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import java.util.Date
import kotlinx.coroutines.launch
import net.ottercloud.sliderschrank.R
import net.ottercloud.sliderschrank.data.AppDatabase
import net.ottercloud.sliderschrank.data.model.Piece
import net.ottercloud.sliderschrank.data.model.PieceTagCrossRef

@Composable
fun rememberPieceEditActions(
    database: AppDatabase,
    state: PieceEditScreenState,
    onNavigateBack: () -> Unit
): PieceEditActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    return PieceEditActions(
        onSave = {
            scope.launch {
                savePiece(
                    database = database,
                    state = state,
                    context = context,
                    onSuccess = onNavigateBack
                )
            }
        },
        onDelete = {
            scope.launch {
                state.piece?.let {
                    database.pieceDao().deletePiece(it)
                    onNavigateBack()
                }
            }
        }
    )
}

data class PieceEditActions(val onSave: () -> Unit, val onDelete: () -> Unit)

private suspend fun savePiece(
    database: AppDatabase,
    state: PieceEditScreenState,
    context: Context,
    onSuccess: () -> Unit
) {
    val imageUrl = state.imageUrl ?: ""

    if (imageUrl.isEmpty()) {
        Toast.makeText(
            context,
            context.getString(R.string.image_is_required),
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    val newPiece = buildPiece(state, imageUrl)
    savePieceData(database, newPiece, state)

    onSuccess()
}

private suspend fun savePieceData(
    database: AppDatabase,
    newPiece: Piece,
    state: PieceEditScreenState
) {
    val tagNames = (state.completedTags + state.currentTagInput.trim())
        .filter { it.isNotEmpty() }
        .distinct()

    val tagIds = tagNames.map { database.tagDao().getOrCreateTag(it) }

    if (state.piece == null) {
        // New piece
        val pieceId = database.pieceDao().insertPiece(newPiece)
        tagIds.forEach { tagId ->
            database.pieceDao().insertPieceTagCrossRef(PieceTagCrossRef(pieceId, tagId))
        }
    } else {
        // Update existing piece
        database.pieceDao().updatePieceWithTags(newPiece, tagIds)
    }
}

private fun buildPiece(state: PieceEditScreenState, imageUrl: String): Piece {
    val existing = state.piece
    return existing?.copy(
        imageUrl = imageUrl,
        categoryId = state.selectedCategory?.id,
        slot = state.selectedSlot,
        colour = state.selectedColour
    )
        ?: Piece(
            imageUrl = imageUrl,
            createdAt = Date(),
            colour = state.selectedColour,
            slot = state.selectedSlot,
            categoryId = state.selectedCategory?.id
        )
}