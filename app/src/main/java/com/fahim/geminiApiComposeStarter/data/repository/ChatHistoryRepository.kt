package com.fahim.geminiApiComposeStarter.data.repository

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.model.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatHistoryRepository(private val dao: ChatMessageDao) {

    fun getAllMessages(): Flow<List<ChatMessage>> = dao.getAllMessages().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun insertMessage(message: ChatMessage) {
        dao.insertMessage(message.toEntity())
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}

private fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    role = MessageRole.valueOf(role),
    content = content,
    timestamp = timestamp,
)

private fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    role = role.name,
    content = content,
    timestamp = timestamp,
)
