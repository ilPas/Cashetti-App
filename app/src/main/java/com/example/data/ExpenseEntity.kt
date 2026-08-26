package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountType: String, // "SERBATOIO_PERSONALE", "SERBATOIO_GINEVRA", "DISCREZIONALE_VARIABILE", "FONDO_EVENTI_DEPOSIT", "FONDO_EVENTI_WITHDRAWAL", "ESSENZIALE_REALE"
    val amount: Double,
    val category: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val merchant: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val eventTargetDateMillis: Long? = null,
    val eventId: Long? = null,
    val excludeFromStats: Boolean = false,
    val isNecessary: Boolean = true
)
