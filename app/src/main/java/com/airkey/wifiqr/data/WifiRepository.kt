package com.airkey.wifiqr.data

import kotlinx.coroutines.flow.Flow

class WifiRepository(
    private val dao: WifiDao,
    private val eventDao: ConnectionEventDao,
    private val geofenceDao: GeofenceConfigDao
) {
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
    suspend fun updateQrImagePath(id: Long, path: String?) = dao.updateQrImagePath(id, path)
    suspend fun getAllNetworksList(): List<WifiNetwork> = dao.getAllNetworksList()

    suspend fun logConnectionEvent(networkId: Long, ssid: String): Long =
        eventDao.insert(ConnectionEvent(networkId = networkId, ssid = ssid))

    suspend fun logConnectionEventFull(event: ConnectionEvent): Long =
        eventDao.insert(event)

    suspend fun updateConnectionEvent(event: ConnectionEvent) = eventDao.update(event)
    suspend fun getEventById(id: Long): ConnectionEvent? = eventDao.getById(id)
    fun getEventsForNetwork(networkId: Long): Flow<List<ConnectionEvent>> =
        eventDao.getEventsForNetwork(networkId)
    suspend fun getEventsForNetworkList(networkId: Long): List<ConnectionEvent> =
        eventDao.getEventsForNetworkList(networkId)
    fun getRecentEvents(limit: Int = 100): Flow<List<ConnectionEvent>> =
        eventDao.getRecentEvents(limit)
    suspend fun getAllEventsList(): List<ConnectionEvent> = eventDao.getAllEventsList()
    suspend fun getConnectionCount(networkId: Long): Int = eventDao.getConnectionCount(networkId)
    suspend fun deleteEventsForNetwork(networkId: Long) = eventDao.deleteForNetwork(networkId)

    suspend fun upsertGeofence(config: GeofenceConfig) = geofenceDao.upsert(config)
    suspend fun deleteGeofence(config: GeofenceConfig) = geofenceDao.delete(config)
    suspend fun getGeofenceForNetwork(networkId: Long): GeofenceConfig? =
        geofenceDao.getForNetwork(networkId)
    suspend fun getAllEnabledGeofences(): List<GeofenceConfig> = geofenceDao.getAllEnabled()
    fun getAllGeofencesFlow(): Flow<List<GeofenceConfig>> = geofenceDao.getAllFlow()
    suspend fun getAllGeofencesList(): List<GeofenceConfig> = geofenceDao.getAllList()
    suspend fun setGeofenceEnabled(networkId: Long, enabled: Boolean) =
        geofenceDao.setEnabled(networkId, enabled)
}
