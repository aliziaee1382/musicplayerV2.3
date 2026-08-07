package com.example.model

data class EqualizerPreset(
    val name: String,
    val gains: List<Float> // 5 bands: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz (range -12f to +12f)
) {
    companion object {
        val Flat = EqualizerPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f))
        val BassBoost = EqualizerPreset("Bass Boost", listOf(8f, 6f, 2f, 0f, -1f))
        val Pop = EqualizerPreset("Pop", listOf(-1f, 2f, 5f, 3f, -2f))
        val Vocal = EqualizerPreset("Vocal", listOf(-2f, 0f, 6f, 4f, 1f))
        val Jazz = EqualizerPreset("Jazz", listOf(3f, 2f, 1f, 2f, 4f))
        val Rock = EqualizerPreset("Rock", listOf(6f, 3f, -1f, 4f, 6f))
        val EDM = EqualizerPreset("EDM", listOf(7f, 5f, 0f, 3f, 5f))
        val Custom = EqualizerPreset("Custom", listOf(0f, 0f, 0f, 0f, 0f))

        val PRESETS = listOf(Flat, BassBoost, Pop, Vocal, Jazz, Rock, EDM, Custom)
    }
}
