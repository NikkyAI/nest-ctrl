package nestdrop

sealed class PresetLocation {
    abstract val id: Int
    data class Milk(
        val name: String,
        override val id: Int,
        val path: String,
        val previewPath: String,
        val category: String,
        val subCategory: String? = null,
    ): PresetLocation()

    data class Img(
        val name: String,
        override val id: Int,
        val path: String,
        val category: String,
        val subCategory: String? = null,
    ): PresetLocation()
}

