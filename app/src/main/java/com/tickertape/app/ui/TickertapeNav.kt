package com.tickertape.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Savings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

const val ROUTE_STOCKS = "stocks"
const val ROUTE_PORTFOLIO = "portfolio"
const val ROUTE_PROFIT = "profit"
const val ROUTE_FORECAST = "forecast"
const val ROUTE_PRODUCTS = "products"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_STOCK = "stock/{sid}"

val AppTabs = listOf(
    TabSpec(ROUTE_STOCKS, "Stocks", Icons.AutoMirrored.Filled.ShowChart),
    TabSpec(ROUTE_PORTFOLIO, "Book", Icons.Filled.AccountBalanceWallet),
    TabSpec(ROUTE_PROFIT, "Profit", Icons.Filled.Savings),
    TabSpec(ROUTE_FORECAST, "Forecast", Icons.Filled.QueryStats),
    TabSpec(ROUTE_PRODUCTS, "Products", Icons.Filled.Inventory2),
)

private val tabRoutes = AppTabs.map { it.route }.toSet()

@Composable
fun TickertapeNav() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tab = route?.takeIf { it in tabRoutes }

    NavHost(navController = nav, startDestination = ROUTE_STOCKS) {
        composable(ROUTE_STOCKS) {
            StocksScreen(
                currentRoute = tab,
                onOpen = { sid -> nav.navigate("stock/$sid") },
                onTab = { dest -> nav.switchTab(dest) },
                onSettings = { nav.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(
            ROUTE_STOCK,
            arguments = listOf(navArgument("sid") { type = NavType.StringType }),
        ) { backStack ->
            StockDetailScreen(
                sid = backStack.arguments?.getString("sid").orEmpty(),
                onBack = { nav.popBackStack() },
                onSettings = { nav.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_PORTFOLIO) {
            PortfolioScreen(
                currentRoute = tab,
                onTab = { dest -> nav.switchTab(dest) },
                onSettings = { nav.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_PROFIT) {
            ProfitScreen(
                currentRoute = tab,
                onTab = { dest -> nav.switchTab(dest) },
                onSettings = { nav.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_FORECAST) {
            ForecastScreen(
                currentRoute = tab,
                onTab = { dest -> nav.switchTab(dest) },
                onSettings = { nav.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_PRODUCTS) {
            ProductsScreen(
                currentRoute = tab,
                onTab = { dest -> nav.switchTab(dest) },
                onSettings = { nav.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}

private fun androidx.navigation.NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
