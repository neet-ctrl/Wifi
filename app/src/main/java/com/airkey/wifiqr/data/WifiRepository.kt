package com.airkey.wifiqr.data

import kotlinx.coroutines.flow.Flow

class WifiRepository(private val dao: WifiDao) {
    val allNetworks: Flow<List<WifiNetwork>> = dao.getAllNetworks()
    val favoriteNetworks: Flow<List<WifiNetwork>> = dao.getFavoriteNetworks()

    fun searchNetworks(query: String): Flow<List<WifiNetwork>> = dao.searchNetworks(query)
    fun getByCategory(category: String): Flow<List<WifiNetwork>> = dao.getByCategory(category)

    suspend fun getById(id: Long): WifiNetwork? = dao.getById(id)
    suspend fun getBySsid(ssid: String): WifiNetwork? = dao.getBySsid(ssid)
    suspend fun insert(network: WifiNetwork): Long = dao.insert(network)
    suspend fun update(network: WifiNetwork) = dao.update(network)
    suspend fun delete(network: WifiNetwork) = dao.delete(network)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun getCount(): Int = dao.getCount()
    suspend fun setFavorite(id: Long, isFav: Boolean) = dao.setFavorite(id, isFav)
    suspend fun updateLastConnected(id: Long) = dao.updateLastConnected(id, System.currentTimeMillis())
}
