package com.smsms.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smsms.app.ui.orders.CreateOrderScreen
import com.smsms.app.ui.orders.OrderDetailScreen
import com.smsms.app.ui.orders.OrderListScreen

object Routes {
    const val ORDER_LIST = "orders"
    const val CREATE_ORDER = "orders/create"
    const val ORDER_DETAIL = "orders/{orderId}"
    fun orderDetail(orderId: Int) = "orders/$orderId"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.ORDER_LIST) {
        composable(Routes.ORDER_LIST) {
            OrderListScreen(
                onOrderClick = { orderId -> navController.navigate(Routes.orderDetail(orderId)) },
                onCreateOrderClick = { navController.navigate(Routes.CREATE_ORDER) }
            )
        }
        composable(Routes.CREATE_ORDER) {
            CreateOrderScreen(onOrderCreated = { navController.popBackStack() })
        }
        composable(
            route = Routes.ORDER_DETAIL,
            arguments = listOf(navArgument("orderId") { type = NavType.IntType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getInt("orderId") ?: return@composable
            OrderDetailScreen(orderId = orderId, onBack = { navController.popBackStack() })
        }
    }
}
