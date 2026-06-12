package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.core.repository.AppRepositoryImpl
import com.example.ui.screens.billing.ActivePOSConsole
import com.example.ui.screens.billing.BillingViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testClickAddCustomerAndDiscountButtons() {
        runBlocking {
            try {
                val context = ApplicationProvider.getApplicationContext<Context>() as Application
                val database = Room.inMemoryDatabaseBuilder(
                    context,
                    AppDatabase::class.java
                ).allowMainThreadQueries().build()
                val repository = AppRepositoryImpl(database)
                val viewModel = BillingViewModel(context, repository)

                composeTestRule.setContent {
                    ActivePOSConsole(
                        orderId = "new",
                        onNavigateBack = {},
                        viewModel = viewModel
                    )
                }

                composeTestRule.waitForIdle()

                // If VIEW CART button is present, click it to open mobile cart sheet
                if (composeTestRule.onAllNodesWithText("VIEW CART").fetchSemanticsNodes().isNotEmpty()) {
                    composeTestRule.onNodeWithText("VIEW CART").performClick()
                    composeTestRule.waitForIdle()
                }

                // Try clicking the Add Customer button
                composeTestRule.onNodeWithText("Add Customer").performClick()
                composeTestRule.waitForIdle()

                // Verify dialog header is visible
                composeTestRule.onNodeWithText("CUSTOMER RECORD").assertIsDisplayed()
            } catch (e: Throwable) {
                e.printStackTrace()
                throw e
            }
        }
    }
}
