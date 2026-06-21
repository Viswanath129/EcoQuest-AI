package com.example.data

import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction

class EcoRepository(private val db: EcoDatabase) {

    val questDao = db.questDao()
    val userStatsDao = db.userStatsDao()
    val chatDao = db.chatDao()

    val allQuests: Flow<List<Quest>> = questDao.getAllQuests()
    val userStats: Flow<UserStats?> = userStatsDao.getUserStats(1)
    val chatHistory: Flow<List<ChatMessage>> = chatDao.getChatHistory()

    suspend fun completeQuestAtomic(questId: Int, xpGain: Int, co2SavedGain: Double, levelUpLogic: (Int, Int) -> Triple<Int, Int, Int>): Boolean {
        return db.withTransaction {
            val q = questDao.getQuestById(questId)
            if (q == null || q.completed) {
                return@withTransaction false
            }
            
            questDao.updateQuest(q.copy(completed = true, timestamp = System.currentTimeMillis()))
            
            val currentStats = userStatsDao.getUserStatsSync(1) ?: UserStats()
            val newXp = currentStats.xp + xpGain
            
            val (newLevel, finalXp, nextLevelXp) = levelUpLogic(currentStats.level, newXp)
            
            val newStats = currentStats.copy(
                level = newLevel,
                xp = finalXp,
                xpToNextLevel = nextLevelXp,
                co2SavedCumulative = currentStats.co2SavedCumulative + co2SavedGain,
                missionsCompleted = currentStats.missionsCompleted + 1
            )
            userStatsDao.insertOrUpdate(newStats)
            return@withTransaction true
        }
    }

    suspend fun getUserStatsSync(): UserStats? = userStatsDao.getUserStatsSync(1)

    suspend fun saveUserStats(stats: UserStats) {
        userStatsDao.insertOrUpdate(stats)
    }

    suspend fun saveQuest(quest: Quest) {
        questDao.insertQuest(quest)
    }

    suspend fun saveQuests(quests: List<Quest>) {
        questDao.insertQuests(quests)
    }

    suspend fun updateQuest(quest: Quest) {
        questDao.updateQuest(quest)
    }

    suspend fun deleteQuestById(id: Int) {
        questDao.deleteQuestById(id)
    }

    suspend fun clearAllQuests() {
        questDao.clearAllQuests()
    }

    suspend fun clearUncompletedQuests() {
        questDao.clearUncompletedQuests()
    }

    suspend fun addChatMessage(message: ChatMessage) {
        chatDao.insertMessage(message)
    }

    suspend fun clearChatHistory() {
        chatDao.clearHistory()
    }
}
