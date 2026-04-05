package com.mouli.habittracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mouli.habittracker.ui.screens.auth.AuthScreen
import com.mouli.habittracker.ui.screens.auth.AuthViewModel
import com.mouli.habittracker.ui.screens.dashboard.DashboardScreen
import com.mouli.habittracker.ui.screens.dashboard.DashboardViewModel
import com.mouli.habittracker.ui.screens.onboarding.PandaInterviewScreen
import com.mouli.habittracker.ui.screens.onboarding.OnboardingViewModel
import com.mouli.habittracker.ui.theme.HabTheme

private object HabRoute {
    const val Auth = "auth"
    const val Interview = "interview"
    const val Dashboard = "dashboard"
}

@Composable
fun HabApp() {
    val context = LocalContext.current
    val application = context.applicationContext as HabApplication
    val navController = rememberNavController()
    val sessionViewModel: SessionViewModel = viewModel(
        factory = SessionViewModel.Factory(application.container)
    )
    val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(application.container)
    )
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val onboardingViewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.Factory(application, application.container)
    )
    val onboardingState by onboardingViewModel.uiState.collectAsStateWithLifecycle()
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(application, application.container)
    )
    val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val webClientId = remember(context) {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resourceId == 0) "" else context.getString(resourceId)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }
    val targetRoute = remember(sessionState) {
        val localOnlyProfile = sessionState.profile?.takeIf { it.userId == null }
        when {
            sessionState.isLoading -> null
            sessionState.currentUser != null && sessionState.profile?.onboardingCompleted == true -> HabRoute.Dashboard
            sessionState.currentUser != null -> HabRoute.Interview
            localOnlyProfile?.onboardingCompleted == true -> HabRoute.Dashboard
            localOnlyProfile != null -> HabRoute.Interview
            else -> HabRoute.Auth
        }
    }

    LaunchedEffect(targetRoute) {
        val route = targetRoute ?: return@LaunchedEffect
        android.util.Log.d("HabApp", "Navigating to $route (current: $currentRoute)")
        if (currentRoute == route) return@LaunchedEffect
        
        navController.navigate(route) {
            // Pop up to the starting destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    LaunchedEffect(targetRoute) {
        if (
            targetRoute == HabRoute.Dashboard &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    HabTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (sessionState.isLoading && currentRoute == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                NavHost(
                    navController = navController,
                    startDestination = HabRoute.Auth
                ) {
                    composable(HabRoute.Auth) {
                        AuthScreen(
                            uiState = authUiState,
                            googleClientId = webClientId,
                            onGoogleToken = authViewModel::onGoogleTokenReceived,
                            onGoogleFailure = authViewModel::onError
                        )
                    }
                    composable(HabRoute.Interview) {
                        PandaInterviewScreen(
                            state = onboardingState,
                            onNameChanged = onboardingViewModel::updateName,
                            onAddHabit = onboardingViewModel::addHabitDraft,
                            onRemoveHabit = onboardingViewModel::removeHabitDraft,
                            onHabitChanged = onboardingViewModel::updateHabitDraft,
                            onAddReminder = onboardingViewModel::addReminderTime,
                            onRemoveReminder = onboardingViewModel::removeReminderTime,
                            onFinish = {
                                onboardingViewModel.completeInterview(
                                    userId = sessionState.currentUser?.uid,
                                    fallbackDisplayName = sessionState.currentUser?.displayName
                                )
                            }
                        )
                    }
                    composable(HabRoute.Dashboard) {
                        DashboardScreen(
                            state = dashboardState,
                            onIncrement = dashboardViewModel::incrementHabit,
                            onToggleFreeze = dashboardViewModel::toggleFreeze,
                            onBacklogAmountChanged = dashboardViewModel::setBacklogAmount,
                            onBacklogFreezeChanged = dashboardViewModel::setBacklogFreeze,
                            onBookQueryChanged = dashboardViewModel::updateBookSearchQuery,
                            onAddBookSuggestion = dashboardViewModel::addBookSuggestion,
                            onManualAddBook = dashboardViewModel::manualAddBook,
                            onIncrementBookProgress = dashboardViewModel::incrementBookProgress,
                            onUpdateBookPages = dashboardViewModel::updateBookPages,
                            onMoveBookToStatus = dashboardViewModel::moveBookToStatus,
                            onDeleteBook = dashboardViewModel::deleteBook,
                            onAddHabit = dashboardViewModel::addHabit,
                            onUpdateHabit = dashboardViewModel::updateHabit,
                            onDeleteHabit = dashboardViewModel::deleteHabit,
                            onRefreshSync = dashboardViewModel::triggerManualSync,
                            onSignOut = authViewModel::signOut
                        )
                    }
                }
            }
        }
    }
}
