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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.ottercloud.sliderschrank.data.model.Slot

@Composable
fun GarmentSliders(
    state: HomeScreenState,
    onSelectSlot: (Slot) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        getSlotOrder().forEach { slot ->
            val piecesForSlot = state.groupedPieces[slot].orEmpty()
            val pagerState = state.pagerStates[slot]
            val weight = when (slot) {
                Slot.HEAD, Slot.FEET -> 0.2f
                else -> 0.3f
            }

            if (piecesForSlot.isNotEmpty() && pagerState != null) {
                val currentPiece = piecesForSlot.getOrNull(pagerState.currentPage)
                val isCurrentItemLocked = currentPiece?.piece?.id in state.lockedPieceIds

                GarmentSlider(
                    garments = piecesForSlot,
                    pagerState = pagerState,
                    isSwipeEnabled = !isCurrentItemLocked,
                    lockedPieceIds = state.lockedPieceIds,
                    onLockClick = state::onLockClick,
                    onPieceClick = { onSelectSlot(it.piece.slot) },
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxWidth(),
                    backgroundLayers = if (slot.supportsLayers()) {
                        state.backgroundLayers
                    } else {
                        emptyList()
                    }
                )
            }
        }
    }
}