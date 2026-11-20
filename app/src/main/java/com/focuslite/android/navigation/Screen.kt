package com.focuslite.android.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object AppLimits : Screen("app_limits")
    data object ReelsBlocker : Screen("reels_blocker")
    data object Downtime : Screen("downtime")
    data object UsageAnalytics : Screen("usage_analytics")
}
