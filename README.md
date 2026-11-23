# Arena — Android Client

A premium, fully-offline-UI Android client for the [Arena API](https://platform.arena.ai/docs/api) (OpenAI- and Anthropic-compatible).

- **Package:** `ai.arena.client`
- **App name:** Arena
- **Requires:** Android 8.1+ (API 26), fully tested target for Android 13+ (API 33–36)
- **Version:** 2.1.1

## Features

- **Streaming chat** — tokens render live with a quiet pulsing "Arena" indicator and stop button.
- **Thinking, your way** — reasoning (when the API returns it) renders in a collapsible card:
  - Global modes: **Collapsed / Expanded / Hidden** (Settings → Thinking)
  - Per-message expand/collapse override
  - **Thinking level: Off / Low / Medium / High** — `reasoning_effort` on OpenAI format,
    thinking budget (1k/4k/6k tokens) on Anthropic format. Off = provider default.
- **Connection test** — verifies your key with `GET /v1/models` only. **Free: 0 credits spent.**
- **Locked to the working model** — `coding-router-preview` (Arena Router), the model your
  promotional funds cover. No model picker to accidentally spend on direct models.
- **OpenAI & Anthropic formats** — switch per Settings.
- **Chats that persist** — drawer with recent conversations, new/select/delete, titles auto-named.
- **Code-first rendering** — fenced code blocks with copy buttons, inline code, bold, lists.
- **Arena dashboard UI** — warm charcoal, hairline-bordered cards, serif headings,
  white primary buttons, green status dot; dark + light, edge-to-edge, predictive back.

## Build the APK (GitHub Actions)

The repo ships with [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml):

1. Push to `main` (or open a PR) — or run the workflow manually from the **Actions** tab.
2. When the run finishes, download the **`Arena-Android-APK`** artifact from the run,
   grab **`Arena-Android-v1.0.0.apk`** from the [`apk`](https://github.com/masterace572-prog/Arena-ai-android-client/tree/apk)
   branch, or use the [Releases](https://github.com/masterace572-prog/Arena-ai-android-client/releases) page.
3. Install on device: `adb install Arena-Android-v1.0.0.apk` (or open the APK on the phone).

Local build (needs JDK 17 + Android SDK 35):

```bash
gradle assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

Release builds are signed with the debug key so they install out of the box. For
distribution, replace with your own keystore via CI secrets.

## Setup

No setup — the API key is **built into the app**; it opens straight into chat behind a
splash screen. First launch migrates defaults: Anthropic format, thinking **expanded**,
reasoning level **Medium** (a 400 auto-retries with thinking stripped, so chats never brick).

## v2.0

- Claude-inspired full UI: warm paper/charcoal, terracotta accent, serif display type
- Splash screen (Android 12+ SplashScreen API) + redesigned sparkle logo
- Credits card in Settings (router $50 promotional / all-models $0)
- Settings: connection test, format, system prompt, temperature,
  thinking display + reasoning level, Telegram contact
- Live thinking: reasoning streams open while the model works
- Centered reading column, pill composer, greeting empty state

## Structure

```
app/src/main/java/ai/arena/client/
  MainActivity.kt          # edge-to-edge single activity
  ChatViewModel.kt         # state: chats, streaming, connection, settings
  data/                    # settings (DataStore), conversations (JSON), model helpers
  net/ArenaClient.kt       # OkHttp + SSE for OpenAI & Anthropic streams
  ui/                      # Compose: chat, drawer, settings, picker, onboarding, theme
```
