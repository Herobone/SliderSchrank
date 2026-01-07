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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.ottercloud.sliderschrank.R
import net.ottercloud.sliderschrank.data.AppDatabase
import net.ottercloud.sliderschrank.data.model.PieceWithDetails
import net.ottercloud.sliderschrank.data.model.Slot
import net.ottercloud.sliderschrank.ui.FilteredView

fun getSlotDisplayNameResource(slot: Slot): Int = when (slot) {
    Slot.HEAD -> R.string.slot_head
    Slot.TOP -> R.string.slot_top
    Slot.BOTTOM -> R.string.slot_bottom
    Slot.FEET -> R.string.slot_feet
    Slot.ACCESSORY -> R.string.slot_accessory
}

@Composable
fun PiecePickerDialog(
    slot: Slot,
    pieces: List<PieceWithDetails>,
    state: HomeScreenState,
    scope: CoroutineScope,
    database: AppDatabase?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.select_slot,
                            stringResource(getSlotDisplayNameResource(slot))
                        ),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                HorizontalDivider()
                FilteredView(
                    modifier = Modifier.fillMaxSize(),
                    items = pieces,
                    imageUrlProvider = { it.piece.imageUrl },
                    tagProvider = { it.tags.map { tag -> tag.name } },
                    onItemClick = { pieceWithDetails ->
                        val targetIndex = state.groupedPieces[slot]
                            ?.indexOfFirst { it.piece.id == pieceWithDetails.piece.id }
                            ?: -1

                        if (targetIndex >= 0) {
                            scope.launch {
                                state.pagerStates[slot]?.animateScrollToPage(targetIndex)
                            }
                        }
                        onDismiss()
                    },
                    isFavoriteProvider = { it.piece.isFavorite },
                    onFavoriteClick = { pieceWithDetails ->
                        scope.launch {
                            val updatedPiece = pieceWithDetails.piece.copy(
                                isFavorite = !pieceWithDetails.piece.isFavorite
                            )
                            database?.pieceDao()?.updatePiece(updatedPiece)
                        }
                    },
                    categoryProvider = { it.category?.name },
                    slotProvider = { it.piece.slot },
                    slotFilter = slot
                )
            }
        }
    }
}