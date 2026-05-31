package com.accu.sdk

/**
 * ACCU SDK — Permission Scopes
 *
 * ACCU uses a scope-based permission model. When the user grants your app
 * access, they can choose which of these scopes to enable. Your code
 * must only call APIs that belong to scopes you have been granted.
 *
 * Check which scopes you have via:
 *   IAccuService.hasScope(AccuScopes.SHELL)
 *
 * Scopes are granted when the user taps "Grant" in the ACCU permission dialog.
 */
object AccuScopes {

    /**
     * SHELL — Execute arbitrary shell commands via `sh -c`.
     *
     * APIs requiring this scope:
     *   exec(), execAsync(), execAndGetOutput()
     *
     * Risk level: HIGHEST. Only request if your app genuinely needs
     * arbitrary shell execution (e.g. a terminal emulator).
     */
    const val SHELL = "SHELL"

    /**
     * PACKAGE_MANAGE — Install, uninstall, enable, disable, hide, freeze apps.
     *
     * APIs requiring this scope:
     *   installApk(), uninstallPackage(), uninstallKeepData(),
     *   enablePackage(), disablePackage(), hidePackage(), unhidePackage(),
     *   suspendPackage(), unsuspendPackage(), clearPackageData(),
     *   enableComponent(), disableComponent(), forceStop()
     *
     * Risk level: HIGH.
     */
    const val PACKAGE_MANAGE = "PACKAGE_MANAGE"

    /**
     * PERMISSIONS — Grant/revoke runtime permissions and control AppOps.
     *
     * APIs requiring this scope:
     *   grantPermission(), revokePermission(), setAppOp(), getAppOp()
     *
     * Risk level: HIGH.
     */
    const val PERMISSIONS = "PERMISSIONS"

    /**
     * SETTINGS — Read/write Settings.Secure, Settings.Global, Settings.System.
     *
     * APIs requiring this scope:
     *   writeSecureSetting(), readSecureSetting(),
     *   writeGlobalSetting(), readGlobalSetting(),
     *   writeSystemSetting(), readSystemSetting()
     *
     * Risk level: MEDIUM. Can change system behaviour but not install code.
     */
    const val SETTINGS = "SETTINGS"

    /**
     * LOCALE — Set per-app language overrides via ActivityManager.
     *
     * APIs requiring this scope:
     *   setApplicationLocale()
     *
     * Risk level: LOW. Only affects app-display language.
     */
    const val LOCALE = "LOCALE"

    /**
     * ALL — Meta-scope that implies every individual scope above.
     * Returned by hasScope() when the user granted full access.
     * You do NOT request "ALL" directly — it is set automatically
     * when the user taps "Grant Full Access" in ACCU's dialog.
     */
    const val ALL = "ALL"

    /** Convenience set of every individual scope (not including ALL). */
    val ALL_SCOPES: Set<String> = setOf(SHELL, PACKAGE_MANAGE, PERMISSIONS, SETTINGS, LOCALE)
}
