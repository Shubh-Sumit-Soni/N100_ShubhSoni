package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.repository.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeDao: FakeChatMessageDao
    private lateinit var chatHistoryRepository: ChatHistoryRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        fakeDao = FakeChatMessageDao()
        chatHistoryRepository = ChatHistoryRepository(fakeDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(hasApiKey: Boolean = true): ChatViewModel {
        return ChatViewModel(
            repository = fakeRepository,
            chatHistoryRepository = chatHistoryRepository,
            hasApiKey = hasApiKey,
        )
    }

    @Test
    fun `initial state has empty messages and no loading`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.promptError)
        assertEquals("", state.prompt)
    }

    @Test
    fun `onPromptChange updates prompt text`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello Gemini")

        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
    }

    @Test
    fun `onSend with empty prompt sets EMPTY error`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun `onSend without API key sets missing key error`() = runTest {
        val viewModel = createViewModel(hasApiKey = false)
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onSend adds user message and sets loading`() = runTest {
        fakeRepository.nextResult = Result.success("Response")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        // After the send call but before coroutine completes
        val state = viewModel.uiState.value
        assertTrue(state.messages.any { it.role == MessageRole.USER && it.content == "Hello" })
        assertTrue(state.isLoading)
        assertEquals("", state.prompt) // prompt is cleared
    }

    @Test
    fun `successful Gemini response adds gemini message`() = runTest {
        fakeRepository.nextResult = Result.success("Hello from Gemini!")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.messages.size)
        assertEquals(MessageRole.USER, state.messages[0].role)
        assertEquals("Hello", state.messages[0].content)
        assertEquals(MessageRole.GEMINI, state.messages[1].role)
        assertEquals("Hello from Gemini!", state.messages[1].content)
    }

    @Test
    fun `Gemini error sets error message and clears loading`() = runTest {
        fakeRepository.nextResult = Result.failure(RuntimeException("Network error"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network error", state.errorMessage)
        // User message should still be present
        assertEquals(1, state.messages.size)
        assertEquals(MessageRole.USER, state.messages[0].role)
    }

    @Test
    fun `onDismissError clears error message`() = runTest {
        fakeRepository.nextResult = Result.failure(RuntimeException("Error"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        viewModel.onDismissError()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onClearHistory removes all messages`() = runTest {
        fakeRepository.nextResult = Result.success("Response")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.messages.isNotEmpty())

        viewModel.onClearHistory()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun `messages persist to dao`() = runTest {
        fakeRepository.nextResult = Result.success("Response")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        // Check dao received the messages
        assertEquals(2, fakeDao.storedMessages.size)
        assertEquals("USER", fakeDao.storedMessages[0].role)
        assertEquals("GEMINI", fakeDao.storedMessages[1].role)
    }

    @Test
    fun `history is restored from dao on init`() = runTest {
        // Pre-populate the dao
        fakeDao.storedMessages.add(
            ChatMessageEntity(id = "pre-1", role = "USER", content = "Old message", timestamp = 1000L)
        )
        fakeDao.emitUpdate()

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("Old message", state.messages[0].content)
    }
}

// ── Fakes ────────────────────────────────────────────────────────────────────

class FakeGeminiRepository : GeminiRepository {
    var nextResult: Result<String> = Result.success("")

    override suspend fun generateText(prompt: String): Result<String> = nextResult
}

class FakeChatMessageDao : ChatMessageDao {
    val storedMessages = mutableListOf<ChatMessageEntity>()
    private val flow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = flow

    override suspend fun insertMessage(message: ChatMessageEntity) {
        storedMessages.add(message)
        flow.value = storedMessages.toList()
    }

    override suspend fun clearAll() {
        storedMessages.clear()
        flow.value = emptyList()
    }

    fun emitUpdate() {
        flow.value = storedMessages.toList()
    }
}
