# Gemini Chat — Enhanced Jetpack Compose App

An enhanced Android Gemini chat application built on the professor-provided **GeminiApiComposeStarter** repository. The app provides a secure, persistent, responsive, and tested AI chat experience using Google's Gemini API.

---

## Features

| Feature | Description |
|---|---|
| **Gemini AI Chat** | Multi-turn conversation with Google Gemini via the Generative AI SDK |
| **Material 3 UI** | Polished chat bubbles, top bar, empty state, loading indicator, and Snackbar errors |
| **LazyColumn Conversation** | Efficient scrolling list with stable message keys and auto-scroll |
| **Voice Input** | Speech-to-text via `RecognizerIntent` — tap the mic, speak, review, then send |
| **Room Persistence** | Chat history survives app restarts using Room database |
| **Preferences DataStore** | User preferences stored via Jetpack DataStore |
| **Secure API Key Handling** | AES-256-GCM encryption with Android Keystore; key never persisted in plaintext |
| **Responsive Layout** | Adapts to phones, tablets, portrait, and landscape orientations |
| **Dark Mode** | Follows system theme with Material 3 dynamic colors (Android 12+) |
| **StateFlow Architecture** | ViewModel exposes `StateFlow<ChatUiState>` collected with `collectAsStateWithLifecycle()` |
| **Unit Tests** | 11 ViewModel tests with fake repository and coroutine test dispatcher |
| **Compose UI Tests** | 11 instrumented Compose tests covering rendering, interaction, and state display |
| **Release Minification** | R8/ProGuard enabled for release builds |

---

## Architecture

```
UI Layer
├── MainActivity
├── ChatScreen (Composables)
│   ├── ChatTopBar
│   ├── ChatMessageList (LazyColumn)
│   │   └── ChatMessageBubble
│   ├── ChatEmptyState
│   ├── LoadingBubble
│   └── ChatInputBar (TextField + Voice + Send)
└── Theme (Material 3, Dark/Light)

ViewModel Layer
└── ChatViewModel
    ├── StateFlow<ChatUiState>
    ├── GeminiRepository (API calls)
    └── ChatHistoryRepository (Room)

Data Layer
├── GeminiRepository / GeminiRepositoryImpl
├── Room
│   ├── ChatMessageEntity
│   ├── ChatMessageDao
│   └── AppDatabase
├── ChatHistoryRepository
└── UserPreferencesRepository (DataStore)

Security Layer
└── SecureApiKeyManager
    ├── Android Keystore (AES-256 key)
    └── AES/GCM/NoPadding encryption
```

---

## Setup

### Prerequisites

- **Android Studio** Ladybug or newer
- **JDK 17**
- **Android SDK** with API 36 (compileSdk) and API 26+ device/emulator
- A valid **Gemini API key** from [Google AI Studio](https://aistudio.google.com/apikey)

### API Key Configuration

1. Copy the template:
   ```
   cp local.properties.example local.properties
   ```

2. Edit `local.properties` and add your real API key:
   ```properties
   GEMINI_API_KEY=your_real_api_key_here
   ```

3. **Build the project.** The key flows through Gradle → `BuildConfig.GEMINI_API_KEY` → encrypted storage:

   ```
   local.properties (git-ignored)
        ↓
   Gradle reads at build time
        ↓
   BuildConfig.GEMINI_API_KEY
        ↓
   First launch: encrypted with AES-256-GCM via Android Keystore
        ↓
   Ciphertext stored in private SharedPreferences
        ↓
   Subsequent launches: decrypted in-memory only when creating Gemini client
   ```

4. **Fallback chain** (in `app/build.gradle.kts`):
   ```kotlin
   val geminiApiKey =
       localProperties.getProperty("GEMINI_API_KEY")
           ?: System.getenv("GEMINI_API_KEY")
           ?: ""
   ```

> **⚠️ IMPORTANT:** Never commit `local.properties`. It is listed in `.gitignore`. Use `local.properties.example` as a reference.

---

## Security Model

### API Key Encryption Flow

1. **Android Keystore** generates and stores an AES-256 symmetric key (alias: `gemini_api_key_alias`)
2. On first launch, the build-time API key is encrypted using **AES/GCM/NoPadding** (128-bit authentication tag)
3. The **IV (12 bytes) + ciphertext** are Base64-encoded and stored in a private `SharedPreferences` file
4. The plaintext API key exists **only in memory** when initializing the `GenerativeModel` client
5. The key is **never logged, displayed, or included in error messages**

### Security Limitations

> **Client-side API key protection cannot make a client-side API key completely inaccessible to a determined attacker.**

Even with Android Keystore encryption:

- A rooted device can access the Keystore and decrypt the key
- The key must exist in memory to make API calls, where it can be extracted via debugging
- The compiled APK contains the encrypted key and the code to decrypt it
- Reverse engineering (even with R8 obfuscation) can reveal the decryption logic

**For production applications, consider:**

- **Backend proxy server** — the API key stays on the server, never on the client
- **Firebase App Check** — attests that requests come from your genuine app
- **API key restrictions** — restrict the key by Android app package name and SHA-1 fingerprint in Google Cloud Console
- **Usage quotas** — set per-key usage limits to mitigate abuse

---

## Building

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build (with R8 minification)
```bash
./gradlew assembleRelease
```

> The release build has `isMinifyEnabled = true` with ProGuard rules configured for the Generative AI SDK, Room, DataStore, and Coroutines.

---

## Testing

### Unit Tests
```bash
./gradlew testDebugUnitTest
```

**11 tests** covering:
- Initial state verification
- Prompt changes and validation
- Missing API key handling
- Message send flow (user + Gemini response)
- Error handling and dismissal
- Clear history
- Room DAO persistence
- History restoration on init

All tests use a `FakeGeminiRepository` and `FakeChatMessageDao` — no real API calls.

### Compose UI Tests
```bash
./gradlew connectedDebugAndroidTest
```

> Requires a connected device or emulator.

**11 tests** covering:
- Empty state rendering
- Input field, send button, voice button presence
- Text entry
- Message display with role labels
- Loading indicator display
- Send button disabled states (loading, empty prompt)
- Message list vs empty state

---

## Project Structure

```
app/src/main/java/com/fahim/geminiApiComposeStarter/
├── MainActivity.kt
├── data/
│   ├── GeminiRepository.kt          # Interface
│   ├── GeminiRepositoryImpl.kt       # Gemini SDK implementation
│   ├── local/
│   │   ├── AppDatabase.kt           # Room database
│   │   ├── ChatMessageDao.kt        # Room DAO
│   │   └── ChatMessageEntity.kt     # Room entity
│   ├── preferences/
│   │   └── UserPreferencesRepository.kt  # DataStore preferences
│   └── repository/
│       └── ChatHistoryRepository.kt  # Room ↔ domain mapping
├── model/
│   └── ChatMessage.kt               # Domain model + MessageRole enum
├── security/
│   └── SecureApiKeyManager.kt        # AES-256-GCM + Android Keystore
└── ui/
    ├── chat/
    │   ├── ChatScreen.kt             # Compose UI
    │   ├── ChatUiState.kt            # Immutable UI state
    │   └── ChatViewModel.kt          # ViewModel + StateFlow
    ├── text/
    │   └── BoldMarkdown.kt           # **bold** rendering
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## License

University assignment project. Not intended for production use.
