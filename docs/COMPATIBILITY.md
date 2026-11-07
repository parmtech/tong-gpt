# Tong GPT v0.2.0-dev — realistic compatibility and performance plan

## Product intent
Tong GPT is a small, independent shell for **ChatGPT Web**. It is not the official ChatGPT Android app, and no third-party source can promise one-to-one feature parity with OpenAI's proprietary Android client.

## Feature support matrix

| Feature | Current implementation | Verification |
| --- | --- | --- |
| ChatGPT web chats and account | ChatGPT WebView; site controls available features | Device sign-in test pending |
| Full-screen phone fitting | Edge-to-edge background + platform safe-insets for bars, camera cutout and keyboard | CI compiles; physical-device visual QA pending |
| Keep screen awake | Native toggle, ON by default while in foreground | Manual timeout test pending |
| Microphone and WebRTC audio | HTTPS origin-checked capture permission, Android mic permission; voice playback allowed after user initiates | Voice may still fail due unsupported WebView or site feature detection |
| Camera and video | HTTPS origin-checked camera permission, native fullscreen video view | Camera/video compatibility depends on website |
| Attach files | Android system file picker | Physical-device QA pending |
| Download ordinary HTTPS files | System DownloadManager with restricted cookie forwarding | Blob downloads not supported; physical QA pending |
| App shares and links | Website's features + safe external-browser fallback | Manual QA pending |
| Browser navigation | Single WebView with Android back, no tabs or address bar | Manual QA pending |
| Codex Web | Optional link to https://chatgpt.com/codex within same WebView | Mobile account eligibility and website UI may limit |
| Official Codex Remote pairing | **Not implemented** | Native proprietary mobile integration unavailable through WebView |
| Screen sharing from native Voice | **Not implemented** | Requires supported first-party mobile capabilities; website alone cannot guarantee |
| Push notifications and app integrations | **Not implemented** | Cannot recreate first-party backend or private APIs |

## Current optimisations
- Avoids embedded ads, analytics SDKs, custom network proxy, and a new backend.
- WebView uses platform default cache, storage, GPU decisions, current user agent, and lifecycle pause/resume.
- No forced GPU layer (can harm battery and low-end devices).
- Safe redirect allowlist and external fallback avoid unrestricted browsing.
- Offscreen app clears FLAG_KEEP_SCREEN_ON and pauses its WebView.
- Restores browsing state across configuration recreation when possible.
- No credentials are copied out of the WebView and no JavaScript bridges are present.

## What cannot be honestly guaranteed
- Being faster than the official native app without controlled device benchmarks.
- Full native feature parity or official OpenAI remote device pairing.
- Chrome login cookies working inside Android WebView.
- Voice session establishing reliably on every WebView/browser version.

## Performance test protocol
Use at least Android 10, Android 13 and a modern Android 15/16 device.
Measure cold launch to interactive, chat response load, message scroll jank, memory growth during 20+ turns,
voice connection success, screen rotation, keyboard resizing, device battery impact over a 30-minute session.
Compare the same account, Wi-Fi connection and phone hardware to Chrome and the official Android client.
Report measurements, never invent results.

## Standards reviewed
- Android 15 edge-to-edge: https://developer.android.com/about/versions/15/behavior-changes-15
- Android system insets: https://developer.android.com/codelabs/edge-to-edge
- WebView permissions: https://developer.android.com/reference/android/webkit/PermissionRequest
- ChatGPT Voice: https://help.openai.com/en/articles/20001274-chatgpt-voice
- OpenAI ChatGPT mobile feature reference: https://help.openai.com/en/articles/8142208-chatgpt-ios-and-android-app-faq
