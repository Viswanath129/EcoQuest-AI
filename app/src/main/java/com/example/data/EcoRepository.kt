package com.example.data

import kotlinx.coroutines.flow.Flow

class EcoRepository(private val db: EcoDatabase) {

    val questDao = db.questDao()
    val userStatsDao = db.userStatsDao()
    val chatDao = db.chatDao()

    val allQuests: Flow<List<Quest>> = questDao.getAllQuests()
    val userStats: Flow<UserStats?> = userStatsDao.getUserStats(1)
    val chatHistory: Flow<List<ChatMessage>> = chatDao.getChatHistory()

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

    suspend fun addChatMessage(message: ChatMessage) {
        chatDao.insertMessage(message)
    }

    suspend fun clearChatHistory() {
        chatDao.clearHistory()
    }
}
