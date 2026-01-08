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
package net.ottercloud.sliderschrank.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import net.ottercloud.sliderschrank.R
import net.ottercloud.sliderschrank.data.AppDatabase
import net.ottercloud.sliderschrank.data.model.Colour
import net.ottercloud.sliderschrank.data.model.Slot

@Composable
fun PieceEditScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    pieceId: Long? = null,
    imageUri: String? = null
) {
    val database = rememberAppDatabase()
    val state = rememberPieceEditState(database, pieceId, imageUri)
    val actions = rememberPieceEditActions(database, state, onNavigateBack)

    if (state.isLoading) {
        LoadingScreen(modifier)
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = { PieceEditTopBar(onNavigateBack, pieceId, actions.onDelete) }
    ) { innerPadding ->
        PieceEditContent(state, actions, database, innerPadding)
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PieceEditContent(
    state: PieceEditScreenState,
    actions: PieceEditActions,
    database: AppDatabase,
    innerPadding: androidx.compose.foundation.layout.PaddingValues
) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ImagePreview(state.imageUrl)
        CategorySelectorComponent(state, database)
        SlotSelector(state)
        ColourSelector(state)
        TagInputComponent(state)
        SaveButton(actions.onSave)
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun rememberAppDatabase(): AppDatabase {
    val context = androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.runtime.remember { AppDatabase.getDatabase(context) }
}

@Composable
private fun ImagePreview(imageUrl: String?) {
    imageUrl?.let {
        AsyncImage(
            model = it,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
}

@Composable
private fun SlotSelector(state: PieceEditScreenState) {
    val slotNames = buildSlotNames()
    DropdownSelectorComponent(
        label = stringResource(R.string.slot),
        items = Slot.entries.toList(),
        selectedItem = state.selectedSlot,
        itemLabel = { slotNames[it] ?: it.name },
        onItemSelect = state.onSlotChange
    )
}

@Composable
private fun ColourSelector(state: PieceEditScreenState) {
    val colourNames = buildColourNames()
    DropdownSelectorComponent(
        label = stringResource(R.string.colour),
        items = Colour.entries.toList(),
        selectedItem = state.selectedColour,
        itemLabel = { colourNames[it] ?: it.name },
        onItemSelect = state.onColourChange
    )
}

@Composable
private fun SaveButton(onSave: () -> Unit) {
    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.save))
    }
}

@Composable
private fun buildSlotNames(): Map<Slot, String> = mapOf(
    Slot.HEAD to stringResource(R.string.slot_head),
    Slot.TOP to stringResource(R.string.slot_top),
    Slot.BOTTOM to stringResource(R.string.slot_bottom),
    Slot.FEET to stringResource(R.string.slot_feet),
    Slot.ACCESSORY to stringResource(R.string.slot_accessory)
)

@Composable
private fun buildColourNames(): Map<Colour, String> = mapOf(
    Colour.BLACK to stringResource(R.string.colour_black),
    Colour.WHITE to stringResource(R.string.colour_white),
    Colour.RED to stringResource(R.string.colour_red),
    Colour.BLUE to stringResource(R.string.colour_blue),
    Colour.GREEN to stringResource(R.string.colour_green),
    Colour.YELLOW to stringResource(R.string.colour_yellow),
    Colour.GREY to stringResource(R.string.colour_grey),
    Colour.BROWN to stringResource(R.string.colour_brown),
    Colour.PINK to stringResource(R.string.colour_pink),
    Colour.ORANGE to stringResource(R.string.colour_orange),
    Colour.PURPLE to stringResource(R.string.colour_purple)
)