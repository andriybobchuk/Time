package com.andriybobchuk.time.app


import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.andriybobchuk.time.time.presentation.AnalyticsScreen as TimeAnalyticsScreen
import com.andriybobchuk.time.time.presentation.AnalyticsViewModel as TimeAnalyticsViewModel
import com.andriybobchuk.time.time.presentation.TimeTrackingScreen
import com.andriybobchuk.time.time.presentation.TimeTrackingViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NavigationHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Route.TimeGraph
    ) {
//        navigation<Route.MooneyGraph>(
//            startDestination = Route.Transactions
//        ) {
//            composable<Route.Transactions> {
//                val viewModel = koinViewModel<TransactionViewModel>()
//                TransactionsScreen(
//                    viewModel = viewModel,
//                    bottomNavbar = { BottomNavigationBar(navController, 0) }
//                )
//            }
//            composable<Route.Accounts> {
//                val viewModel = koinViewModel<AccountViewModel>()
//                AccountScreen(
//                    viewModel = viewModel,
//                    bottomNavbar = { BottomNavigationBar(navController, 1) }
//                )
//            }
//
//            composable<Route.Analytics> {
//                val viewModel = koinViewModel<AnalyticsViewModel>()
//                AnalyticsScreen(
//                    viewModel = viewModel,
//                    bottomNavbar = { BottomNavigationBar(navController, 2) }
//                )
//            }
//        }
        
        navigation<Route.TimeGraph>(
            startDestination = Route.TimeTracking
        ) {
            composable<Route.TimeTracking> {
                val viewModel = koinViewModel<TimeTrackingViewModel>()
                TimeTrackingScreen(
                    viewModel = viewModel,
                    bottomNavbar = { TimeBottomNavigationBar(navController, 0) }
                )
            }
            composable<Route.TimeAnalytics> {
                val viewModel = koinViewModel<TimeAnalyticsViewModel>()
                TimeAnalyticsScreen(
                    viewModel = viewModel,
                    bottomNavbar = { TimeBottomNavigationBar(navController, 1) }
                )
            }
        }
    }

}

@Composable
private inline fun <reified T : ViewModel> NavBackStackEntry.sharedKoinViewModel(
    navController: NavController
): T {
    val navGraphRoute = destination.parent?.route ?: return koinViewModel<T>()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return koinViewModel(
        viewModelStoreOwner = parentEntry
    )
}
