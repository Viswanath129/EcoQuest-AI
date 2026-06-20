package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quests")
data class Quest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val xp: Int,
    val co2Saved: Double, // in kg
    val difficulty: String, // "Easy", "Medium", "Hard"
    val estimatedTime: String, // e.g. "2 mins"
    val reason: String,
    val completed: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val xp: Int = 0,
    val xpToNextLevel: Int = 100,
    val co2SavedCumulative: Double = 0.0,
    val missionsCompleted: Int = 0,
    val streakDays: Int = 1,
    val onboarded: Boolean = false,
    val carbonScore: Int = 100, // 0 to 100
    val annualEmissions: Double = 5.2, // Tons
    val impactCategory: String = "Low", // "Low", "Moderate", "High"
    val transportHabit: String = "",
    val foodHabit: String = "",
    val electricityHabit: String = "",
    val shoppingHabit: String = ""
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user", "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
