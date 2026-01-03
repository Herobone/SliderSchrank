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

import androidx.annotation.DrawableRes
import java.util.Locale

enum class GarmentType {
    HEAD,
    TOP,
    BOTTOM,
    FEET
}

data class Garment(
    val id: Int,
    val name: String,
    val type: GarmentType,
    @param:DrawableRes val imageResId: Int
)

private fun getGarmentTypeFromId(id: Int): GarmentType {
    val idString = id.toString()

    return when {
        idString.startsWith("1") -> GarmentType.HEAD
        idString.startsWith("2") -> GarmentType.TOP
        idString.startsWith("3") -> GarmentType.BOTTOM
        idString.startsWith("4") -> GarmentType.FEET
        else -> GarmentType.HEAD
    }
}

private fun loadGarmentsFromDrawables(): List<Garment> {
    val garments = mutableListOf<Garment>()
    val drawableFields = R.drawable::class.java.fields

    for (field in drawableFields) {
        val resName = field.name

        if (resName.startsWith("img_")) {
            try {
                val idString = resName.removePrefix("img_")
                val id = idString.toInt()

                @DrawableRes val resId = field.getInt(null)

                val type = getGarmentTypeFromId(id)

                val name = "${
                    type.name.lowercase(Locale.ROOT).replaceFirstChar {
                        it.titlecase(Locale.ROOT)
                    }
                } $id"

                garments.add(Garment(id, name, type, resId))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    return garments.sortedBy { it.id }
}

val dummyGarments: List<Garment> = loadGarmentsFromDrawables()