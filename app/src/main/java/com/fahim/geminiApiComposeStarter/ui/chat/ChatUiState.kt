package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.model.ChatMessage

/** Immutable UI state for the conversation chat flow. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val hasApiKey: Boolean = true,
)

enum class PromptError { EMPTY }
