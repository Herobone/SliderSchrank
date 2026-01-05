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
package net.ottercloud.sliderschrank

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.util.Locale
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import net.ottercloud.sliderschrank.data.AppDatabase
import net.ottercloud.sliderschrank.data.model.Colour
import net.ottercloud.sliderschrank.data.model.OutfitWithPieces
import net.ottercloud.sliderschrank.data.model.Piece
import net.ottercloud.sliderschrank.data.model.PieceWithDetails
import net.ottercloud.sliderschrank.data.model.Slot
import net.ottercloud.sliderschrank.ui.FilteredView
import net.ottercloud.sliderschrank.ui.theme.SliderSchrankTheme
import net.ottercloud.sliderschrank.util.DummyDataGenerator
import net.ottercloud.sliderschrank.util.LikeUtil
import net.ottercloud.sliderschrank.util.SettingsManager
import net.ottercloud.sliderschrank.util.performUiShuffle

private val slotOrder = listOf(Slot.HEAD, Slot.TOP, Slot.BOTTOM, Slot.FEET)

private fun Slot.displayName(): String =
    name.lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }

@OptIn(ExperimentalFoundationApi::class)
private class HomeScreenState(
    val groupedPieces: Map<Slot, List<PieceWithDetails>>,
    val pagerStates: Map<Slot, PagerState>,
    val savedOutfits: List<OutfitWithPieces>,
    val onToggleFavorite: (List<OutfitWithPieces>, Set<Long>) -> Unit
) {
    var lockedPieceIds by mutableStateOf(emptySet<Long>())
        private set

    val currentOutfitIds by derivedStateOf {
        pagerStates.mapNotNull { (slot, pagerState) ->
            val pieceId = groupedPieces[slot]?.getOrNull(pagerState.currentPage)?.piece?.id
            // Filter out the empty HEAD piece (id = -1)
            if (pieceId != null && pieceId != -1L) pieceId else null
        }.toSet()
    }

    val isCurrentOutfitSaved by derivedStateOf {
        val currentIds = currentOutfitIds
        savedOutfits.any { saved ->
            saved.pieces.map { it.id }.toSet() == currentIds
        }
    }

    fun onLockClick(pieceId: Long) {
        lockedPieceIds = if (lockedPieceIds.contains(pieceId)) {
            lockedPieceIds - pieceId
        } else {
            lockedPieceIds + pieceId
        }
    }

    fun onFavoriteClick() {
        onToggleFavorite(savedOutfits, currentOutfitIds)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberHomeScreenState(
    groupedPieces: Map<Slot, List<PieceWithDetails>>,
    savedOutfits: List<OutfitWithPieces>,
    onToggleFavorite: (List<OutfitWithPieces>, Set<Long>) -> Unit
): HomeScreenState {
    val pagerStates = slotOrder.associateWith { slot ->
        val piecesForSlot = groupedPieces[slot].orEmpty()
        rememberPagerState(pageCount = { piecesForSlot.size })
    }

    return remember(groupedPieces, pagerStates, savedOutfits, onToggleFavorite) {
        HomeScreenState(
            groupedPieces = groupedPieces,
            pagerStates = pagerStates,
            savedOutfits = savedOutfits,
            onToggleFavorite = onToggleFavorite
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier, loadOutfitId: Long? = null) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val settingsManager = remember { SettingsManager(context) }
    val background by settingsManager.background.collectAsState(initial = AppBackground.WHITE)
    val database = remember(context, isInPreview) {
        if (isInPreview) null else AppDatabase.getDatabase(context)
    }
    val scope = rememberCoroutineScope()

    val outfitDao = database?.outfitDao()
    val pieceDao = database?.pieceDao()
    val likeUtil = remember(outfitDao, pieceDao) {
        if (outfitDao != null && pieceDao != null) {
            LikeUtil(context, outfitDao, pieceDao)
        } else {
            null
        }
    }

    val savedOutfits by likeUtil?.favoriteOutfitsWithPieces?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) }

    LaunchedEffect(database) {
        database?.let { DummyDataGenerator.generateDummyData(context, it) }
    }

    val pieces by database?.pieceDao()?.getAllPiecesWithDetails()?.collectAsState(
        initial = emptyList()
    )
        ?: remember { mutableStateOf(emptyList()) }

    val groupedPieces = remember(pieces) {
        Log.d("HomeScreen", "Pieces list size: ${pieces.size}")
        pieces.forEach { piece ->
            Log.d("HomeScreen", "Piece: id=${piece.piece.id}, slot=${piece.piece.slot}")
        }

        val grouped = pieces.groupBy { it.piece.slot }.toMutableMap()
        Log.d("HomeScreen", "Grouped slots: ${grouped.keys}")

        // Add "None" option for HEAD slot at the beginning
        val headPieces = grouped[Slot.HEAD].orEmpty().toMutableList()
        val emptyHeadPiece = PieceWithDetails(
            piece = Piece(
                id = -1L, // Special ID for empty piece
                imageUrl = "", // Empty image URL
                isFavorite = false,
                colour = Colour.BLACK,
                slot = Slot.HEAD
            ),
            category = null,
            tags = emptyList()
        )
        headPieces.add(0, emptyHeadPiece) // Add at the beginning
        grouped[Slot.HEAD] = headPieces

        Log.d("HomeScreen", "Final grouped slots: ${grouped.keys}")
        grouped.toMap()
    }

    val onToggleFavorite: (List<OutfitWithPieces>, Set<Long>) -> Unit = remember(scope, likeUtil) {
        { existing, current ->
            scope.launch {
                likeUtil?.toggleFavorite(existing, current)
            }
        }
    }

    val state = rememberHomeScreenState(groupedPieces, savedOutfits, onToggleFavorite)

    // Load outfit if loadOutfitId is provided
    // Depend on pieces to re-trigger when data loads (not groupedPieces which always has HEAD)
    LaunchedEffect(loadOutfitId, pieces.isNotEmpty()) {
        Log.d(
            "HomeScreen",
            "LaunchedEffect triggered: loadOutfitId=$loadOutfitId, pagerStates.size=${state.pagerStates.size}, groupedPieces.size=${groupedPieces.size}, pieces.size=${pieces.size}"
        )

        if (loadOutfitId != null && loadOutfitId > 0 && database != null &&
            state.pagerStates.isNotEmpty() && pieces.isNotEmpty()
        ) {
            Log.d("HomeScreen", "Attempting to load outfit with ID: $loadOutfitId")

            val outfitWithPieces = database.outfitDao().getOutfitWithPiecesById(
                loadOutfitId
            ).firstOrNull()

            Log.d(
                "HomeScreen",
                "Loaded outfit: ${outfitWithPieces?.outfit?.id}, pieces: ${outfitWithPieces?.pieces?.size}"
            )

            outfitWithPieces?.let { outfit ->
                // Track which slots have pieces in the outfit
                val slotsInOutfit = outfit.pieces.map { it.slot }.toSet()

                // For each piece in the outfit, find its slot and scroll to it
                // Use scrollToPage (instant) to load all pieces at once without animation delay
                outfit.pieces.forEach { piece ->
                    val slot = piece.slot
                    val piecesForSlot = state.groupedPieces[slot]
                    val pagerState = state.pagerStates[slot]

                    Log.d(
                        "HomeScreen",
                        "Processing piece: ${piece.id}, slot: $slot, piecesForSlot.size=${piecesForSlot?.size}, pagerState=$pagerState"
                    )

                    if (piecesForSlot != null && pagerState != null) {
                        val targetIndex = piecesForSlot.indexOfFirst { it.piece.id == piece.id }
                        Log.d("HomeScreen", "Target index for piece ${piece.id}: $targetIndex")

                        if (targetIndex >= 0) {
                            Log.d("HomeScreen", "Scrolling to page $targetIndex for slot $slot")
                            pagerState.scrollToPage(targetIndex)
                        } else {
                            Log.w("HomeScreen", "Piece ${piece.id} not found in slot $slot")
                        }
                    } else {
                        Log.w(
                            "HomeScreen",
                            "Missing data for slot $slot: piecesForSlot=$piecesForSlot, pagerState=$pagerState"
                        )
                    }
                }

                // For HEAD slot, if no HEAD piece in outfit, scroll to "None" (index 0)
                if (Slot.HEAD !in slotsInOutfit) {
                    state.pagerStates[Slot.HEAD]?.let { pagerState ->
                        Log.d(
                            "HomeScreen",
                            "No HEAD piece in outfit, scrolling to 'None' (index 0)"
                        )
                        pagerState.scrollToPage(0)
                    }
                }
            }
        } else {
            Log.d(
                "HomeScreen",
                "Skipping outfit load: loadOutfitId=$loadOutfitId, database=$database, pagerStates.isEmpty=${state.pagerStates.isEmpty()}, pieces.isEmpty=${pieces.isEmpty()}"
            )
        }
    }

    val onShuffleClick: () -> Unit = remember(scope, state) {
        {
            performUiShuffle(
                scope = scope,
                pagerStates = state.pagerStates,
                groupedPieces = state.groupedPieces,
                lockedPieceIds = state.lockedPieceIds
            )
        }
    }

    var selectedSlotForPicker by remember { mutableStateOf<Slot?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        when (background) {
            AppBackground.CORK -> {
                Image(
                    painter = painterResource(id = R.drawable.kork),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            AppBackground.GRAY -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray)
                )
            }

            AppBackground.WHITE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
            }

            AppBackground.CHECKERED -> {
                CheckedBackground(modifier = Modifier.fillMaxSize())
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            HomeScreenTopBar(
                isOutfitSaved = state.isCurrentOutfitSaved,
                onShuffleClick = onShuffleClick,
                onFavoriteClick = state::onFavoriteClick
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                slotOrder.forEach { slot ->
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
                            onPieceClick = { selectedSlotForPicker = it.piece.slot },
                            modifier = Modifier
                                .weight(weight)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }

        selectedSlotForPicker?.let { slot ->
            Dialog(
                onDismissRequest = { selectedSlotForPicker = null },
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
                                text = "Select ${slot.displayName()}",
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
                                selectedSlotForPicker = null
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
    }
}

@Composable
private fun HomeScreenTopBar(
    isOutfitSaved: Boolean,
    onShuffleClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(R.string.your_outfit), style = MaterialTheme.typography.titleLarge)
        Row {
            IconButton(onClick = onShuffleClick) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = stringResource(R.string.random_outfit)
                )
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isOutfitSaved) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = stringResource(R.string.save_outfit),
                    tint = if (isOutfitSaved) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GarmentSlider(
    garments: List<PieceWithDetails>,
    pagerState: PagerState,
    isSwipeEnabled: Boolean,
    lockedPieceIds: Set<Long>,
    onLockClick: (Long) -> Unit,
    onPieceClick: (PieceWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = isSwipeEnabled,
            key = { page -> garments[page].piece.id },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) { page ->
            val garment = garments[page]

            val itemOnLockClick = remember(garment.piece.id) {
                { onLockClick(garment.piece.id) }
            }

            val itemOnPieceClick = remember(garment.piece.id) {
                { onPieceClick(garment) }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GarmentItem(
                    garment = garment,
                    isLocked = lockedPieceIds.contains(garment.piece.id),
                    onLockClick = itemOnLockClick,
                    onGarmentClick = itemOnPieceClick
                )
            }
        }
    }
}

@Composable
fun GarmentItem(
    garment: PieceWithDetails,
    isLocked: Boolean,
    onLockClick: () -> Unit,
    onGarmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onGarmentClick)
        ) {
            // Check if this is the empty HEAD piece
            if (garment.piece.id == -1L) {
                // Display "None" for empty piece
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "None",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(garment.piece.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = garment.category?.name ?: garment.piece.id.toString(),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        // Don't show lock icon for empty piece
        if (garment.piece.id != -1L) {
            IconButton(
                onClick = onLockClick,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = stringResource(R.string.lock_item),
                    tint = if (isLocked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SliderSchrankTheme {
        HomeScreen()
    }
}