package com.example.ui.screens.main

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.billing.BillingPOSScreen
import com.example.ui.screens.appointments.AppointmentsScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.expenses.ExpensesScreen
import com.example.ui.screens.inventory.InventoryScreen
import com.example.ui.screens.more.MoreScreen
import com.example.ui.theme.*

sealed class Screen(val route: String, val title: String, val filledIcon: androidx.compose.ui.graphics.vector.ImageVector, val outlinedIcon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard, Icons.Outlined.Dashboard)
    object Billing : Screen("billing", "Billing", Icons.Default.PointOfSale, Icons.Outlined.PointOfSale)
    object Appointments : Screen("appointments", "Bookings", Icons.Default.Event, Icons.Outlined.Event)
    object Inventory : Screen("inventory", "Inventory", Icons.Default.Inventory, Icons.Outlined.Inventory2)
    object Expenses : Screen("expenses", "Expenses", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong)
    object More : Screen("more", "More", Icons.Default.MoreHoriz, Icons.Outlined.MoreHoriz)
}

@Composable
fun MainScaffold(onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("syncpos_settings", android.content.Context.MODE_PRIVATE) }
    var currentCategory by remember { mutableStateOf(sharedPrefs.getString("business_category", "F&B") ?: "F&B") }

    DisposableEffect(sharedPrefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "business_category") {
                currentCategory = prefs.getString("business_category", "F&B") ?: "F&B"
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val screens = remember(currentCategory) {
        if (currentCategory == "Service-Based") {
            listOf(
                Screen.Dashboard,
                Screen.Billing,
                Screen.Appointments,
                Screen.Inventory,
                Screen.More
            )
        } else {
            listOf(
                Screen.Dashboard,
                Screen.Billing,
                Screen.Inventory,
                Screen.More
            )
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                // Divider line above the bottom nav
                HorizontalDivider(color = SystemInactive, thickness = 1.dp)

                // Beautiful custom bottom navigation bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(Color.White)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    screens.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        val displayTitle = if (screen == Screen.Inventory && currentCategory == "Service-Based") "Services" else screen.title

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                // Indicator top line (space stays persistent to prevent layout shift)
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isSelected) BrandPrimary else Color.Transparent)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Icon(
                                    imageVector = if (isSelected) screen.filledIcon else screen.outlinedIcon,
                                    contentDescription = displayTitle,
                                    tint = if (isSelected) BrandPrimary else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = displayTitle,
                                    fontSize = 11.sp,
                                    color = if (isSelected) BrandPrimary else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Billing.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToExpenses = {
                        navController.navigate(Screen.Expenses.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToInventory = {
                        navController.navigate(Screen.Inventory.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToBilling = {
                        navController.navigate(Screen.Billing.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Billing.route) {
                BillingPOSScreen()
            }
            composable(Screen.Appointments.route) {
                AppointmentsScreen()
            }
            composable(Screen.Inventory.route) {
                InventoryScreen()
            }
            composable(Screen.Expenses.route) {
                ExpensesScreen()
            }
            composable(Screen.More.route) {
                MoreScreen(
                    onNavigateToExpenses = {
                        navController.navigate(Screen.Expenses.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSignOut = onSignOut
                )
            }
        }
    }
}
