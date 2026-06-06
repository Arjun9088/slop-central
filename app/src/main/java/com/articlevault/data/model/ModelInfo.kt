package com.articlevault.data.model

data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val filename: String,
    val parameterCount: String = "",
    val gated: Boolean = false
) {
    val sizeDisplay: String
        get() {
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb > 1024) "%.1f GB".format(mb / 1024.0)
            else "%.0f MB".format(mb)
        }

    val isDownloaded: Boolean
        get() = false // overridden in repository
}
