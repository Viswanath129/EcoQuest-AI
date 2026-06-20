package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Double? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?,
    val finishReason: String?
)

// Data class to parse generated quests
@JsonClass(generateAdapter = true)
data class GeneratedQuestsList(
    val quests: List<GeneratedQuest>
)

@JsonClass(generateAdapter = true)
data class GeneratedQuest(
    val title: String,
    val description: String,
    val xp: Int,
    val co2Saved: Double, // in kg
    val difficulty: String, // "Easy", "Medium", "Hard"
    val estimatedTime: String, // e.g. "5 mins"
    val reason: String
)
