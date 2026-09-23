package com.fahim.geminiApiComposeStarter.ui.chat

import android.graphics.Bitmap
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.model.Conversation
import com.fahim.geminiApiComposeStarter.model.QuizAttempt
import com.fahim.geminiApiComposeStarter.model.StudyMode

/**
 * Immutable UI state for the Gemini AI Study Workspace.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val hasApiKey: Boolean = true,
    val activeMode: StudyMode = StudyMode.CHAT,
    val conversations: List<Conversation> = emptyList(),
    val currentConversationId: String = "default",
    val currentConversationTitle: String = "Study Workspace",
    val savedNotes: List<ChatMessage> = emptyList(),
    val selectedImageBitmap: Bitmap? = null,
    val selectedImageUri: String? = null,
    val speakingMessageId: String? = null,
    val quizAttempts: List<QuizAttempt> = emptyList(),
    val showModeDialog: Boolean = false,
    val showSavedNotesDialog: Boolean = false,
    val showInsightsDialog: Boolean = false,
)

enum class PromptError { EMPTY }
