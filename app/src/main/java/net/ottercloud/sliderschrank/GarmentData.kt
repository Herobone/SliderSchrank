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

// --- Data Model ---

/** Defines the category of the garment. */
enum class GarmentType {
    HEAD,
    TOP, // For shirts, pullovers, jackets
    BOTTOM, // For trousers, skirts
    FEET
}

/** Represents a single piece of clothing. */
data class Garment(val id: Int, val name: String, val type: GarmentType)

// --- Mock Data ---

/** A dummy list of garments to display, following your allocation. */
val dummyGarments = listOf(
    // Headwear (Slider 1)
    Garment(1, "Beanie", GarmentType.HEAD),
    Garment(2, "Cap", GarmentType.HEAD),

    // Tops (Slider 2)
    Garment(10, "Blaues Shirt", GarmentType.TOP),
    Garment(11, "Roter Pullover", GarmentType.TOP),
    Garment(12, "Grüne Jacke", GarmentType.TOP),

    // Bottoms (Slider 3)
    Garment(20, "Jeans", GarmentType.BOTTOM),
    Garment(21, "Schwarze Hose", GarmentType.BOTTOM),

    // Footwear (Slider 4)
    Garment(30, "Sneaker", GarmentType.FEET),
    Garment(31, "Stiefel", GarmentType.FEET)
)