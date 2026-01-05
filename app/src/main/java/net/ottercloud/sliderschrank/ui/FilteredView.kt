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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.ottercloud.sliderschrank.data.model.Slot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilteredView(
    items: List<T>,
    imageUrlProvider: (T) -> String,
    tagProvider: (T) -> List<String>,
    onItemClick: (T) -> Unit,
    isFavoriteProvider: (T) -> Boolean,
    onFavoriteClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    categoryProvider: (T) -> String? = { null },
    slotProvider: (T) -> Slot? = { null },
    slotFilter: Slot? = null
) {
    val selectedFilters = remember { mutableStateListOf<String>() }

    // First filter by slot to get the base items for this slot
    val slotFilteredItems = remember(items, slotFilter) {
        if (slotFilter != null) {
            items.filter { item ->
                val itemSlot = slotProvider(item)
                itemSlot == slotFilter
            }
        } else {
            items
        }
    }

    // Extract all unique tags and categories from slot-filtered items only
    val allFilters = remember(slotFilteredItems) {
        slotFilteredItems.flatMap { item ->
            val tags = tagProvider(item)
            val category = categoryProvider(item)
            if (category != null) tags + category else tags
        }.distinct().sorted()
    }

    val filteredItems by remember(slotFilteredItems, selectedFilters.toList()) {
        derivedStateOf {
            slotFilteredItems.filter { item ->
                // Filter by Chips (Tags/Categories)
                if (selectedFilters.isNotEmpty()) {
                    val itemTags = tagProvider(item)
                    val itemCategory = categoryProvider(item)
                    val itemFilters = if (itemCategory !=
                        null
                    ) {
                        itemTags + itemCategory
                    } else {
                        itemTags
                    }
                    // Check if item has ALL selected filters (AND logic)
                    selectedFilters.all { it in itemFilters }
                } else {
                    true
                }
            }.sortedByDescending { isFavoriteProvider(it) }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(allFilters) { filter ->
                val isSelected = filter in selectedFilters
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) {
                            selectedFilters.remove(filter)
                        } else {
                            selectedFilters.add(filter)
                        }
                    },
                    label = { Text(filter) }
                )
            }
        }

        // Grid of Items
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredItems) { item ->
                ClothingItemCard(
                    imageUrl = imageUrlProvider(item),
                    isFavorite = isFavoriteProvider(item),
                    onFavoriteClick = { onFavoriteClick(item) },
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
fun ClothingItemCard(
    imageUrl: String,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                    tint = if (isFavorite) Color.Yellow else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}