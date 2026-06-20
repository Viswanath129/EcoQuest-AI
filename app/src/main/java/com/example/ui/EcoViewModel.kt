package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.Crashlytics
import com.example.data.*
import com.example.network.*
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class EcoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EcoRepository

    init {
        val database = EcoDatabase.getDatabase(application)
        repository = EcoRepository(database)
    }

    // UI flows from repository
    val quests: StateFlow<List<Quest>> = repository.allQuests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStats: StateFlow<UserStats?> = repository.userStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val chatHistory: StateFlow<List<ChatMessage>> = repository.chatHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGeneratingQuests = MutableStateFlow(false)
    val isGeneratingQuests: StateFlow<Boolean> = _isGeneratingQuests.asStateFlow()

    private val _loadingMessage = MutableStateFlow("Preparing green engine...")
    val loadingMessage: StateFlow<String> = _loadingMessage.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _showCelebration = MutableStateFlow(false)
    val showCelebration: StateFlow<Boolean> = _showCelebration.asStateFlow()

    private val _recentCelebrationQuest = MutableStateFlow<Quest?>(null)
    val recentCelebrationQuest: StateFlow<Quest?> = _recentCelebrationQuest.asStateFlow()

    private val _apiKeyWarning = MutableStateFlow(!GeminiApiClient.isApiKeyValid())
    val apiKeyWarning: StateFlow<Boolean> = _apiKeyWarning.asStateFlow()

    fun dismissCelebration() {
        _showCelebration.value = false
        _recentCelebrationQuest.value = null
    }

    // Helper to get matching level text
    fun getLevelName(level: Int): String {
        return when (level) {
            1 -> "Eco Rookie"
            2 -> "Green Explorer"
            3 -> "Carbon Warrior"
            4 -> "Planet Guardian"
            else -> "Earth Champion"
        }
    }

    // Seeding default data on startup if database is empty
    fun seedInitialDataIfNeeded() {
        viewModelScope.launch {
            val stats = repository.getUserStatsSync()
            if (stats == null) {
                // First launch seed
                repository.saveUserStats(
                    UserStats(
                        id = 1,
                        level = 1,
                        xp = 0,
                        xpToNextLevel = 100,
                        co2SavedCumulative = 0.0,
                        missionsCompleted = 0,
                        streakDays = 1,
                        onboarded = false,
                        carbonScore = 100,
                        annualEmissions = 4.5,
                        impactCategory = "Moderate"
                    )
                )
                // Seed some default quests
                val initialQuests = listOf(
                    Quest(
                        title = "🌱 Carry Reusable Bottle",
                        description = "Do not buy plastic bottled water today; carry your steel or glass container instead.",
                        xp = 25,
                        co2Saved = 0.4,
                        difficulty = "Easy",
                        estimatedTime = "2 mins",
                        reason = "Plastic production is heavily carbon-reliant. Reusable bottle habits cut container shipping and plastic molding lifecycle emissions.",
                        completed = false
                    ),
                    Quest(
                        title = "🔌 Eliminate Standby Power",
                        description = "Switch off power strips or unplug your TV, charger brick, kitchen appliances when not in use today.",
                        xp = 35,
                        co2Saved = 0.8,
                        difficulty = "Easy",
                        estimatedTime = "5 mins",
                        reason = "Unplugging phantom energy units stops standby currents which collectively draw up to 10% of household electrical grid usage.",
                        completed = false
                    ),
                    Quest(
                        title = "🛴 Switch Commute to Walking",
                        description = "Walk or ride a bike for any errand that is less than 2 km from your home.",
                        xp = 50,
                        co2Saved = 1.2,
                        difficulty = "Medium",
                        estimatedTime = "20 mins",
                        reason = "Short car trips emit cold exhaust which produces double the pollutants compared to warmed engines per kilometer.",
                        completed = false
                    )
                )
                repository.saveQuests(initialQuests)

                // Seed coach greetings
                repository.addChatMessage(
                    ChatMessage(
                        role = "model",
                        content = "Hey there! 🌍 I'm your EcoQuest sustainability advisor. Ready to level up your planet-saving journey? Tell me about your sustainability goals, or configure your profile in the 'Profile' tab to calculate your actual carbon score and generate personalized daily quests!",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Complete a quest
    fun completeQuest(quest: Quest) {
        viewModelScope.launch {
            if (quest.completed) return@launch

            val levelStrategy = { currentLevel: Int, currentXp: Int ->
                var level = currentLevel
                var xp = currentXp
                var xpNeeded = getXpNeededForLevel(level)

                // Level up logic
                while (xp >= xpNeeded && level < 5) {
                    xp -= xpNeeded
                    level++
                    xpNeeded = getXpNeededForLevel(level)
                }

                if (xp >= xpNeeded && level >= 5) {
                    // Cap at level 5
                    xp = xpNeeded
                }
                Triple(level, xp, getXpNeededForLevel(level))
            }

            val success = repository.completeQuestAtomic(
                questId = quest.id,
                xpGain = quest.xp,
                co2SavedGain = quest.co2Saved,
                levelUpLogic = levelStrategy
            )

            if (success) {
                // Trigger visual overlay celebration callback
                _recentCelebrationQuest.value = quest
                _showCelebration.value = true
            }
        }
    }

    private fun getXpNeededForLevel(level: Int): Int {
        return when (level) {
            1 -> 100
            2 -> 150
            3 -> 250
            4 -> 400
            else -> 600
        }
    }

    // Complete Onboarding & Save habits
    fun completeOnboarding(
        name: String,
        country: String,
        transport: String,
        food: String,
        electricity: String,
        shopping: String
    ) {
        viewModelScope.launch {
            // Real sustainability carbon logic
            var baseEmissions = 2.0 // standard baseline offset

            val tAdd = when (transport) {
                "Gasoline Car Solo" -> 3.8
                "Electric / Hybrid" -> 1.4
                "Public Transit" -> 0.7
                else -> 0.1 // Walking/Biking
            }

            val fAdd = when (food) {
                "Heavy Meat Consumer" -> 2.6
                "Moderate Meat / Veg" -> 1.4
                "Vegetarian" -> 0.6
                else -> 0.2 // Vegan
            }

            val eAdd = when (electricity) {
                "High electrical use" -> 3.2
                "Moderate home use" -> 1.6
                else -> 0.4 // Eco conscious
            }

            val sAdd = when (shopping) {
                "Frequent shopper" -> 1.9
                "Occasional buyer" -> 0.9
                else -> 0.2 // Vintage/minimalist
            }

            val totalEmissions = baseEmissions + tAdd + fAdd + eAdd + sAdd
            val score = maxOf(15, minOf(99, 100 - (totalEmissions * 7).toInt()))

            val category = when {
                totalEmissions < 4.0 -> "Low"
                totalEmissions <= 7.5 -> "Moderate"
                else -> "High"
            }

            val currentStats = repository.getUserStatsSync() ?: UserStats()
            val updated = currentStats.copy(
                onboarded = true,
                carbonScore = score,
                annualEmissions = totalEmissions,
                impactCategory = category,
                transportHabit = transport,
                foodHabit = food,
                electricityHabit = electricity,
                shoppingHabit = shopping
            )

            repository.saveUserStats(updated)

            // Dynamic quest refresh based on new profile!
            generateMissions(silent = true)
        }
    }

    // Request new daily quests from Gemini AI or simulated offline generator
    fun generateMissions(silent: Boolean = false) {
        if (_isGeneratingQuests.value) return
        viewModelScope.launch {
            if (!silent) _isGeneratingQuests.value = true
            
            // Progressive progress messages
            val progressJob = launch {
                val messages = listOf(
                    "Analyzing lifestyle...",
                    "Calculating carbon impact...",
                    "Building recommendations...",
                    "Generating personalized missions..."
                )
                for (msg in messages) {
                    _loadingMessage.value = msg
                    kotlinx.coroutines.delay(1200)
                }
            }

            try {
                val stats = repository.getUserStatsSync() ?: UserStats()
                
                if (GeminiApiClient.isApiKeyValid()) {
                    // Assemble prompt
                    val prompt = """
                        You are the EcoQuest AI Sustainability Coach.
                        Generate exactly 3 creative, highly action-oriented daily quests for a user with the following green-habits profile:
                        - Transportation habits: ${stats.transportHabit.ifEmpty { "Moderate transit" }}
                        - Food style: ${stats.foodHabit.ifEmpty { "Moderate meat" }}
                        - Home Electricity usage: ${stats.electricityHabit.ifEmpty { "Moderate use" }}
                        - Shopping habits: ${stats.shoppingHabit.ifEmpty { "Occasional boutique shopper" }}
                        - Current User Level: Level ${stats.level} (${getLevelName(stats.level)})

                        The response MUST be valid JSON (do not include markdown block ticks like ```json) conforming exactly to this structure:
                        {
                          "quests": [
                            {
                              "title": "A short engaging headline under 32 chars beginning with single relevant emoji",
                              "description": "Specific, actionable climate effort details to execute today.",
                              "xp": 35,
                              "co2Saved": 0.85,
                              "difficulty": "Easy",
                              "estimatedTime": "5 mins",
                              "reason": "Clear explanation of how this reduces greenhouse emissions to help motivate the user."
                            }
                          ]
                        }
                    """.trimIndent()

                    val sysInstr = "You are the primary server-side mission generator for EcoQuest. Respond only in raw, clean JSON format. Never include introductory text."

                    val request = GeminiRequest(
                        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                        generationConfig = GenerationConfig(temperature = 0.8, responseMimeType = "application/json"),
                        systemInstruction = Content(parts = listOf(Part(text = sysInstr)))
                    )

                    val response = withContext(Dispatchers.IO) {
                        withTimeout(15000L) { // 15-second timeout for Gemini AI requests
                            GeminiApiClient.service.generateContent(GeminiApiClient.getApiKey(), request)
                        }
                    }

                    val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                    Log.d("EcoQuest", "Gemini Quests raw response: $jsonText")

                    // Clean and safely parse JSON block
                    fun safeExtractJson(text: String): String {
                        val start = text.indexOf("{")
                        val end = text.lastIndexOf("}")
                        if (start != -1 && end != -1 && end > start) {
                            return text.substring(start, end + 1)
                        }
                        return text.trim()
                    }

                    val cleanJson = safeExtractJson(jsonText)
                    
                    val moshi = GeminiApiClient.moshiInstance
                    val listAdapter = moshi.adapter(GeneratedQuestsList::class.java)
                    val parsed = listAdapter.fromJson(cleanJson)

                    if (parsed != null && parsed.quests.isNotEmpty()) {
                        repository.clearUncompletedQuests()
                        val mappedQuests = parsed.quests.map { q ->
                            Quest(
                                title = q.title,
                                description = q.description,
                                xp = q.xp,
                                co2Saved = q.co2Saved,
                                difficulty = q.difficulty,
                                estimatedTime = q.estimatedTime,
                                reason = q.reason,
                                completed = false
                            )
                        }
                        repository.saveQuests(mappedQuests)
                    } else {
                        throw Exception("Failed to parse valid quests")
                    }
                } else {
                    // Fallback simulation mode
                    delaySimulatedWork()
                    generateFallbackQuests(stats)
                }
            } catch (e: Exception) {
                Crashlytics.recordException(e)
                Log.e("EcoQuest", "Gemini quest generation error: ", e)
                // Graceful fallback to rich static templates
                val stats = repository.getUserStatsSync() ?: UserStats()
                generateFallbackQuests(stats)
            } finally {
                progressJob.cancel()
                if (!silent) _isGeneratingQuests.value = false
            }
        }
    }

    private suspend fun delaySimulatedWork() = withContext(Dispatchers.IO) {
        kotlinx.coroutines.delay(1200) // Beautiful feeling of processing
    }

    private suspend fun generateFallbackQuests(stats: UserStats) {
        val simulatedQuests = mutableListOf<Quest>()

        // Generate tailored options offline
        if (stats.transportHabit.contains("Gasoline", ignoreCase = true)) {
            simulatedQuests.add(
                Quest(
                    title = "🚗 Commute Eco-Sharing",
                    description = "Carpool to work/college, or choose public rail/bus for your primary trip today.",
                    xp = 40,
                    co2Saved = 2.8,
                    difficulty = "Medium",
                    estimatedTime = "30 mins",
                    reason = "Single-occupancy combustion vehicles drive the highest percentage of individual carbon output. Bus trips slice transit emissions by 75% per seat.",
                    completed = false
                )
            )
        } else {
            simulatedQuests.add(
                Quest(
                    title = "🚴 Chain Commuting Trips",
                    description = "Batch all outdoor errands into a single cycle path or walking line to save footprint fatigue.",
                    xp = 30,
                    co2Saved = 0.6,
                    difficulty = "Easy",
                    estimatedTime = "15 mins",
                    reason = "Batching secondary travels prevents cold engine start cycles where fuel burn and toxic components peak.",
                    completed = false
                )
            )
        }

        if (stats.foodHabit.contains("Meat", ignoreCase = true)) {
            simulatedQuests.add(
                Quest(
                    title = "🍲 Veg-Powered Dining",
                    description = "Enjoy a fully meatless day today—enjoy plant proteins, legumes and roasted roots instead.",
                    xp = 45,
                    co2Saved = 1.9,
                    difficulty = "Medium",
                    estimatedTime = "25 mins",
                    reason = "Livestock agriculture outputs significant methane gases. Beef has a carbon output 10x higher than plant cereals per calorie.",
                    completed = false
                )
            )
        } else {
            simulatedQuests.add(
                Quest(
                    title = "🥕 Buy Zero-Plastic Produce",
                    description = "Avoid plastic-packaged foods. Pick loose veggies and deposit them into your tote.",
                    xp = 30,
                    co2Saved = 0.4,
                    difficulty = "Easy",
                    estimatedTime = "10 mins",
                    reason = "Unwrapped local crops save industrial thermo-forming emissions and reduce landfill degradation outputs.",
                    completed = false
                )
            )
        }

        if (stats.electricityHabit.contains("High", ignoreCase = true)) {
            simulatedQuests.add(
                Quest(
                    title = "🌡️ Eco-Calibrate Thermostat",
                    description = "Adjust thermostat warmer by 2°C (cooling mode) or cooler by 2°C (heating mode) for 4 consecutive hours.",
                    xp = 35,
                    co2Saved = 1.5,
                    difficulty = "Medium",
                    estimatedTime = "4 hours",
                    reason = "HVAC load draws the lion's share of household electrical inputs. Minor adjustments yield 15% reduction in grid demands.",
                    completed = false
                )
            )
        } else {
            simulatedQuests.add(
                Quest(
                    title = "🌞 Power-Strip Shut Down",
                    description = "Turn off power strips of console systems and entertainment setups before sleeping.",
                    xp = 25,
                    co2Saved = 0.5,
                    difficulty = "Easy",
                    estimatedTime = "1 min",
                    reason = "Connected screens and gaming arrays draw background current constantly, draining grids with phantom loads.",
                    completed = false
                )
            )
        }

        // Wipe old quests and insert new ones
        repository.clearUncompletedQuests()
        repository.saveQuests(simulatedQuests)
    }

    // Send chat messages to sustainability coach
    fun sendCoachMessage(text: String) {
        if (text.trim().isEmpty()) return

        viewModelScope.launch {
            val userMsg = ChatMessage(role = "user", content = text, timestamp = System.currentTimeMillis())
            repository.addChatMessage(userMsg)

            _isSendingMessage.value = true

            try {
                if (GeminiApiClient.isApiKeyValid()) {
                    // Fetch full local history to pass context
                    val currentHistory = repository.chatHistory.first()

                    // Assemble chat entries
                    val contentList = currentHistory.map { msg ->
                        Content(
                            role = if (msg.role == "user") "user" else "model",
                            parts = listOf(Part(text = msg.content))
                        )
                    }

                    val sysInstr = """
                        You are GreenLeaf, the AI Sustainability Coach of EcoQuest AI. You are a warm, witty climate educator and practical hacker.
                        Help users lower their carbon footprint, explain the science of climate impact, offer student-budget advice, and cheer on their leveling progress.
                        Respond in 2 to 3 compact paragraphs. Use clear emojis, list items occasionally, and maintain an inspiring, friendly, and empowering tone.
                    """.trimIndent()

                    val request = GeminiRequest(
                        contents = contentList,
                        systemInstruction = Content(parts = listOf(Part(text = sysInstr))),
                        generationConfig = GenerationConfig(temperature = 0.7)
                    )

                    val response = withContext(Dispatchers.IO) {
                        GeminiApiClient.service.generateContent(GeminiApiClient.getApiKey(), request)
                    }

                    val modelText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "I'm processing that! Let's save some carbon in the meantime."

                    repository.addChatMessage(
                        ChatMessage(role = "model", content = modelText, timestamp = System.currentTimeMillis())
                    )
                } else {
                    // Simulated rich responses based on standard questions
                    delaySimulatedWork()
                    val simulatedReply = simulateCoachReply(text)
                    repository.addChatMessage(
                        ChatMessage(role = "model", content = simulatedReply, timestamp = System.currentTimeMillis())
                    )
                }
            } catch (e: Exception) {
                Crashlytics.recordException(e)
                Log.e("EcoQuest", "Chat coach response error: ", e)
                repository.addChatMessage(
                    ChatMessage(
                        role = "model",
                        content = "Oops! My green servers encountered a small breeze. Let me assure you: every small individual action matters immensely! Try check your connection or insert your API Key.",
                        timestamp = System.currentTimeMillis()
                    )
                )
            } finally {
                _isSendingMessage.value = false
            }
        }
    }

    private fun simulateCoachReply(userQuery: String): String {
        val query = userQuery.lowercase()
        return when {
            query.contains("student") || query.contains("hostel") || query.contains("dorm") || query.contains("campus") -> {
                "Dorm living is a fantastic laboratory for sustainability! 🎓\n\n" +
                "1. **Brighten down:** Dim your phone and laptop screens by 20% to save charging battery power and protect your eyes.\n" +
                "2. **Laundry batching:** Wash laundry with cold cycles in bulky batches, saving heating energy cycles.\n" +
                "3. **Eco-unplug:** Unplug desk hubs and monitors before leaving for lectures. Let's make that campus footprint beautifully microscopic!"
            }
            query.contains("food") || query.contains("eat") || query.contains("meat") || query.contains("diet") -> {
                "Dining is where personal planetary healing starts! 🍲\n\n" +
                "Replacing even one beef meal per week with lentils or grain bowls cuts carbon loads by roughly 250 kg annually. " +
                "Additionally, try preserving vegetable scraps to make homemade stock. " +
                "Food waste rotting in landfills is a primary contributor of global methane output!"
            }
            query.contains("electricity") || query.contains("power") || query.contains("light") || query.contains("energy") -> {
                "Grid power is often carbon-intensive! 🔌\n\n" +
                "To streamline usage, set your AC target to 25°C. Lowering your water heater thermostat slightly also creates massive cumulative grid relief. " +
                "Unplug adapters that feel warm to the touch when idle—they're consuming background 'vampire' volts!"
            }
            query.contains("car") || query.contains("transport") || query.contains("commute") || query.contains("travel") -> {
                "Micro-mobility is clean and energetic! 🚲\n\n" +
                "Choosing public trains or bus transit lowers your personal per-passenger travel emissions by 60-80% compared to solo highway driving. " +
                "For intermediate errands, grab a bike or walk—it burns body calories instead of gasoline!"
            }
            else -> {
                "That's a stellar inquiry! 🌱 Personal accountability is cumulative. " +
                "When you carry a dynamic bag, skip single-use cups, or switch to ambient lighting levels, you inspire friends and roommates to do the same. " +
                "Tell me: what habit are we focusing on streamlining next?"
            }
        }
    }

    // Reset everything
    fun resetAllData() {
        viewModelScope.launch {
            repository.clearAllQuests()
            repository.clearChatHistory()
            repository.saveUserStats(
                UserStats(
                    id = 1,
                    level = 1,
                    xp = 0,
                    xpToNextLevel = 100,
                    co2SavedCumulative = 0.0,
                    missionsCompleted = 0,
                    streakDays = 1,
                    onboarded = false,
                    carbonScore = 100,
                    annualEmissions = 4.5,
                    impactCategory = "Moderate"
                )
            )
            seedInitialDataIfNeeded()
        }
    }

    fun reassessFootprint() {
        viewModelScope.launch {
            val stats = repository.getUserStatsSync() ?: return@launch
            repository.saveUserStats(stats.copy(onboarded = false))
        }
    }
}
