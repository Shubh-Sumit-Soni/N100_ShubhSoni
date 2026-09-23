package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.audio.TextToSpeechManager
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.repository.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val secureApiKeyManager = SecureApiKeyManager()
    private lateinit var textToSpeechManager: TextToSpeechManager

    private val viewModel: ChatViewModel by viewModels {
        // On first launch, store the build-time API key securely in Android Keystore.
        // On subsequent launches, retrieve the encrypted key in-memory.
        val buildConfigKey = BuildConfig.GEMINI_API_KEY
        if (buildConfigKey.isNotBlank() && !secureApiKeyManager.hasStoredKey(this)) {
            secureApiKeyManager.storeApiKey(this, buildConfigKey)
        }

        val apiKey = secureApiKeyManager.retrieveApiKey(this) ?: buildConfigKey
        val hasKey = apiKey.isNotBlank()

        val db = AppDatabase.getInstance(this)
        val chatHistoryRepository = ChatHistoryRepository(
            messageDao = db.chatMessageDao(),
            conversationDao = db.conversationDao(),
            quizDao = db.quizAttemptDao(),
        )

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = apiKey),
            chatHistoryRepository = chatHistoryRepository,
            hasApiKey = hasKey,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        textToSpeechManager = TextToSpeechManager(this)

        setContent {
            GeminiApiComposeStarterTheme {
                ChatRoute(
                    viewModel = viewModel,
                    onSpeakMessage = { messageId, text ->
                        textToSpeechManager.speak(messageId, text)
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeechManager.shutdown()
    }
}
