package com.fahim.geminiApiComposeStarter.core.context

import com.fahim.geminiApiComposeStarter.core.ai.StudyPromptBuilder
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.model.StudyMode

/**
 * Encapsulates the bounded, curated context passed to the Gemini Gateway.
 */
data class ConversationContext(
    val recentMessages: List<ChatMessage>,
    val systemInstruction: String,
    val effectivePrompt: String,
    val mode: StudyMode,
)

/**
 * Prepares bounded conversational context for the Gemini model.
 * Prevents token overflow while preserving conversational continuity and follow-ups.
 */
class ContextEngine(
    private val maxHistoryWindow: Int = 10,
) {

    fun buildContext(
        history: List<ChatMessage>,
        newPrompt: String,
        mode: StudyMode,
        relevantSavedNotes: List<ChatMessage> = emptyList(),
    ): ConversationContext {
        // Rolling window of the most recent messages (excluding the new prompt which is sent separately)
        val boundedHistory = if (history.size > maxHistoryWindow) {
            history.takeLast(maxHistoryWindow)
        } else {
            history
        }

        val baseInstruction = StudyPromptBuilder.getSystemInstruction(mode)
        val fullInstruction = if (relevantSavedNotes.isNotEmpty()) {
            val notesSummary = relevantSavedNotes.take(3).joinToString("\n---\n") { it.content.take(150) }
            "$baseInstruction\n\nStudent's saved reference notes:\n$notesSummary"
        } else {
            baseInstruction
        }

        val effectivePrompt = StudyPromptBuilder.wrapPrompt(newPrompt.trim(), mode)

        return ConversationContext(
            recentMessages = boundedHistory,
            systemInstruction = fullInstruction,
            effectivePrompt = effectivePrompt,
            mode = mode,
        )
    }
}
