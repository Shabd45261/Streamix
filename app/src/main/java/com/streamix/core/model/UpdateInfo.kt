package com.streamix.core.model

data class UpdateInfo(
    val latestVersion: Int,
    val versionName: String,
    val apkUrl: String,
    val mandatory: Boolean,
    val releaseNotes: List<String>
)
