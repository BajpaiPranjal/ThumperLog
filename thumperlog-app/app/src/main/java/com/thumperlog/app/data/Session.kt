package com.thumperlog.app.data

data class Sample(val tMs: Long, val mag: Float)

data class Session(
    val id: String,
    val label: String,
    val startTime: Long,
    val duration: Long,
    val peak: Float,
    val avg: Float,
    val samples: List<Sample>
)
