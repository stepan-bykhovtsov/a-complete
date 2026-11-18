# A-Complete

[![JetBrains Plugin](https://img.shields.io/badge/JetBrains%20Plugin-A--Complete-blue.svg)](https://plugins.jetbrains.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-purple.svg)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/IntelliJ%20Platform-2026.2%2B-orange.svg)](https://plugins.jetbrains.com/docs/intellij)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

> **A-Complete** is a minimalist, lightning-fast AI code completion plugin for JetBrains IDEs.
> It takes you from pressing <kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>\\</kbd> to having clean, contextually aware code inserted directly into your editor in **~1.5 seconds**.

---

## ⚡ Why A-Complete?

- **Minimalist & Blazing Fast:** Zero bloat, zero intrusive tool windows or heavy panels. Press the shortcut and get code generated immediately.
- **Powered by OpenRouter & OpenAI Standards:** Use any top-tier model via OpenRouter (Claude 3.5 Sonnet, GPT-4o, DeepSeek V3/R1, Gemini 2.5 Flash, Qwen 2.5 Coder, Llama 3.3) or point to any OpenAI-compatible endpoint (OpenAI, LocalAI, Ollama, DeepSeek API).
- **Enterprise-Grade Security:** API keys are never stored in plain-text configuration files or committed to VCS. Keys are securely stored in your operating system's Credential Store via IntelliJ's `PasswordSafe`.
- **Non-Intrusive UX:** Errors and misconfigurations are surfaced via clean native balloon notifications with direct action buttons, rather than corrupting your source files with error comments.
- **Smart Indentation & Context:** Intelligently extracts surrounding file context, detects language and file types, and matches prompt indentation for seamless code insertion.
- **Resilient & Thread-Safe:** Uses IntelliJ `RangeMarker` to track insertion points even if you edit text while the request is in flight. Generates code as a single atomic undoable command (`Ctrl+Z`).

---

## 🚀 Quick Start

### 1. Installation
Install the plugin from the JetBrains Marketplace (or build and install from disk).

### 2. Configure Your API Key
1. Open **Settings / Preferences** (<kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>S</kbd> or <kbd>⌘</kbd> + <kbd>,</kbd>).
2. Navigate to **Tools &rarr; A-Complete**.
3. Paste your [OpenRouter API Key](https://openrouter.ai/keys) (or your preferred OpenAI-compatible key).
4. Choose or type your model.
5. Click **Test Connection** to verify latency and connectivity.
6. Click **Apply & OK**.

### 3. Generate Code
1. Place the cursor on a comment line (or select a prompt comment), for example:
   ```kotlin
   // sort list of users by age ascending and take top 5
   ```
2. Press <kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>\\</kbd> (or right-click and choose **A-Complete: Generate Code**, or use **Code &rarr; Generate...**).
3. The generated snippet is inserted right below your comment with the caret positioned at the end, ready to keep typing!

---

## ⚙️ Configuration Options

| Option | Default | Description |
|---|---|---|
| **API Key** | *Empty* | OpenRouter or compatible API key. Stored securely in OS Keychain. |
| **Model** | `~anthropic/claude-haiku-latest` | Model slug. Editable combo box supporting any dynamically fetched or custom model. |
| **API Base URL** | `https://openrouter.ai/api/v1` | Base URL for API requests. Supports custom self-hosted gateways or direct OpenAI endpoints. |
| **Timeout (seconds)** | `60` | Request timeout in seconds before failing gracefully (default: 60s). |
| **Context** | Full File | Sends full file context to the model for comprehensive understanding. |

---

## 🏗️ Architecture & Best Practices

The codebase is engineered strictly following [JetBrains Platform SDK Best Practices](https://plugins.jetbrains.com/docs/intellij):

```
src/main/kotlin/dev/bykhovtsov/acomplete/
├── actions/
│   └── GenerateCodeAction.kt       # BGT update thread, RangeMarker tracking, atomic WriteCommandAction
├── client/
│   ├── OpenRouterClient.kt         # HTTP/2 connection pooling, cancellation-aware, dual endpoint parser
│   └── model/
│       └── OpenRouterDto.kt        # Strongly-typed Gson DTOs (Chat & Responses APIs)
├── notification/
│   └── ACompleteNotifier.kt        # Native balloon notifications with action links
├── prompt/
│   └── PromptBuilder.kt            # High-precision system instructions and context builder
├── service/
│   └── ACompleteService.kt         # Application-level service coordinating pipeline
├── settings/
│   ├── ACompleteConfigurable.kt    # Kotlin UI DSL v2 settings panel with Test Connection
│   ├── ACompleteCredentials.kt     # PasswordSafe wrapper for OS credential manager
│   └── ACompleteSettings.kt        # PersistentStateComponent for non-sensitive settings
└── util/
    ├── ACompleteBundle.kt          # DynamicBundle for i18n
    ├── CodeCleaningUtils.kt        # Markdown code fence stripping & indentation alignment
    └── EditorContextExtractor.kt   # Context extraction, selection handling, language detection
```

---

## 🛠️ Building & Developing

### Requirements
- JDK 25
- Gradle 9.x (included via `./gradlew`)
- IntelliJ IDEA 2026.2+

### Gradle Tasks
- **Run IDE with plugin:**
  ```bash
  ./gradlew runIde
  ```
- **Run unit tests:**
  ```bash
  ./gradlew test
  ```
- **Verify plugin compatibility:**
  ```bash
  ./gradlew verifyPlugin
  ```
- **Build distribution ZIP:**
  ```bash
  ./gradlew buildPlugin
  ```

---

## 📄 License

Apache License 2.0. See [LICENSE](LICENSE) for details.
