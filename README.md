# Arena — Android Client

A premium, fully-offline-UI Android client for the [Arena API](https://platform.arena.ai/docs/api) (OpenAI- and Anthropic-compatible).

- **Package:** `ai.arena.client`
- **App name:** Arena
- **Requires:** Android 8.1+ (API 26), fully tested target for Android 13+ (API 33–36)

## Features

- **Streaming chat** — tokens render live with a typing indicator and stop button.
- **Thinking, your way** — reasoning (when the API returns it) renders in a collapsible card:
  - Global modes: **Collapsed / Expanded / Hidden** (Settings → Thinking)
  - Per-message expand/collapse override
  - Live "Thinking…" pulse while the model reasons
- **Connection test** — verifies your key with `GET /v1/models` only. **Free: 0 credits spent.**
- **Model picker** — merges suggested high-rank models with everything your key can call:
  - `coding-router-preview` (auto router)
  - `claude-fable-5.2-max`, `claude-fable-5.1-max`, `gpt-6.2-astra-max`, `gpt-6-astra-max`,
    `claude-opus-5.5-high`, `gpt-6.2-sol-max`, and more — plus the live list from your key
  - Search, or type any exact model ID
- **OpenAI & Anthropic formats** — switch per Settings; Anthropic adds optional extended-thinking requests.
- **Chats that persist** — drawer with recent conversations, new/select/delete, titles auto-named.
- **Code-first rendering** — fenced code blocks with copy buttons, inline code, bold, lists.
- **Premium UI** — flat smooth colors, no gradients, restrained typography, dark + light themes,
  edge-to-edge, predictive back.

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

## Setup in the app

1. Open the app → **Test connection & continue** with your Arena key
   (create one at [platform.arena.ai/dashboard/keys](https://platform.arena.ai/dashboard/keys)).
2. The test calls `GET /v1/models` — it does **not** spend your credits.
3. Pick a model from the chip in the top bar and chat.

> Note: chat requests consume your workspace balance exactly like any other API
> client; the router (`coding-router-preview`) is the most credit-friendly default.

## Structure

```
app/src/main/java/ai/arena/client/
  MainActivity.kt          # edge-to-edge single activity
  ChatViewModel.kt         # state: chats, streaming, connection, settings
  data/                    # settings (DataStore), conversations (JSON), model helpers
  net/ArenaClient.kt       # OkHttp + SSE for OpenAI & Anthropic streams
  ui/                      # Compose: chat, drawer, settings, picker, onboarding, theme
```
