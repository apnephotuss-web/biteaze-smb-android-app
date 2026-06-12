package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToExpenses: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToBilling: () -> Unit,
    viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showFilterDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 86.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // MODULE 1: Sticky Header Component Box (Handles popup positioning)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), clip = false)
                .background(Color.White, shape = RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header Titles
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SHOP DASHBOARD",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.W900,
                        color = Color(0xFF1E293B),
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        }

        // MODULE 2: Key Metrics Bento-Style Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Total Money Earned
                BentoCard(
                    modifier = Modifier.weight(1f),
                    labelText = "TOTAL MONEY EARNED",
                    valueText = "Rs. ${String.format("%,.0f", state.totalRevenue)}",
                    iconColor = BrandPrimary,
                    iconBg = Color(0xFFFFF7ED),
                    icon = Icons.Default.TrendingUp
                )

                // Card 2: Average Bill Value (AOV Indicator)
                val avgBill = if (state.totalOrdersCount > 0) state.totalRevenue / state.totalOrdersCount else 0.0
                BentoCard(
                    modifier = Modifier.weight(1f),
                    labelText = "AVERAGE BILL VALUE",
                    valueText = "Rs. ${String.format("%.2f", avgBill)}",
                    iconColor = Color(0xFF10B981),
                    iconBg = Color(0xFFE6FBF2),
                    icon = Icons.Default.Savings,
                    borderColor = Color(0xFFD1FAE5),
                    textColor = Color(0xFF059669)
                )
            }

            // Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 3: Number of Sales (Bill Counter)
                BentoCard(
                    modifier = Modifier.weight(1f),
                    labelText = "NUMBER OF SALES",
                    valueText = "${state.totalOrdersCount} bills paid",
                    iconColor = BrandSecondary,
                    iconBg = Color(0xFFEEF2FF),
                    icon = Icons.Default.ShoppingBag
                )

                // Card 4: Low Stock Alerts
                BentoCard(
                    modifier = Modifier.weight(1f),
                    labelText = "LOW STOCK ALERTS",
                    valueText = "${state.lowStockCount} items dry",
                    iconColor = Color(0xFFEF4444),
                    iconBg = Color(0xFFFFE4E6),
                    icon = Icons.Default.ErrorOutline,
                    borderColor = Color(0xFFFECDD3),
                    textColor = Color(0xFFE11D48)
                )
            }

            // Card 5: Total Expenses Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToExpenses() }
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFFEF2F2), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = "Expenses",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "TOTAL EXPENSES",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 1.sp
                            )
                        }

                        // more accessible OPEN button
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFEE2E2), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "OPEN",
                                color = Color(0xFFB91C1C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Rs. ${String.format("%,.0f", state.totalExpenses)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFDC2626),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // MODULE 3: Advanced Infographics Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SalesTrendLineChart(title = "7 Days Trend", subtitle = "Revenue pattern during last 7 days", data = state.last7DaysSales, labels = List(7) { "Day ${it + 1}" })

            SalesTrendLineChart(title = "30 Days Trend", subtitle = "Revenue pattern during last 30 days", data = state.last30DaysSales, labels = List(30) { if ((it + 1) % 5 == 0) "D${it + 1}" else "" })

            // Chart 2: 12-Month Revenue Comparison Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header Tag & Titles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFEEF2FF), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ANNUAL SNAPSHOT",
                                    color = BrandSecondary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Revenue Over 12 Months",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Monthly sales lookback performance comparison",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // BAR GRAPH PLATFORM (Height 145dp)
                    val annualRevenue = state.monthlySales
                    val maxVal = annualRevenue.maxOrNull()?.takeIf { it > 0 } ?: 1.0
                    val monthLabels = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
                    var selectedBarIdx by remember { mutableStateOf(-1) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Color(0xFFFAFAFA), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Scrollable Bars Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                Alignment.Bottom
                            ) {
                                annualRevenue.forEachIndexed { i, amount ->
                                    val barHeightFrac = amount / maxVal
                                    val isSelected = selectedBarIdx == i

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.Bottom,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Tooltip bubble on top
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .shadow(4.dp, RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Rs. ${String.format("%,.0f", amount)}",
                                                    color = Color.White,
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }

                                        // Vertical Bar block with top rounded corner
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(barHeightFrac.toFloat())
                                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                                .background(if (isSelected) BrandPrimary else Color(0xFFE2E8F0))
                                                .clickable { selectedBarIdx = if (isSelected) -1 else i }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // X Axis months
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                monthLabels.forEach { m ->
                                    Text(
                                        text = m,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Footer Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Calendar Month Comparison",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(BrandSecondary, RoundedCornerShape(1.dp)))
                            Text(
                                text = "Cumulative Sales",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        // MODULE 4: Tail Insights Section Stacked
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // DETAIL CARD C: Top Selling Items
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFD1FAE5), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DEMAND",
                                color = Color(0xFF10B981),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Top Selling Items",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Highest demand catalog items by gross revenue",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Body List
                    val topSellers = state.topSellers
                    if (topSellers.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                            border = BorderStroke(1.dp, Color(0xFFE0E7FF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "📦 No product sales logged in this period.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3730A3),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 192.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            topSellers.forEach { item ->
                                val gross = item.price * item.quantitySold
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${item.category} • ${item.quantitySold} sold".uppercase(),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF94A3B8),
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Text(
                                        text = "Rs. ${String.format("%,.0f", gross)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF334155),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // DETAIL CARD A: "How Customers Paid" (Tender Distribution)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = "OVERVIEW",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "How Customers Paid",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Payment distribution breakdown during selected filter duration",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Progress Rows
                    val totals = state.paymentCashAmount + state.paymentCardAmount + state.paymentUpiAmount
                    val entries = listOf(
                        Triple("CASH Payment", state.paymentCashAmount, BrandPrimary),
                        Triple("CARD Payment", state.paymentCardAmount, BrandSecondary),
                        Triple("UPI Payment", state.paymentUpiAmount, Color(0xFF10B981)),
                        Triple("OTHERS Payment", 0.0, Color(0xFF94A3B8))
                    )

                    entries.forEach { (label, amt, color) ->
                        val pct = if (totals > 0) (amt / totals) * 100 else 0.0
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "Rs. ${String.format("%,.0f", amt)} (${String.format("%.0f", pct)}%)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155),
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Wide Progress bar tracker
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(2.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(if (totals > 0) (amt / totals).toFloat() else 0f)
                                        .fillMaxHeight()
                                        .background(color, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }

            // DETAIL CARD B: Low Stock Warnings
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFE4E6), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "WARNING",
                                color = Color(0xFFF43F5E),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Low Stock Warnings",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Catalog items currently running low on checkout shelves",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Body
                    val lowStocks = state.lowStockWarningProducts
                    if (lowStocks.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE6FBF2)),
                            border = BorderStroke(1.dp, Color(0xFFD1FAE5))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "☘️ No low stock items! All items are fully loaded.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 192.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            lowStocks.forEach { prod ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(12.dp))
                                        .clickable { onNavigateToInventory() }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "SKU: ${prod.sku.ifBlank { "No SKU" }}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8),
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFEE2E2), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${prod.stock} left",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF991B1B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

    }
}
}

@Composable
fun SalesTrendLineChart(title: String, subtitle: String, data: List<Double>, labels: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Tag & Titles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFF7ED), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CURRENT GRAPH",
                                color = BrandPrimary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // GRAPH CANVAS CONTAINER
            var activeTimelineNode by remember { mutableStateOf(-1) }
            val timelineLabels = labels
            val trendValues = data

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFFFAFAFA), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Spline Line Canvas Section
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidthPx = 2.5.dp.toPx()
                            val nodeRadiusPx = 4.dp.toPx()
                            val outlineStrokePx = 2.dp.toPx()

                            // Guide lines calculations (10%, 50%, 90% height)
                            val yLines = listOf(size.height * 0.1f, size.height * 0.5f, size.height * 0.9f)
                            yLines.forEach { y ->
                                drawLine(
                                    color = Color(0xFFE2E8F0),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1f,
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        intervals = floatArrayOf(10f, 10f),
                                        phase = 0f
                                    )
                                )
                            }

                            // Node coordinates
                            val minVal = trendValues.minOrNull() ?: 0.0
                            val maxVal = trendValues.maxOrNull() ?: 1.0
                            val graphRange = if (maxVal == minVal) 1.0 else (maxVal - minVal)

                            val xPositions = trendValues.indices.map { i ->
                                size.width * (0.1f + (0.8f * (i.toFloat() / (trendValues.size - 1).coerceAtLeast(1))))
                            }

                            val yPositions = trendValues.map { v ->
                                val faction = (v - minVal) / graphRange
                                size.height * (0.85f - (faction.toFloat() * 0.70f))
                            }

                            // Build curved spline path
                            val path = Path().apply {
                                if (xPositions.isNotEmpty()) {
                                    moveTo(xPositions[0], yPositions[0])
                                    for (i in 0 until xPositions.lastIndex) {
                                        val cx = (xPositions[i] + xPositions[i + 1]) / 2f
                                        cubicTo(
                                            cx, yPositions[i],
                                            cx, yPositions[i + 1],
                                            xPositions[i + 1], yPositions[i + 1]
                                        )
                                    }
                                }
                            }

                            // Draw background gradient wave mask under the curve
                            if (xPositions.isNotEmpty()) {
                                val gradientFillPath = Path().apply {
                                    addPath(path)
                                    lineTo(xPositions.last(), size.height)
                                    lineTo(xPositions[0], size.height)
                                    close()
                                }

                                drawPath(
                                    path = gradientFillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFF97316).copy(alpha = 0.20f),
                                            Color(0xFFF97316).copy(alpha = 0.0f)
                                        ),
                                        startY = 0f,
                                        endY = size.height
                                    )
                                )
                            }

                            // Draw orange solid spline line
                            drawPath(
                                path = path,
                                color = BrandPrimary,
                                style = Stroke(
                                    width = strokeWidthPx,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            // Draw dotted vertices nodes
                            xPositions.forEachIndexed { idx, x ->
                                val y = yPositions[idx]
                                drawCircle(
                                    color = Color.White,
                                    radius = nodeRadiusPx,
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = BrandPrimary,
                                    radius = nodeRadiusPx,
                                    center = Offset(x, y),
                                    style = Stroke(width = outlineStrokePx)
                                )
                            }
                        }

                        // Interactive hot spot buttons over canvas nodes (with floating overlay tooltips)
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            trendValues.forEachIndexed { idx, salesVal ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f)
                                        .clickable {
                                            activeTimelineNode = if (activeTimelineNode == idx) -1 else idx
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (activeTimelineNode == idx) {
                                        Box(
                                            modifier = Modifier
                                                .offset(y = (-45).dp)
                                                .shadow(4.dp, RoundedCornerShape(8.dp))
                                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Rs. ${String.format("%,.0f", salesVal)}",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // X axis Labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        timelineLabels.forEach { label ->
                            Text(
                                text = label,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto updated live",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).background(BrandPrimary, CircleShape))
                    Text(
                        text = "Trend curve",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    labelText: String,
    valueText: String,
    iconColor: Color,
    iconBg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    borderColor: Color = Color(0xFFF1F5F9),
    textColor: Color = Color(0xFF0F172A)
) {
    Card(
        modifier = modifier
            .heightIn(min = 110.dp)
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = labelText,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = labelText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = valueText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
