package com.aritxonly.teak.common.material

enum class TeakMaterialLevel(
    val key: String,
    val blurRadius: Float,
    val blurSaturation: Float,
) {
    CrystalClear("crystal_clear", 28f, 1.05f),
    Light("light", 60f, 1.02f),
    Soft("soft", 128f, 1.00f),
    Hazy("hazy", 196f, 0.98f),
    Deep("deep", 256f, 0.95f);

    companion object {
        fun fromKey(value: String?): TeakMaterialLevel = entries.firstOrNull { it.key == value } ?: Soft
    }
}
