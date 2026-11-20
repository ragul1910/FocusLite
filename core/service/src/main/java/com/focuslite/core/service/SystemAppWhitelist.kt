package com.focuslite.core.service

/**
 * Registry of critical system packages and safety services that should NEVER be blocked,
 * ensuring emergency calls, SMS, system settings, and app management remain accessible.
 */
object SystemAppWhitelist {

    private val WHITELISTED_PACKAGES = setOf(
        // FocusLite itself
        "com.focuslite.android",

        // System UI & Core Android
        "android",
        "com.android.systemui",

        // Phone & Emergency Services
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.emergency",
        "com.google.android.apps.safetyhub",
        "com.motorola.blur.conversations",

        // SMS & Essential Messaging
        "com.google.android.apps.messaging",
        "com.android.mms",

        // System Settings & Package Management (Guarantees user can always uninstall or modify settings)
        "com.android.settings",
        "com.motorola.coresettingsext",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.android.vending", // Google Play Store

        // Input Methods / Keyboards
        "com.google.android.inputmethod.latin",
        "com.android.inputmethod.latin",

        // Launchers (Home screens)
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher",
        "com.android.launcher3",
        "com.motorola.launcher3",
        "com.motorola.hello.launcher"
    )

    fun isWhitelisted(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return true
        if (WHITELISTED_PACKAGES.contains(packageName)) return true
        if (packageName.startsWith("com.android.systemui")) return true
        if (packageName.startsWith("com.android.settings")) return true
        return false
    }
}
