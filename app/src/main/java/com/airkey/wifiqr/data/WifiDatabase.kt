package com.airkey.wifiqr.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiDao {
    @Query("SELECT * FROM wifi_networks ORDER BY savedAt DESC")
    fun getAllNetworks(): Flow<List<WifiNetwork>>

    @Query("SELECT * FROM wifi_networks WHERE isFavorite = 1 ORDER BY savedAt DESC")
    fun getFavoriteNetworks(): Flow<List<WifiNetwork>>

    @Query("SELECT * FROM wifi_networks WHERE ssid LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY savedAt DESC")
    fun searchNetworks(query: String): Flow<List<WifiNetwork>>

    @Query("SELECT * FROM wifi_networks WHERE category = :category ORDER BY savedAt DESC")
    fun getByCategory(category: String): Flow<List<WifiNetwork>>

    @Query("SELECT * FROM wifi_networks WHERE id = :id")
    suspend fun getById(id: Long): WifiNetwork?

    @Query("SELECT * FROM wifi_networks WHERE ssid = :ssid LIMIT 1")
    suspend fun getBySsid(ssid: String): WifiNetwork?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(network: WifiNetwork): Long

    @Update
    suspend fun update(network: WifiNetwork)

    @Delete
    suspend fun delete(network: WifiNetwork)

    @Query("DELETE FROM wifi_networks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM wifi_networks")
    suspend fun getCount(): Int

    @Query("UPDATE wifi_networks SET isFavorite = :isFav WHERE id = :id")
    suspend fun setFavorite(id: Long, isFav: Boolean)

    @Query("UPDATE wifi_networks SET lastConnected = :time WHERE id = :id")
    suspend fun updateLastConnected(id: Long, time: Long)

    @Query("UPDATE wifi_networks SET qrCodeImagePath = :path WHERE id = :id")
    suspend fun updateQrImagePath(id: Long, path: String?)

    @Query("SELECT * FROM wifi_networks ORDER BY savedAt DESC")
    suspend fun getAllNetworksList(): List<WifiNetwork>
}

@Dao
interface ConnectionEventDao {
    @Insert
    suspend fun insert(event: ConnectionEvent): Long

    @Update
    suspend fun update(event: ConnectionEvent)

    @Query("SELECT * FROM connection_events WHERE networkId = :networkId ORDER BY connectedAt DESC")
    fun getEventsForNetwork(networkId: Long): Flow<List<ConnectionEvent>>

    @Query("SELECT * FROM connection_events WHERE networkId = :networkId ORDER BY connectedAt DESC")
    suspend fun getEventsForNetworkList(networkId: Long): List<ConnectionEvent>

    @Query("SELECT * FROM connection_events ORDER BY connectedAt DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 100): Flow<List<ConnectionEvent>>

    @Query("SELECT COUNT(*) FROM connection_events WHERE networkId = :networkId")
    suspend fun getConnectionCount(networkId: Long): Int

    @Query("DELETE FROM connection_events WHERE networkId = :networkId")
    suspend fun deleteForNetwork(networkId: Long)

    @Query("SELECT * FROM connection_events WHERE id = :id")
    suspend fun getById(id: Long): ConnectionEvent?
}

@Dao
interface GeofenceConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: GeofenceConfig)

    @Delete
    suspend fun delete(config: GeofenceConfig)

    @Query("SELECT * FROM geofence_configs WHERE networkId = :networkId")
    suspend fun getForNetwork(networkId: Long): GeofenceConfig?

    @Query("SELECT * FROM geofence_configs WHERE enabled = 1")
    suspend fun getAllEnabled(): List<GeofenceConfig>

    @Query("SELECT * FROM geofence_configs")
    fun getAllFlow(): Flow<List<GeofenceConfig>>

    @Query("UPDATE geofence_configs SET enabled = :enabled WHERE networkId = :networkId")
    suspend fun setEnabled(networkId: Long, enabled: Boolean)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS connection_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                networkId INTEGER NOT NULL,
                ssid TEXT NOT NULL,
                connectedAt INTEGER NOT NULL,
                downloadSpeedMbps REAL,
                uploadSpeedMbps REAL,
                pingMs INTEGER,
                signalDbm INTEGER,
                frequencyMhz INTEGER,
                linkSpeedMbps INTEGER
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS geofence_configs (
                networkId INTEGER PRIMARY KEY NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                radiusMeters REAL NOT NULL DEFAULT 100,
                enabled INTEGER NOT NULL DEFAULT 1,
                label TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent())
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE wifi_networks ADD COLUMN qrCodeImagePath TEXT")
    }
}

@Database(
    entities = [WifiNetwork::class, ConnectionEvent::class, GeofenceConfig::class],
    version = 3,
    exportSchema = false
)
abstract class WifiDatabase : RoomDatabase() {
    abstract fun wifiDao(): WifiDao
    abstract fun connectionEventDao(): ConnectionEventDao
    abstract fun geofenceConfigDao(): GeofenceConfigDao

    companion object {
        @Volatile private var INSTANCE: WifiDatabase? = null

        fun getDatabase(context: Context): WifiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WifiDatabase::class.java,
                    "airkey_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
