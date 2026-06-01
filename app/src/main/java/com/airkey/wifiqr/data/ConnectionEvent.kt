package com.airkey.wifiqr.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connection_events")
data class ConnectionEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val networkId: Long,
    val ssid: String,
    val connectedAt: Long = System.currentTimeMillis(),
    val downloadSpeedMbps: Float? = null,
    val uploadSpeedMbps: Float? = null,
    val pingMs: Int? = null,
    val signalDbm: Int? = null,
    val frequencyMhz: Int? = null,
    val linkSpeedMbps: Int? = null
)
