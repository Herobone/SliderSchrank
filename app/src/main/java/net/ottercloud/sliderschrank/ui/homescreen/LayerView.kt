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

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import net.ottercloud.sliderschrank.R
import net.ottercloud.sliderschrank.data.model.PieceWithDetails
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

private data class UiLayer(val uniqueId: String, val piece: PieceWithDetails)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerView(
    layers: List<PieceWithDetails>,
    onReorder: (Int, Int) -> Unit,
    onDelete: (Int) -> Unit,
    onAdd: () -> Unit,
    onSelectLayer: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        LayerListContent(
            layers = layers,
            onReorder = onReorder,
            onDelete = onDelete,
            onAdd = onAdd,
            onSelectLayer = onSelectLayer
        )
    }
}

@Composable
private fun LayerListContent(
    layers: List<PieceWithDetails>,
    onReorder: (Int, Int) -> Unit,
    onDelete: (Int) -> Unit,
    onAdd: () -> Unit,
    onSelectLayer: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.layers),
            style = MaterialTheme.typography.titleLarge
        )

        val uiLayers = rememberUniqueLayers(layers)
        var localLayers by remember { mutableStateOf(uiLayers) }

        LaunchedEffect(uiLayers) {
            localLayers = uiLayers
        }

        val lazyListState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            localLayers = localLayers.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
            val realFrom = layers.lastIndex - from.index
            val realTo = layers.lastIndex - to.index
            onReorder(realFrom, realTo)
        }

        LayerList(
            state = lazyListState,
            reorderableState = reorderableState,
            localLayers = localLayers,
            originalLayers = layers,
            onDelete = onDelete,
            onSelectLayer = onSelectLayer
        )

        AddLayerButton(onAdd)
    }
}

@Composable
private fun rememberUniqueLayers(layers: List<PieceWithDetails>): List<UiLayer> = remember(layers) {
    val counts = mutableMapOf<Long, Int>()
    layers.asReversed().map { pieceWithDetails ->
        val id = pieceWithDetails.piece.id
        val count = counts.getOrDefault(id, 0)
        counts[id] = count + 1
        UiLayer("${id}_$count", pieceWithDetails)
    }
}

@Composable
private fun LayerList(
    state: LazyListState,
    reorderableState: ReorderableLazyListState,
    localLayers: List<UiLayer>,
    originalLayers: List<PieceWithDetails>,
    onDelete: (Int) -> Unit,
    onSelectLayer: (Int) -> Unit
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(localLayers, key = { _, item -> item.uniqueId }) { index, uiLayer ->
            ReorderableLayerRow(
                reorderableState = reorderableState,
                uiLayer = uiLayer,
                index = index,
                originalLayers = originalLayers,
                onDelete = onDelete,
                onSelectLayer = onSelectLayer
            )
        }
    }
}

@Composable
private fun LazyItemScope.ReorderableLayerRow(
    reorderableState: ReorderableLazyListState,
    uiLayer: UiLayer,
    index: Int,
    originalLayers: List<PieceWithDetails>,
    onDelete: (Int) -> Unit,
    onSelectLayer: (Int) -> Unit
) {
    ReorderableItem(reorderableState, key = uiLayer.uniqueId) { isDragging ->
        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
        val realIndex = originalLayers.lastIndex - index

        LayerItem(
            piece = uiLayer.piece,
            onDelete = { if (realIndex in originalLayers.indices) onDelete(realIndex) },
            onClick = { if (realIndex in originalLayers.indices) onSelectLayer(realIndex) },
            modifier = Modifier
                .shadow(elevation)
                .background(MaterialTheme.colorScheme.surface),
            dragHandleModifier = Modifier.draggableHandle(),
            isActive = realIndex == originalLayers.lastIndex
        )
    }
}

@Composable
private fun AddLayerButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Text(
            text = stringResource(R.string.add_layer),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun LayerItem(
    piece: PieceWithDetails,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = if (isActive) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Drag handle
            Icon(
                Icons.Default.DragHandle,
                contentDescription = stringResource(R.string.reorder_layer),
                modifier = dragHandleModifier
                    .size(24.dp)
            )

            // Image
            AsyncImage(
                model = piece.piece.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            // Info
            Text(
                text = piece.category?.name ?: stringResource(R.string.clothes),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )

            // Delete
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.remove_layer)
                )
            }
        }
    }
}