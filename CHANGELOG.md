<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# A-Complete Changelog

## [Unreleased]
### Major Architecture & Quality Refactoring
- **Security**:
  - Eliminated hardcoded tokens and comments containing API keys.
  - Migrated API key persistence from plaintext XML (`PersistentStateComponent`) to OS Credential Store via `PasswordSafe` (`CredentialAttributes`).
  - Added automatic backward-compatible migration for any existing plaintext settings keys.
- **Reliability & Thread Safety**:
  - Rewrote action execution with `ActionUpdateThread.BGT`.
  - Added `RangeMarker` tracking to guarantee accurate code insertion even when documents are modified while requests are in flight.
  - Ensured write actions are properly dispatched to the Event Dispatch Thread (EDT) via `invokeLater(ModalityState.defaultModalityState())` to prevent UI freezing and deadlocks.
  - Wrapped insertion into an atomic `WriteCommandAction` for single-step Undo/Redo (`Ctrl+Z`).
- **User Experience (UX)**:
  - Replaced intrusive in-code error comments with native JetBrains notification balloons.
  - Added a one-click "Open Settings" action button to the missing API key notification.
  - Designed modern Settings panel using Kotlin UI DSL v2 (`panel { ... }`).
  - Added real-time **Test Connection** button in Settings with latency measurement.
  - Replaced hardcoded model list with dynamic model fetching from the OpenRouter `/models` endpoint, local caching, and a 'Refresh Models' button.
  - Added menu entries in `GenerateGroup` (Code -> Generate) and `EditorPopupMenu`.
  - Automatically moves the caret to the end of inserted code and scrolls smoothly.
- **Client & Performance**:
  - Encapsulated network communication in a dedicated, reusable `OpenRouterClient` with HTTP/2 and connection pooling.
  - Added dual support for standard OpenAI `/chat/completions` and OpenRouter `/responses` formats.
  - Integrated cooperative task cancellation with `ProgressIndicator` and `HttpClient.sendAsync`.
  - Added smart context extraction window (default 250 lines) to minimize token consumption and reduce latency.
  - Added intelligent markdown code fence stripping and prompt indentation matching.
- **Codebase & Tooling**:
  - Organized project into clean, modular packages (`actions`, `service`, `client`, `settings`, `util`, `notification`, `prompt`).
  - Internationalized all user-facing strings using `DynamicBundle` (`messages/ACompleteBundle.properties`).
  - Configured Kotlin toolchain to Java 25 as required by IntelliJ Platform 2026.2+.
  - Added comprehensive unit test suite covering code cleaning, markdown stripping, indentation, DTO serialization, endpoint resolution, and prompt building.
  - Upgraded IntelliJ Platform Gradle Plugin settings to 2.20.1.

## [1.0.0] - 2026-11-19
- Initial prototype release.
