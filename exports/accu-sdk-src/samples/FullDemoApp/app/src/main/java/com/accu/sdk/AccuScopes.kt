package com.accu.sdk

object AccuScopes {
    const val SHELL = "SHELL"
    const val PACKAGE_MANAGE = "PACKAGE_MANAGE"
    const val PERMISSIONS = "PERMISSIONS"
    const val SETTINGS = "SETTINGS"
    const val LOCALE = "LOCALE"
    const val ALL = "ALL"

    val ALL_SCOPES: Set<String> = setOf(SHELL, PACKAGE_MANAGE, PERMISSIONS, SETTINGS, LOCALE)
}
