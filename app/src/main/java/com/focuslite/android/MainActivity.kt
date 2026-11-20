package com.focuslite.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.focuslite.android.navigation.Screen
import com.focuslite.feature.applimits.AppLimitsScreen
import com.focuslite.feature.dashboard.DashboardScreen
import com.focuslite.feature.downtime.DowntimeScreen
import com.focuslite.feature.onboarding.OnboardingScreen
import com.focuslite.feature.reelsblocker.ReelsBlockerScreen
import com.focuslite.feature.usageanalytics.UsageAnalyticsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as FocusLiteApplication
        val container = app.container

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route
                    ) {
                        composable(Screen.Onboarding.route) {
                            OnboardingScreen(
                                onComplete = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                reelsShieldRepo = container.reelsShieldRepository,
                                onNavigateToLimits = { navController.navigate(Screen.AppLimits.route) },
                                onNavigateToReels = { navController.navigate(Screen.ReelsBlocker.route) },
                                onNavigateToDowntime = { navController.navigate(Screen.Downtime.route) }
                            )
                        }

                        composable(Screen.AppLimits.route) {
                            AppLimitsScreen(
                                appBlockerRepo = container.appBlockerRepository,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.ReelsBlocker.route) {
                            ReelsBlockerScreen(
                                reelsShieldRepo = container.reelsShieldRepository,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Downtime.route) {
                            DowntimeScreen(
                                downtimeRepo = container.downtimeRepository,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.UsageAnalytics.route) {
                            UsageAnalyticsScreen(
                                usageStatsRepo = container.usageStatsRepository,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
