package com.airkey.wifiqr.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geofence_configs")
data class GeofenceConfig(
    @PrimaryKey val networkId: Long,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 100f,
    val enabled: Boolean = true,
    val label: String = ""
)
