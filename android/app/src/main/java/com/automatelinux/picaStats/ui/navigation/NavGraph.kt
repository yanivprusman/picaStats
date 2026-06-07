package com.automatelinux.picaStats.ui.navigation

import com.automatelinux.picaStats.BuildConfig
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.automatelinux.feedbacklib.ui.chat.FeedbackChatScreen
import com.automatelinux.feedbacklib.ui.issues.FeedbackIssuesScreen
import com.automatelinux.picaStats.ui.StatsScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            StatsScreen(
                isProd = BuildConfig.IS_PROD,
                onReportIssue = { navController.navigate("feedback") },
            )
        }

        composable("feedback") {
            FeedbackChatScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToIssues = { navController.navigate("issues") },
                isProd = BuildConfig.IS_PROD,
            )
        }

        composable("issues") {
            FeedbackIssuesScreen(
                onNavigateBack = { navController.popBackStack() },
                isProd = BuildConfig.IS_PROD,
            )
        }
    }
}
