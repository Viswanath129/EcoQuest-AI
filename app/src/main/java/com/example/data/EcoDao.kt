package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests ORDER BY timestamp DESC")
    fun getAllQuests(): Flow<List<Quest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<Quest>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: Quest)

    @Update
    suspend fun updateQuest(quest: Quest)

    @Query("DELETE FROM quests WHERE id = :id")
    suspend fun deleteQuestById(id: Int)

    @Query("DELETE FROM quests")
    suspend fun clearAllQuests()
}

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = :id LIMIT 1")
    fun getUserStats(id: Int = 1): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = :id LIMIT 1")
    suspend fun getUserStatsSync(id: Int = 1): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStats)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatHistory(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}
