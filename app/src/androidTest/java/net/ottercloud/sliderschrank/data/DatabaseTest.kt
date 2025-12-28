/*
 * Copyright (c) 2025 OtterCloud
 *
 * Redistribution and use in source and binary forms, with or without * modification, are permitted provided that the following conditions are met:
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
package net.ottercloud.sliderschrank.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.ottercloud.sliderschrank.data.dao.CategoryDao
import net.ottercloud.sliderschrank.data.dao.OutfitDao
import net.ottercloud.sliderschrank.data.dao.PieceDao
import net.ottercloud.sliderschrank.data.dao.TagDao
import net.ottercloud.sliderschrank.data.model.Category
import net.ottercloud.sliderschrank.data.model.Colour
import net.ottercloud.sliderschrank.data.model.Outfit
import net.ottercloud.sliderschrank.data.model.OutfitPieceCrossRef
import net.ottercloud.sliderschrank.data.model.OutfitTagCrossRef
import net.ottercloud.sliderschrank.data.model.Piece
import net.ottercloud.sliderschrank.data.model.PieceTagCrossRef
import net.ottercloud.sliderschrank.data.model.Slot
import net.ottercloud.sliderschrank.data.model.Tag
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var tagDao: TagDao
    private lateinit var pieceDao: PieceDao
    private lateinit var outfitDao: OutfitDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        categoryDao = db.categoryDao()
        tagDao = db.tagDao()
        pieceDao = db.pieceDao()
        outfitDao = db.outfitDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndReadCategory() = runBlocking {
        val category = Category(name = "T-Shirts")
        categoryDao.insertCategory(category)
        val allCategories = categoryDao.getAllCategories().first()
        assertEquals(allCategories[0].name, "T-Shirts")
    }

    @Test
    fun insertAndReadTag() = runBlocking {
        val tag = Tag(name = "Summer")
        tagDao.insertTag(tag)
        val allTags = tagDao.getAllTags().first()
        assertEquals(allTags[0].name, "Summer")
    }

    @Test
    fun insertPieceWithDetails() = runBlocking {
        // Setup Category
        val categoryId = categoryDao.insertCategory(Category(name = "Shirts"))

        // Setup Tag
        val tagId = tagDao.insertTag(Tag(name = "Formal"))

        // Setup Piece
        val piece = Piece(
            imageUrl = "url",
            colour = Colour.WHITE,
            slot = Slot.TOP,
            categoryId = categoryId
        )
        val pieceId = pieceDao.insertPiece(piece)

        // Add Relation
        pieceDao.insertPieceTagCrossRef(PieceTagCrossRef(pieceId, tagId))

        // Verify
        val pieceWithDetails = pieceDao.getPieceWithDetailsById(pieceId).first()
        assertNotNull(pieceWithDetails)
        assertEquals("Shirts", pieceWithDetails?.category?.name)
        assertTrue(pieceWithDetails!!.tags.any { it.name == "Formal" })
    }

    @Test
    fun insertOutfitWithDetailsTransaction() = runBlocking {
        // Setup Pieces
        val p1 = Piece(imageUrl = "p1", colour = Colour.BLACK, slot = Slot.TOP)
        val p2 = Piece(imageUrl = "p2", colour = Colour.BLUE, slot = Slot.BOTTOM)
        val p1Id = pieceDao.insertPiece(p1)
        val p2Id = pieceDao.insertPiece(p2)

        // Setup Tag
        val t1Id = tagDao.insertTag(Tag(name = "Work"))

        // Create Outfit via Transaction
        val outfit = Outfit(imageUrl = "outfit_url")
        outfitDao.insertOutfitWithDetails(
            outfit = outfit,
            pieceIds = listOf(p1Id, p2Id),
            tagIds = listOf(t1Id)
        )

        // Verify
        val allOutfits = outfitDao.getAllOutfitsWithPieces().first()
        assertEquals(1, allOutfits.size)
        val result = allOutfits[0]
        assertEquals(2, result.pieces.size)
        assertEquals(1, result.tags.size)
        assertEquals("Work", result.tags[0].name)
    }

    @Test
    fun updateOutfitDetails() = runBlocking {
        // Initial Setup
        val p1Id = pieceDao.insertPiece(
            Piece(imageUrl = "p1", colour = Colour.RED, slot = Slot.TOP)
        )
        val t1Id = tagDao.insertTag(Tag(name = "Old"))
        val outfit = Outfit(imageUrl = "o1")
        val outfitId = outfitDao.insertOutfit(outfit)
        val outfitWithId = Outfit(id = outfitId, imageUrl = "o1")

        outfitDao.insertOutfitPieceCrossRef(OutfitPieceCrossRef(outfitId, p1Id))
        outfitDao.insertOutfitTagCrossRef(OutfitTagCrossRef(outfitId, t1Id))

        // Update to new details
        val p2Id = pieceDao.insertPiece(
            Piece(imageUrl = "p2", colour = Colour.GREEN, slot = Slot.BOTTOM)
        )
        val t2Id = tagDao.insertTag(Tag(name = "New"))

        outfitDao.updateOutfitWithDetails(
            outfit = outfitWithId,
            pieceIds = listOf(p2Id),
            tagIds = listOf(t2Id)
        )

        // Verify
        val result = outfitDao.getOutfitWithPiecesById(outfitId).first()
        assertEquals(1, result?.pieces?.size)
        assertEquals("p2", result?.pieces?.get(0)?.imageUrl)
        assertEquals(1, result?.tags?.size)
        assertEquals("New", result?.tags?.get(0)?.name)
    }
}