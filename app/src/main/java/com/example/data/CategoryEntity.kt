package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetAccount: String = "BOTH" // "DISCREZIONALE", "FONDO_EVENTI", "BOTH"
)
