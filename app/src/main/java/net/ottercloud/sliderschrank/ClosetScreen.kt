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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import net.ottercloud.sliderschrank.data.AppDatabase
import net.ottercloud.sliderschrank.ui.FilteredView
import net.ottercloud.sliderschrank.ui.theme.SliderSchrankTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Closet(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    val pieces by database.pieceDao().getAllPiecesWithDetails().collectAsState(
        initial = emptyList()
    )
    val outfits by database.outfitDao().getAllOutfitsWithPieces().collectAsState(
        initial = emptyList()
    )

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.clothes), stringResource(R.string.outfits))
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.closet)) },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(AppDestinations.SETTINGS.name)
                    }) {
                        Icon(
                            imageVector = AppDestinations.SETTINGS.icon,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> FilteredView(
                    items = pieces,
                    imageUrlProvider = { it.piece.imageUrl },
                    tagProvider = { it.tags.map { tag -> tag.name } },
                    onItemClick = { pieceWithDetails ->
                        navController.navigate(
                            "${AppDestinations.PIECE_EDIT.name}?pieceId=${pieceWithDetails.piece.id}"
                        )
                    },
                    isFavoriteProvider = { it.piece.isFavorite },
                    onFavoriteClick = { pieceWithDetails ->
                        scope.launch {
                            val updatedPiece = pieceWithDetails.piece.copy(
                                isFavorite = !pieceWithDetails.piece.isFavorite
                            )
                            database.pieceDao().updatePiece(updatedPiece)
                        }
                    },
                    categoryProvider = { it.category?.name },
                    slotProvider = { it.piece.slot }
                )

                1 -> FilteredView(
                    items = outfits,
                    imageUrlProvider = { it.outfit.imageUrl },
                    tagProvider = { it.tags.map { tag -> tag.name } },
                    onItemClick = { outfitWithPieces ->
                        Log.d(
                            "ClosetScreen",
                            "Outfit clicked: ID=${outfitWithPieces.outfit.id}, " +
                                "pieces=${outfitWithPieces.pieces.map { it.id }}"
                        )
                        navController.navigate(
                            "${AppDestinations.HOME.name}?outfitId=${outfitWithPieces.outfit.id}"
                        ) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    isFavoriteProvider = { it.outfit.isFavorite },
                    onFavoriteClick = { outfitWithPieces ->
                        scope.launch {
                            val updatedOutfit = outfitWithPieces.outfit.copy(
                                isFavorite = !outfitWithPieces.outfit.isFavorite
                            )
                            database.outfitDao().updateOutfit(updatedOutfit)
                        }
                    },
                    gridMinSize = 180,
                    cardAspectRatio = 0.6f
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ClosetPreview() {
    SliderSchrankTheme {
        Closet(navController = rememberNavController())
    }
}