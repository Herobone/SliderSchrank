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
package net.ottercloud.sliderschrank

import android.util.Log
import androidx.compose.runtime.mutableStateListOf

/**
 * Ein temporärer, speicherinterner "Speicher" für favorisierte Outfits.
 * Dies ist ein Singleton-Objekt, sodass die Liste global zugänglich ist.
 *
 * Es verwendet `mutableStateListOf`, damit Composables, die diese Liste
 * beobachten, bei Änderungen automatisch neu komponiert werden.
 *
 * Ein "Outfit" wird als ein Set von Kleidungsstück-IDs (Set<Int>) gespeichert.
 */
object FavoriteOutfitRepository {

    /**
     * Die Liste aller gespeicherten (favorisierten) Outfits.
     * Jedes Element in dieser Liste ist ein Set von Garment-IDs.
     */
    val favoriteOutfits = mutableStateListOf<Set<Int>>()

    fun addFavorite(outfitIds: Set<Int>) {
        if (!favoriteOutfits.contains(outfitIds)) {
            favoriteOutfits.add(outfitIds)
            Log.d("FavoriteOutfitRepo", "Outfit hinzugefügt: $outfitIds")
        }
    }

    fun removeFavorite(outfitIds: Set<Int>) {
        val removed = favoriteOutfits.remove(outfitIds)
        if (removed) {
            Log.d("FavoriteOutfitRepo", "Outfit entfernt: $outfitIds")
        }
    }

    fun isFavorite(outfitIds: Set<Int>): Boolean = favoriteOutfits.contains(outfitIds)
}