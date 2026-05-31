package com.accu.service

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// ── Permission result codes (mirror IAccuService.aidl constants) ──────────────
const val ACCU_PERMISSION_GRANTED         = 0
const val ACCU_PERMISSION_DENIED          = 1
const val ACCU_PERMISSION_NOT_REQUESTED   = -1
const val ACCU_PERMISSION_SERVICE_ERROR   = -2

// ── Scope names ───────────────────────────────────────────────────────────────
/** Full arbitrary shell execution (sh -c). Highest trust. */
const val SCOPE_SHELL           = "SHELL"
/** Install / uninstall / enable / disable / hide packages. */
const val SCOPE_PACKAGE_MANAGE  = "PACKAGE_MANAGE"
/** Grant / revoke runtime permissions, set AppOps. */
const val SCOPE_PERMISSIONS     = "PERMISSIONS"
/** Write/read Settings.Secure / Settings.Global / Settings.System. */
const val SCOPE_SETTINGS        = "SETTINGS"
/** Set per-app locale via ActivityManager. */
const val SCOPE_LOCALE          = "LOCALE"
/** Grants ALL scopes — equivalent to root/Shizuku full access. */
const val SCOPE_ALL             = "ALL"

val ALL_SCOPES = setOf(
    SCOPE_SHELL, SCOPE_PACKAGE_MANAGE, SCOPE_PERMISSIONS, SCOPE_SETTINGS, SCOPE_LOCALE
)

// ── Data model ────────────────────────────────────────────────────────────────
data class AccuClientGrant(
    val packageName: String,
    val appLabel: String,
    val grantedScopes: Set<String>,
    val grantedAt: Long,
    val lastUsedAt: Long,
    val isGranted: Boolean,          // false = explicitly denied
    val callCount: Long = 0,
)

// ── Manager ───────────────────────────────────────────────────────────────────
@Singleton
class AccuPermissionManager @Inject constructor() {

    private lateinit var prefs: SharedPreferences
    private val _grants = MutableStateFlow<List<AccuClientGrant>>(emptyList())
    val grants: StateFlow<List<AccuClientGrant>> = _grants.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("accu_api_grants", Context.MODE_PRIVATE)
        _grants.value = loadAll(context)
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    fun checkPermission(packageName: String): Int {
        val grant = _grants.value.find { it.packageName == packageName }
            ?: return ACCU_PERMISSION_NOT_REQUESTED
        return if (grant.isGranted) ACCU_PERMISSION_GRANTED else ACCU_PERMISSION_DENIED
    }

    fun hasScope(packageName: String, scope: String): Boolean {
        val grant = _grants.value.find { it.packageName == packageName } ?: return false
        if (!grant.isGranted) return false
        return SCOPE_ALL in grant.grantedScopes || scope in grant.grantedScopes
    }

    fun isGranted(packageName: String) = checkPermission(packageName) == ACCU_PERMISSION_GRANTED

    // ── Mutate ────────────────────────────────────────────────────────────────

    fun grant(context: Context, packageName: String, scopes: Set<String>) {
        val label = getAppLabel(context, packageName)
        val existing = _grants.value.find { it.packageName == packageName }
        val updated = AccuClientGrant(
            packageName  = packageName,
            appLabel     = label,
            grantedScopes = scopes,
            grantedAt    = existing?.grantedAt ?: System.currentTimeMillis(),
            lastUsedAt   = System.currentTimeMillis(),
            isGranted    = true,
            callCount    = existing?.callCount ?: 0L,
        )
        persist(updated)
        Timber.i("ACCU: Granted $packageName scopes=$scopes")
    }

    fun deny(context: Context, packageName: String) {
        val label = getAppLabel(context, packageName)
        val updated = AccuClientGrant(
            packageName  = packageName,
            appLabel     = label,
            grantedScopes = emptySet(),
            grantedAt    = System.currentTimeMillis(),
            lastUsedAt   = System.currentTimeMillis(),
            isGranted    = false,
        )
        persist(updated)
        Timber.i("ACCU: Denied $packageName")
    }

    fun revoke(packageName: String) {
        val updated = _grants.value.map {
            if (it.packageName == packageName) it.copy(isGranted = false, grantedScopes = emptySet())
            else it
        }
        _grants.value = updated
        persistAll(updated)
        Timber.i("ACCU: Revoked $packageName")
    }

    fun delete(packageName: String) {
        val updated = _grants.value.filter { it.packageName != packageName }
        _grants.value = updated
        persistAll(updated)
    }

    fun recordCall(packageName: String) {
        val updated = _grants.value.map {
            if (it.packageName == packageName)
                it.copy(lastUsedAt = System.currentTimeMillis(), callCount = it.callCount + 1)
            else it
        }
        _grants.value = updated
        // Throttled write — only every 10 calls
        val count = updated.find { it.packageName == packageName }?.callCount ?: 0
        if (count % 10 == 0L) persistAll(updated)
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun persist(grant: AccuClientGrant) {
        val all = (_grants.value.filter { it.packageName != grant.packageName } + grant)
        _grants.value = all
        persistAll(all)
    }

    private fun persistAll(grants: List<AccuClientGrant>) {
        val arr = JSONArray()
        grants.forEach { g ->
            val obj = JSONObject().apply {
                put("pkg",        g.packageName)
                put("label",      g.appLabel)
                put("scopes",     JSONArray(g.grantedScopes.toList()))
                put("grantedAt",  g.grantedAt)
                put("lastUsed",   g.lastUsedAt)
                put("granted",    g.isGranted)
                put("calls",      g.callCount)
            }
            arr.put(obj)
        }
        prefs.edit { putString("grants_v1", arr.toString()) }
    }

    private fun loadAll(context: Context): List<AccuClientGrant> {
        val raw = prefs.getString("grants_v1", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val scopesArr = obj.optJSONArray("scopes") ?: JSONArray()
                val scopes = (0 until scopesArr.length()).map { scopesArr.getString(it) }.toSet()
                val pkg = obj.getString("pkg")
                AccuClientGrant(
                    packageName   = pkg,
                    appLabel      = obj.optString("label").ifBlank { getAppLabel(context, pkg) },
                    grantedScopes = scopes,
                    grantedAt     = obj.optLong("grantedAt", 0L),
                    lastUsedAt    = obj.optLong("lastUsed", 0L),
                    isGranted     = obj.optBoolean("granted", false),
                    callCount     = obj.optLong("calls", 0L),
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "ACCU: Failed to load grants")
            emptyList()
        }
    }

    private fun getAppLabel(context: Context, packageName: String): String {
        return try {
            val info: ApplicationInfo = context.packageManager.getApplicationInfo(
                packageName, PackageManager.GET_META_DATA
            )
            context.packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) { packageName }
    }
}
