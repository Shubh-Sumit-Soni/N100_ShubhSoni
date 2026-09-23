package com.fahim.geminiApiComposeStarter.data

import android.graphics.Bitmap
import com.fahim.geminiApiComposeStarter.model.ChatMessage

/**
 * Abstraction over the Gemini API calls.
 * Supports basic generation, multi-turn conversational chat, and multimodal vision requests.
 * Default interface methods ensure full backward compatibility with existing unit test fakes.
 */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>

    suspend fun generateChat(
        prompt: String,
        history: List<ChatMessage> = emptyList(),
        systemInstruction: String? = null,
    ): Result<String> = generateText(prompt)

    suspend fun generateMultimodal(
        prompt: String,
        imageBitmap: Bitmap,
        systemInstruction: String? = null,
    ): Result<String> = generateText(prompt)
}
