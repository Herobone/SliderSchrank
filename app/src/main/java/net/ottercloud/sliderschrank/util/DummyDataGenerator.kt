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
package net.ottercloud.sliderschrank.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ottercloud.sliderschrank.R
import net.ottercloud.sliderschrank.data.AppDatabase
import net.ottercloud.sliderschrank.data.model.Category
import net.ottercloud.sliderschrank.data.model.Colour
import net.ottercloud.sliderschrank.data.model.Outfit
import net.ottercloud.sliderschrank.data.model.Piece
import net.ottercloud.sliderschrank.data.model.Slot

object DummyDataGenerator {

    suspend fun generateDummyData(context: Context, database: AppDatabase) {
        withContext(Dispatchers.IO) {
            val pieceDao = database.pieceDao()
            val categoryDao = database.categoryDao()
            val outfitDao = database.outfitDao()

            // Categories
            val casualCatId = categoryDao.insertCategory(Category(name = "Casual"))
            val formalCatId = categoryDao.insertCategory(Category(name = "Formal"))
            val sportCatId = categoryDao.insertCategory(Category(name = "Sport"))

            // Pieces with their tag names
            val pieces = listOf(
                Piece(
                    imageUrl = getUri(context, R.drawable.img_1001),
                    colour = Colour.BLUE,
                    slot = Slot.HEAD,
                    categoryId = casualCatId
                ) to listOf("Summer", "Cotton"),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_1002),
                    colour = Colour.RED,
                    slot = Slot.HEAD,
                    categoryId = formalCatId
                ) to listOf("Summer"),
                // Tops (2xxx)

                Piece(
                    imageUrl = getUri(context, R.drawable.img_2001),
                    colour = Colour.BLUE,
                    slot = Slot.TOP,
                    categoryId = formalCatId
                ) to listOf("Summer", "Cotton"),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_2002),
                    colour = Colour.BLUE,
                    slot = Slot.TOP,
                    categoryId = casualCatId
                ) to listOf("Summer", "Cotton"),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_2003),
                    colour = Colour.RED,
                    slot = Slot.TOP,
                    categoryId = sportCatId
                ) to listOf("Summer"),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_2004),
                    colour = Colour.GREEN,
                    slot = Slot.TOP,
                    categoryId = casualCatId
                ) to listOf("Cotton"),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_2005),
                    colour = Colour.WHITE,
                    slot = Slot.TOP,
                    categoryId = formalCatId
                ) to listOf("Winter", "Cotton"),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_2006),
                    colour = Colour.BLACK,
                    slot = Slot.TOP,
                    categoryId = formalCatId
                ) to listOf("Winter"),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_2007),
                    colour = Colour.YELLOW,
                    slot = Slot.TOP,
                    categoryId = casualCatId
                ) to listOf("Summer"),

                // Bottoms (3xxx)
                Piece(
                    imageUrl = getUri(context, R.drawable.img_3001),
                    colour = Colour.BLUE,
                    slot = Slot.BOTTOM,
                    categoryId = casualCatId
                ) to listOf("Denim"),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_3002),
                    colour = Colour.BLACK,
                    slot = Slot.BOTTOM,
                    categoryId = formalCatId
                ) to listOf("Winter"),

                // Feet (4xxx)
                Piece(
                    imageUrl = getUri(context, R.drawable.img_4001),
                    colour = Colour.WHITE,
                    slot = Slot.FEET,
                    categoryId = sportCatId
                ) to listOf("Summer"),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_4002),
                    colour = Colour.BLACK,
                    slot = Slot.FEET,
                    categoryId = sportCatId
                ) to listOf("Summer"),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_4003),
                    colour = Colour.BROWN,
                    slot = Slot.FEET,
                    categoryId = casualCatId
                ) to listOf("Winter")
            )

            val createdPieceIds = mutableListOf<Long>()
            pieces.forEach { (piece, tagNames) ->
                val pieceId = pieceDao.insertPiece(piece)
                createdPieceIds.add(pieceId)
                // Add tags using the simplified helper method
                tagNames.forEach { tagName ->
                    pieceDao.addTagToPiece(pieceId, tagName)
                }
            }

            // Create a dummy outfit with actual pieces
            if (createdPieceIds.size >= 4) {
                // Get pieces for the outfit: HEAD, TOP, BOTTOM, FEET
                // HEAD: index 0 (img_1001 - Blue casual hat)
                // TOP: index 3 (img_2002 - Blue casual top)
                // BOTTOM: index 9 (img_3001 - Blue denim jeans)
                // FEET: index 10 (img_4001 - White sport shoes)
                val outfitPieceIds = listOf(
                    createdPieceIds[0], // HEAD
                    createdPieceIds[3], // TOP
                    createdPieceIds[9], // BOTTOM
                    createdPieceIds[13] // FEET
                )

                // Fetch the actual pieces to generate the image
                val outfitPieces = outfitPieceIds.mapNotNull { pieceId ->
                    pieceDao.getPieceById(pieceId)
                }

                // Generate composite outfit image
                val outfitImageUrl = OutfitImageGenerator.generateOutfitImage(context, outfitPieces)

                // Create outfit with generated image
                val outfit = Outfit(
                    imageUrl = outfitImageUrl,
                    isFavorite = true
                )

                // Insert outfit with all pieces and tags
                outfitDao.insertOutfitWithDetails(
                    outfit = outfit,
                    pieceIds = outfitPieceIds,
                    tagIds = emptyList()
                )
            }
        }
    }

    private fun getUri(context: Context, resourceId: Int): String =
        "android.resource://${context.packageName}/$resourceId"
}