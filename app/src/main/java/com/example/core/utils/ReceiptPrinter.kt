package com.example.core.utils

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.ui.screens.billing.BillingUiState
import com.example.ui.screens.billing.DiscountType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptPrinter {

    fun printReceipt(context: Context, state: BillingUiState) {
        try {
            val htmlContent = generateReceiptHtml(state)
            
            // Create a WebView on the Main Thread (where this function should be called)
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager != null) {
                        val jobName = "SyncPOS_Receipt_${state.displayId}"
                        val printAdapter = webView.createPrintDocumentAdapter(jobName)
                        printManager.print(
                            jobName,
                            printAdapter,
                            PrintAttributes.Builder().build()
                        )
                    } else {
                        Log.e("ReceiptPrinter", "PrintManager service is not available on this device.")
                    }
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            Log.e("ReceiptPrinter", "Error generating or starting print job", e)
        }
    }

    private fun generateReceiptHtml(state: BillingUiState): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date())

        val discountAmount = when (state.discountType) {
            DiscountType.PERCENT -> state.subtotal * (state.discountValue / 100.0)
            DiscountType.FIXED -> state.discountValue
            else -> 0.0
        }
        val grandTotal = (state.subtotal + state.calculatedTax - discountAmount).coerceAtLeast(0.0)

        val itemsHtml = StringBuilder()
        for (item in state.cart) {
            val itemTotal = item.singleUnitPrice * item.quantity
            val qtyText = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            
            // Item row
            itemsHtml.append("""
                <tr>
                    <td class="qty">$qtyText</td>
                    <td class="desc">
                        <div><strong>${item.product.name}</strong></div>
            """)

            // Show selected variation if any
            if (item.selectedVariation != null) {
                itemsHtml.append("""<div class="meta">&bull; Variation: ${item.selectedVariation.name}</div>""")
            }

            // Show selected addons if any
            if (item.selectedAddons.isNotEmpty()) {
                val addonsStr = item.selectedAddons.joinToString { "${it.name} (+Rs.${String.format("%.2f", it.price)})" }
                itemsHtml.append("""<div class="meta">&bull; Add-ons: $addonsStr</div>""")
            }

            // Show assigned staff if any
            if (!item.assignedStaffName.isNullOrBlank()) {
                itemsHtml.append("""<div class="meta">&bull; Staff: ${item.assignedStaffName}</div>""")
            }

            itemsHtml.append("""
                    </td>
                    <td class="price text-right">Rs.${String.format("%.2f", item.singleUnitPrice)}</td>
                    <td class="total text-right">Rs.${String.format("%.2f", itemTotal)}</td>
                </tr>
            """)
        }

        val tableInfoHtml = if (!state.tableNumber.isNullOrBlank()) {
            """
            <div class="info-row">
                <span><strong>Dining Table:</strong> Same-day Dining - Table ${state.tableNumber}</span>
            </div>
            """
        } else ""

        val customerInfoHtml = if (!state.customerName.isNullOrBlank() || !state.customerPhone.isNullOrBlank()) {
            """
            <div class="customer-box">
                <strong>Bill To:</strong><br/>
                ${if (!state.customerName.isNullOrBlank()) "Name: ${state.customerName}<br/>" else ""}
                ${if (!state.customerPhone.isNullOrBlank()) "Phone: ${state.customerPhone}<br/>" else ""}
                ${if (!state.customerAddress.isNullOrBlank()) "Address: ${state.customerAddress}" else ""}
            </div>
            """.trimIndent()
        } else ""

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                @page {
                    size: 80mm;
                    margin: 0;
                }
                body {
                    font-family: 'Courier New', Courier, monospace;
                    width: 72mm;
                    margin: 0;
                    padding: 4mm 4mm;
                    color: #000000;
                    background-color: #ffffff;
                    font-size: 11px;
                    line-height: 1.4;
                    -webkit-print-color-adjust: exact;
                }
                .text-center { text-align: center; }
                .text-right { text-align: right; }
                .bold { font-weight: bold; }
                
                .header {
                    margin-bottom: 12px;
                }
                .brand {
                    font-size: 16px;
                    font-weight: 900;
                    letter-spacing: 1px;
                    margin-bottom: 2px;
                }
                .subtitle {
                    font-size: 9px;
                    color: #555555;
                    margin-bottom: 4px;
                }
                
                .divider {
                    border-top: 1px dashed #000000;
                    margin: 8px 0;
                }
                
                .info-row {
                    display: flex;
                    justify-content: space-between;
                    font-size: 10px;
                    margin-bottom: 3px;
                }
                
                .customer-box {
                    background-color: #f7f7f7;
                    border: 1px solid #e3e3e3;
                    border-radius: 4px;
                    padding: 6px;
                    margin: 8px 0;
                    font-size: 10px;
                }
                
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin: 10px 0;
                }
                th {
                    font-size: 10px;
                    font-weight: bold;
                    border-bottom: 1px solid #000000;
                    padding-bottom: 4px;
                    text-align: left;
                }
                td {
                    padding: 4px 0;
                    font-size: 10px;
                    vertical-align: top;
                }
                .qty { width: 10%; }
                .desc { width: 50%; }
                .price { width: 20%; }
                .total { width: 20%; }
                .meta {
                    font-size: 9px;
                    color: #444444;
                    padding-left: 4px;
                    margin-top: 2px;
                }
                
                .calculations {
                    width: 100%;
                    margin-top: 8px;
                }
                .calc-row {
                    display: flex;
                    justify-content: space-between;
                    padding: 2px 0;
                }
                .grand-total-row {
                    display: flex;
                    justify-content: space-between;
                    padding: 6px 0;
                    margin-top: 4px;
                    font-size: 13px;
                    font-weight: bold;
                    border-top: 1px double #000000;
                    border-bottom: 1px double #000000;
                }
                
                .footer {
                    margin-top: 20px;
                    font-size: 10px;
                }
            </style>
        </head>
        <body>
            <div class="header text-center">
                <div class="brand">SyncPOS INVOICE</div>
                <div class="subtitle">Complete Retail & Restaurant Point of Sale</div>
                <div>Terminal: #001 (Active)</div>
            </div>
            
            <div class="divider"></div>
            
            <div class="info-row">
                <span><strong>Date:</strong> $dateStr</span>
            </div>
            <div class="info-row">
                <span><strong>Invoice ID:</strong> Order #&nbsp;${state.displayId}</span>
            </div>
            $tableInfoHtml
            $customerInfoHtml
            
            <div class="divider"></div>
            
            <table>
                <thead>
                    <tr>
                        <th class="qty">Qty</th>
                        <th class="desc">Item</th>
                        <th class="price text-right">Rate</th>
                        <th class="total text-right">Amount</th>
                    </tr>
                </thead>
                <tbody>
                    $itemsHtml
                </tbody>
            </table>
            
            <div class="divider"></div>
            
            <div class="calculations">
                <div class="calc-row">
                    <span>Subtotal:</span>
                    <span>Rs.${String.format("%.2f", state.subtotal)}</span>
                </div>
                <div class="calc-row">
                    <span>GST / Sales Tax:</span>
                    <span>+Rs.${String.format("%.2f", state.calculatedTax)}</span>
                </div>
                ${if (discountAmount > 0) {
                    """
                    <div class="calc-row" style="color: #000000;">
                        <span>Discount (${if (state.discountType == DiscountType.PERCENT) "${state.discountValue}%" else "Flat Value"}):</span>
                        <span>-Rs.${String.format("%.2f", discountAmount)}</span>
                    </div>
                    """
                } else ""}
                
                <div class="grand-total-row">
                    <span>GRAND TOTAL:</span>
                    <span>Rs.${String.format("%.2f", grandTotal)}</span>
                </div>
            </div>
            
            <div class="divider"></div>
            
            <div class="info-row bold">
                <span>Payment Mode:</span>
                <span>${state.paymentMode.name}</span>
            </div>
            
            <div class="footer text-center">
                <strong>Thank you for your business!</strong><br/>
                <span>Please visit again</span>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
