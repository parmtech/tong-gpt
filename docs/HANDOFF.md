# Tong GPT — permanent project handoff

## Identity
- Owner/developer: ParmTech
- Public repo: https://github.com/parmtech/tong-gpt
- Package/application ID: au.parmtech.tonggpt
- Android version: 0.2.0-dev
- Destination: https://chatgpt.com/
- Product: independent Android shell for the official ChatGPT mobile site (not a new AI).
- First development milestone: 2026-11-07.

## User-approved features
- Single full-screen WebView, without address bar, tabs or bookmarks.
- Independent Tong GPT icon and explicit third-party attribution.
- Optional Keep Screen Awake, default ON, only while in foreground.
- Tiny top-right star to open native settings; no general browser UI.
- Native web integrations: file picker, microphone prompt, Android downloads, back navigation.
- External browser fallback for website features or login blocked in WebView.

## Implementation map
- Main screen: app/src/main/java/au/parmtech/tonggpt/MainActivity.kt
- URL origin allowlist: app/src/main/java/au/parmtech/tonggpt/UrlPolicy.kt
- Manifest: app/src/main/AndroidManifest.xml
- Vector launcher icon: app/src/main/res/drawable/ic_launcher_foreground.xml
- String resources and theme: app/src/main/res/values/
- Android CI: .github/workflows/android.yml
- URL security tests: app/src/test/java/au/parmtech/tonggpt/UrlPolicyTest.kt

## Security and compatibility
- Kotlin native Activity and Android WebView, no tracking SDKs or ads.
- Android API 26 minimum, API 35 target; HTTPS and user-agent defaults.
- Restrict main-frame pages to ChatGPT and specific OpenAI login subdomains.
- No JS bridge, injected scripts, credential interception, or custom backend.
- Reject SSL errors, mixed content and unsafe URI schemes.
- Mic only after origin verification and Android permission.
- System DownloadManager on Android 10+; browser fallback earlier.
- Google/Apple/Microsoft OAuth may refuse WebView; browser sessions do not automatically share with WebView.
- Website functions including blob downloads, voice or camera may be limited.
- Tong GPT cannot repair OpenAI native app or service issues.
- Use original independent branding, with clear unofficial disclaimer.

## Build automation
- Main branch push triggers test, lint, debug APK compilation.
- Passing CI uploads APK to Actions artifacts, retained 30 days.
- Main push also attempts an experimental v0.2.0-dev prerelease.
- Debug-signed, not Play Store-ready. Do not claim a working APK before CI verification.

## Tests and backlog
- Confirm CI and test real Android 10/13/15/16 phones.
- Check logins, messages, keyboard, uploads, back gesture, downloads and mic.
- Check Keep Awake ON/OFF, background/foreground and rotation.
- Device-test memory pressure, renderer recovery, edge-to-edge layout.
- Consider blob downloads and camera attachments if demanded by site changes.
- Production builds require durable signing and Play Store compliance review.


## Latest changes — v0.2.0-dev
- Screen-safe edge-to-edge root padding for status bar, navigation bar, cutout and soft keyboard.
- Android System WebView media playback no longer requires an additional user gesture after the user starts playback.
- WebRTC permissions now support microphone and optional camera/video capture, with strict ChatGPT HTTPS origin checks and Android runtime consent.
- Added fullscreen web media custom view (video).
- WebView uses the platform's default GPU layer rather than forced hardware layer.
- Native settings offer Reload ChatGPT and optional Codex Web link; no browser toolbar.
- Version 0.2.0-dev on public source branch. Experimental CI APK; device testing still pending.
- Compatibility and remote architecture research: docs/COMPATIBILITY.md and docs/CODEX_RESEARCH.md.
- No third-party code copied from GPL projects; use official APIs and original MIT-licensed implementation.

### Validation criteria
- GitHub Actions compile + unit tests + lint must pass; review the resulting prerelease asset.
- Test actual ChatGPT voice setup on a physical device before declaring solved.
- A microphone permission being granted is not evidence WebRTC has connected.
