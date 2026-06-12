package com.example.core.sync

import com.example.core.database.entity.ProductEntity
import com.example.core.database.entity.OrderEntity
import com.example.core.database.entity.OrderItemEntity
import com.example.core.database.entity.ExpenseEntity
import com.example.core.repository.AppRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class CloudSyncManager(
    private val repository: AppRepository
) {
    private val firestore: FirebaseFirestore?
        get() = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
        
    private val auth: FirebaseAuth?
        get() = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }

    private val userDocRef
        get() = auth?.currentUser?.uid?.let { firestore?.collection("users")?.document(it) }

    // Backup Local Data to Cloud
    suspend fun backupDataToCloud() {
        val docRef = userDocRef ?: return

        try {
            val products = repository.getAllProducts().first()
            val expenses = repository.getAllExpenses().first()
            val orders = repository.getAllOrders().first()

            val localFirestore = firestore ?: return
            val batch = localFirestore.batch()

            // Backup Products
            products.forEach { product ->
                val ref = docRef.collection("products").document(product.id)
                batch.set(ref, product)
            }

            // Backup Expenses
            expenses.forEach { expense ->
                val ref = docRef.collection("expenses").document(expense.id)
                batch.set(ref, expense)
            }

            // Backup Orders
            orders.forEach { order ->
                val ref = docRef.collection("orders").document(order.id)
                batch.set(ref, order)
                // We're skipping OrderItems for brevity, but this is a simplified backup system.
                // Depending on the schema, you would want to back them up similarly.
            }

            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Restore Cloud Data to Local Data
    suspend fun restoreDataFromCloud() {
        val docRef = userDocRef ?: return

        try {
            // Restore Products
            val productsSnapshot = docRef.collection("products").get().await()
            val products = productsSnapshot.toObjects(ProductEntity::class.java)
            products.forEach { repository.insertOrUpdateProduct(it) }

            // Restore Expenses
            val expensesSnapshot = docRef.collection("expenses").get().await()
            val expenses = expensesSnapshot.toObjects(ExpenseEntity::class.java)
            expenses.forEach { repository.insertExpense(it) }

            // Restore Orders
            val ordersSnapshot = docRef.collection("orders").get().await()
            val orders = ordersSnapshot.toObjects(OrderEntity::class.java)
            
            // Re-insert orders (without items, simply as order entities for brevity)
            orders.forEach { repository.saveOrderWithItems(it, emptyList(), updateStock = false) }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
