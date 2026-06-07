package com.automatelinux.picaStats.ui.navigation

import com.automatelinux.picaStats.BuildConfig
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.automatelinux.feedbacklib.ui.chat.FeedbackChatScreen
import com.automatelinux.feedbacklib.ui.issues.FeedbackIssuesScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            Scaffold(
                floatingActionButton = {
                    if (!BuildConfig.IS_PROD) {
                        FloatingActionButton(
                            onClick = { navController.navigate("feedback") },
                            containerColor = MaterialTheme.colorScheme.primary,
                        ) {
                            Icon(
                                Icons.Filled.BugReport,
                                contentDescription = "Report Issue",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "PicaStats",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
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
