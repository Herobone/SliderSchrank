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
@file:JvmName("PieceEditStateKt")

package net.ottercloud.sliderschrank.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.ottercloud.sliderschrank.data.AppDatabase
import net.ottercloud.sliderschrank.data.model.Category
import net.ottercloud.sliderschrank.data.model.Colour
import net.ottercloud.sliderschrank.data.model.Piece
import net.ottercloud.sliderschrank.data.model.Slot

@Composable
fun rememberPieceEditState(
    database: AppDatabase,
    pieceId: Long?,
    imageUri: String?
): PieceEditScreenState {
    var piece by remember { mutableStateOf<Piece?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedSlot by remember { mutableStateOf(Slot.TOP) }
    var selectedColour by remember { mutableStateOf(Colour.BLACK) }
    var completedTags by remember { mutableStateOf(listOf<String>()) }
    var currentTagInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var imageUrl by remember { mutableStateOf(imageUri) }

    val categories by database.categoryDao().getAllCategories().collectAsState(emptyList())
    val allTags by database.tagDao().getAllTags().collectAsState(emptyList())

    val pieceWithDetailsState = if (isValidPieceId(pieceId)) {
        database.pieceDao().getPieceWithDetailsById(pieceId!!).collectAsState(initial = null)
    } else {
        null
    }

    val pieceWithDetails = pieceWithDetailsState?.value

    // Load piece data
    LaunchedEffect(pieceWithDetails) {
        if (pieceWithDetails != null) {
            piece = pieceWithDetails.piece
            selectedCategory = pieceWithDetails.category
            selectedSlot = pieceWithDetails.piece.slot
            selectedColour = pieceWithDetails.piece.colour
            completedTags = pieceWithDetails.tags.map { tag -> tag.name }
            currentTagInput = ""
            imageUrl = pieceWithDetails.piece.imageUrl
        }
        isLoading = false
    }

    return PieceEditScreenState(
        piece = piece,
        selectedCategory = selectedCategory,
        selectedSlot = selectedSlot,
        selectedColour = selectedColour,
        completedTags = completedTags,
        currentTagInput = currentTagInput,
        isLoading = isLoading,
        imageUrl = imageUrl,
        categories = categories,
        allTags = allTags,
        onCategoryChange = { selectedCategory = it },
        onSlotChange = { selectedSlot = it },
        onColourChange = { selectedColour = it },
        onTagsChange = { completedTags = it },
        onTagInputChange = { currentTagInput = it },
        onImageUrlChange = { imageUrl = it }
    )
}

data class PieceEditScreenState(
    val piece: Piece?,
    val selectedCategory: Category?,
    val selectedSlot: Slot,
    val selectedColour: Colour,
    val completedTags: List<String>,
    val currentTagInput: String,
    val isLoading: Boolean,
    val imageUrl: String?,
    val categories: List<Category>,
    val allTags: List<net.ottercloud.sliderschrank.data.model.Tag>,
    val onCategoryChange: (Category?) -> Unit,
    val onSlotChange: (Slot) -> Unit,
    val onColourChange: (Colour) -> Unit,
    val onTagsChange: (List<String>) -> Unit,
    val onTagInputChange: (String) -> Unit,
    val onImageUrlChange: (String?) -> Unit
)

private fun isValidPieceId(pieceId: Long?): Boolean = pieceId != null && pieceId != -1L