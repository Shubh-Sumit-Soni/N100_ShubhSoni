package com.fahim.geminiApiComposeStarter.model

import java.util.UUID

enum class MessageRole { USER, GEMINI }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)
