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


## v0.3.0-dev Windows Remote milestone
- Native Windows Remote Activity: app/src/main/java/au/parmtech/tonggpt/WindowsRemoteActivity.kt
- Encrypted Android Keystore token: app/src/main/java/au/parmtech/tonggpt/SecureCodexToken.kt
- Windows Codex private Tailscale Serve script: windows/start-codex-remote.ps1
- Full setup/security/testing guide: docs/WINDOWS_REMOTE.md
- UX: Settings → Windows Codex Remote; Codex / Desktop / Split tabs.
- Codex: authenticated WSS bearer to official App Server, thread listing/history/resume,
  interactive turn/start, streamed agent messages/commands, turn/interrupt,
  one-time manual command/file approvals (no automatic execution approvals).
- Voice: RecognizerIntent utterance-to-editable-text + Android TextToSpeech reply.
- Desktop: in-app, HTTPS-only MeshCentral web client for PC live desktop and remote controls.
- Important: MeshCentral server and Windows agent NOT bundled in APK.
  Setup on user-owned PC required, with credentials, private tailnet and MFA.
- No official ChatGPT Remote pairing or guaranteed access to all native Desktop chats.
- No full-duplex ChatGPT Live voice.
- CI compiled tests/lint and generated experimental v0.3.0-dev debug APK.
- Real Windows pairing, voice and screen streaming MUST be tested on-device.


## v0.4.0-dev UX, image download and microphone recovery (2026-11-08)
- Replaced floating star overlay with dedicated 44dp native header row; its menu
  no longer covers ChatGPT's image download, close, sharing or header controls.
- About Tong GPT moved to a clear Settings > About app action.
- Settings content scrollable on short/narrow screens.
- WebView starts below toolbar and uses safe Android status/navigation/IME insets.
- Added WebView blob/data image downloader using origin-scoped AndroidX WebKit
  WebMessageListener restricted to HTTPS chatgpt.com main frames.
- User-triggered downloads only; no credentials, chat messages or login DOM copied.
- Max 10 MiB; JPEG/PNG/WebP/GIF/AVIF/PDF only. Save to Android MediaStore
  Downloads/Tong GPT on Android 10+, with no new storage permission.
- Original normal HTTPS DownloadManager path retained.
- Added microphone test in Settings; short native AudioRecord sampling
  distinguishes Android microphone accessibility from WebView WebRTC issues.
- Added Chrome voice fallback; cannot override another app's microphone lock.
- Fixed Windows Remote invisible-pane bug (vertical LinearLayout weights were
  assigned width=0); responsive split portrait/landscape.
- v0.4.0-dev test/lint/build through GitHub Actions; physical-device voice and
  download must be tested with screenshots before claiming production-ready.
