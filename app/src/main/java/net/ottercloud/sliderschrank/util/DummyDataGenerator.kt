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
import net.ottercloud.sliderschrank.data.model.OutfitPieceCrossRef
import net.ottercloud.sliderschrank.data.model.Piece
import net.ottercloud.sliderschrank.data.model.PieceTagCrossRef
import net.ottercloud.sliderschrank.data.model.Slot
import net.ottercloud.sliderschrank.data.model.Tag

object DummyDataGenerator {

    suspend fun generateDummyData(context: Context, database: AppDatabase) {
        withContext(Dispatchers.IO) {
            val pieceDao = database.pieceDao()
            val categoryDao = database.categoryDao()
            val tagDao = database.tagDao()
            val outfitDao = database.outfitDao()

            // Check if data already exists
            if (categoryDao.getAllCategoriesSync().isNotEmpty()) return@withContext

            // Categories
            val casualCatId = categoryDao.insertCategory(Category(name = "Casual"))
            val formalCatId = categoryDao.insertCategory(Category(name = "Formal"))
            val sportCatId = categoryDao.insertCategory(Category(name = "Sport"))

            // Tags
            val summerTagId = tagDao.insertTag(Tag(name = "Summer"))
            val winterTagId = tagDao.insertTag(Tag(name = "Winter"))
            val cottonTagId = tagDao.insertTag(Tag(name = "Cotton"))
            val denimTagId = tagDao.insertTag(Tag(name = "Denim"))

            // Pieces
            val pieces = listOf(
                // Tops (2xxx)
                Piece(
                    imageUrl = getUri(context, R.drawable.img_2002),
                    colour = Colour.BLUE,
                    slot = Slot.TOP,
                    categoryId = casualCatId
                ) to listOf(summerTagId, cottonTagId),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_2003),
                    colour = Colour.RED,
                    slot = Slot.TOP,
                    categoryId = sportCatId
                ) to listOf(summerTagId),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_2004),
                    colour = Colour.GREEN,
                    slot = Slot.TOP,
                    categoryId = casualCatId
                ) to listOf(cottonTagId),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_2005),
                    colour = Colour.WHITE,
                    slot = Slot.TOP,
                    categoryId = formalCatId
                ) to listOf(winterTagId, cottonTagId),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_2006),
                    colour = Colour.BLACK,
                    slot = Slot.TOP,
                    categoryId = formalCatId
                ) to listOf(winterTagId),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_2007),
                    colour = Colour.YELLOW,
                    slot = Slot.TOP,
                    categoryId = casualCatId
                ) to listOf(summerTagId),

                // Bottoms (3xxx)
                Piece(
                    imageUrl = getUri(context, R.drawable.img_3001),
                    colour = Colour.BLUE,
                    slot = Slot.BOTTOM,
                    categoryId = casualCatId
                ) to listOf(denimTagId),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_3002),
                    colour = Colour.BLACK,
                    slot = Slot.BOTTOM,
                    categoryId = formalCatId
                ) to listOf(winterTagId),

                // Feet (4xxx)
                Piece(
                    imageUrl = getUri(context, R.drawable.img_4001),
                    colour = Colour.WHITE,
                    slot = Slot.FEET,
                    categoryId = sportCatId
                ) to listOf(summerTagId),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_4002),
                    colour = Colour.BLACK,
                    slot = Slot.FEET,
                    categoryId = sportCatId
                ) to listOf(summerTagId),
                Piece(
                    imageUrl = getUri(context, R.drawable.img_4003),
                    colour = Colour.BROWN,
                    slot = Slot.FEET,
                    categoryId = casualCatId
                ) to listOf(winterTagId)
            )

            val createdPieceIds = mutableListOf<Long>()
            pieces.forEach { (piece, tagIds) ->
                val pieceId = pieceDao.insertPiece(piece)
                createdPieceIds.add(pieceId)
                tagIds.forEach { tagId ->
                    pieceDao.insertPieceTagCrossRef(PieceTagCrossRef(pieceId, tagId))
                }
            }

            // Outfits
            if (createdPieceIds.size >= 8) {
                val outfitId = outfitDao.insertOutfit(
                    Outfit(
                        // Using top image as outfit image for now
                        imageUrl = getUri(context, R.drawable.img_2002),
                        isFavorite = true
                    )
                )
                // Add some pieces to the outfit
                // Top (img_2002)
                outfitDao.insertOutfitPieceCrossRef(
                    OutfitPieceCrossRef(outfitId, createdPieceIds[0])
                )
                // Bottom (img_3001)
                outfitDao.insertOutfitPieceCrossRef(
                    OutfitPieceCrossRef(outfitId, createdPieceIds[6])
                )
            }
        }
    }

    private fun getUri(context: Context, resourceId: Int): String =
        "android.resource://${context.packageName}/$resourceId"
}