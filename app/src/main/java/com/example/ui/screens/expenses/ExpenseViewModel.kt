package com.example.ui.screens.expenses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.SyncPosApplication
import com.example.core.database.entity.ExpenseEntity
import com.example.core.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ExpenseViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    val uiState: StateFlow<List<ExpenseEntity>> = repository.getAllExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addExpense(title: String, amount: Double, category: String, date: Long? = null) {
        viewModelScope.launch {
            val entity = ExpenseEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                amount = amount,
                category = category,
                createdAt = date ?: System.currentTimeMillis()
            )
            repository.insertExpense(entity)
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            repository.deleteExpense(id)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = (application as SyncPosApplication).repository
            return ExpenseViewModel(application, repository) as T
        }
    }
}
