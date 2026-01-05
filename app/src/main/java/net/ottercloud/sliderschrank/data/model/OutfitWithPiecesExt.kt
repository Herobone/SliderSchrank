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
package net.ottercloud.sliderschrank.data.model

import net.ottercloud.sliderschrank.data.dao.OutfitDao

/**
 * Extension functions to make tag management easier on OutfitWithPieces
 */

/**
 * Add tags to this outfit. Tags are referenced by name and will be created if they don't exist.
 * @param dao The OutfitDao to use for database operations
 * @param tagNames List of tag names to add
 */
suspend fun OutfitWithPieces.addTags(dao: OutfitDao, tagNames: List<String>) {
    tagNames.forEach { tagName ->
        dao.addTagToOutfit(outfit.id, tagName)
    }
}

/**
 * Remove tags from this outfit.
 * @param dao The OutfitDao to use for database operations
 * @param tagNames List of tag names to remove
 */
suspend fun OutfitWithPieces.removeTags(dao: OutfitDao, tagNames: List<String>) {
    val tagsToRemove = tags.filter { it.name in tagNames }
    tagsToRemove.forEach { tag ->
        dao.removeTagFromOutfit(outfit.id, tag.id)
    }
}

/**
 * Set tags on this outfit (replaces all existing tags).
 * @param dao The OutfitDao to use for database operations
 * @param tagNames List of tag names to set
 */
suspend fun OutfitWithPieces.setTags(dao: OutfitDao, tagNames: List<String>) {
    // Remove all existing tags
    tags.forEach { tag ->
        dao.removeTagFromOutfit(outfit.id, tag.id)
    }
    // Add new tags
    addTags(dao, tagNames)
}

/**
 * Get tag names as a list of strings
 */
val OutfitWithPieces.tagNames: List<String>
    get() = tags.map { it.name }