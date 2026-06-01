package com.airkey.wifiqr.data

import android.content.Context
import androidx.room.*
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

    @Query("SELECT * FROM wifi_networks ORDER BY savedAt DESC")
    suspend fun getAllNetworksList(): List<WifiNetwork>
}

@Database(entities = [WifiNetwork::class], version = 1, exportSchema = false)
abstract class WifiDatabase : RoomDatabase() {
    abstract fun wifiDao(): WifiDao

    companion object {
        @Volatile private var INSTANCE: WifiDatabase? = null

        fun getDatabase(context: Context): WifiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WifiDatabase::class.java,
                    "airkey_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
