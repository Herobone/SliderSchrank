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
data class Garment(
    val id: Int,
    val name: String,
    val type: GarmentType
)

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