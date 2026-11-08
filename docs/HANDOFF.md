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


## v0.5.0-dev — UI, ChatGPT download and voice repairs (2026-11-08)
Root cause identified from user screenshots:
- WindowsRemoteActivity root did not honour Android 15/16 edge-to-edge status bar.
- Remote transcriptScroll and desktopWeb still used width=0 weighted layout, so neither chat history nor desktop displayed even after outer pane fix.
- ChatGPT Settings opened Codex Web in the background without closing its own dialog.
- ChatGPT website itself sometimes displays "Failed to download file" before Android WebView's DownloadListener runs. Standard DownloadManager and blob-only handlers are insufficient for all generated files.
- Embedded ChatGPT WebRTC voice can claim microphone is unavailable even after Android permission; ordinary Android apps CANNOT force-stop unrelated apps or override microphone privacy controls.

Changes:
- Remote root now applies status/nav/cutout/IME insets; transcript and desktop views correctly use MATCH_PARENT width and weighted HEIGHT; pairing form collapses once connected, can reopen with Pair.
- Settings rebuilt into compact dark scrollable sections and dismisses before navigation/reload.
- Native toolbar speech mic invokes Android system RecognizerIntent. User edits transcript, optionally inserts into ChatGPT composer via a one-shot user-triggered script (never reads the page), or copies it to clipboard; never auto-sends.
- NativeFileDownloader uses bounded streaming HTTPS with authenticated ChatGPT cookies, origin-scoped credential forwarding, manual redirects, Referer, MIME and HTML/login-page rejection, MediaStore Downloads/Tong GPT.
- ChatGPT blob/data saver extended to PDF/ZIP/DOCX/TXT and common images up to 25 MB. WebView long-press on a link/image can show native Save/Open/Copy; this is a fallback when website downloads do not invoke the WebView download event.
- Optional browser-compatibility user-agent toggle, default OFF, intended for testing site feature detection. Reloads page when changed.
- Based on independent implementation and public Android documentation; consulted MIT-licensed WebGPT project (https://github.com/MrHuaweiFan/WebGPT) as a design reference for native HTTP downloads and WebView compatibility.

Crucial caveats:
- Native speech typing is not ChatGPT's full-duplex Voice conversation. WebRTC voice parity is NOT guaranteed.
- An expired/unauthorized ChatGPT attachment is not recoverable by the client; user may need to regenerate the file or open in Chrome.
- Download fetch supports only user-triggered trusted OpenAI/ChatGPT URLs; not every in-page JavaScript-generated download can be intercepted.
- Actual phone download, microphone, login and Codex Remote integration still require device QA.
- Android debug signing may require uninstalling earlier APK, losing local WebView login.


## v0.6.0-dev — native Windows Codex pairing scanner
- In-app scanner: app/src/main/java/au/parmtech/tonggpt/PairingQrScannerActivity.kt
- QR parser: app/src/main/java/au/parmtech/tonggpt/PairingCodeParser.kt
- Tests: app/src/test/java/au/parmtech/tonggpt/PairingCodeParserTest.kt
- Windows-local QR creator: windows/generate-pairing-qr.py
- Android dependency: com.journeyapps:zxing-android-embedded:4.3.0
- Settings → Scan Windows Codex QR opens Windows Remote with scanner immediately.
  Windows Remote also has a QR scanner button; camera permission is requested.
- Support only versioned tonggpt://pair payloads using WSS and a strong bearer
  token; review hostname with explicit approval before saving in Android Keystore.
- Official ChatGPT Desktop QR is detected and can optionally be opened with
  official ChatGPT Android app. It CANNOT pair Tong GPT with proprietary OpenAI
  Remote sessions, nor silently share the user's official Codex Remote chats.
- QR payload is never logged, printed or uploaded by the Android client.
- A production-like pairing handshake and Windows integration require real-device
  verification; code build and unit tests are necessary but not sufficient.


## v0.7.0-dev (2026-11-08) — QR decode, sign-in and native voice follow-up

User phone screenshots show:
- Codex Desktop's heavily stylized dotted QR inside the local ZXing camera preview, without detection.
- Tong GPT ChatGPT WebView logged out. A local QR scanner is independent from ChatGPT Web sign-in; this never authorizes official Remote.
- Website dictation reports "Couldn't start dictation"; official website Voice asks for microphone permission while logged out; the separate Android/Google recognizer reports "Didn't catch that" (user speaks Punjabi/Hindi).
Changes:
- QR scanner now first tries Google Code Scanner 16.1.0 (on-device decoding, auto zoom). If unavailable/canceled, app retains the embedded ZXing camera scanner with TRY_HARDER and mixed normal/inverted decoding; Google module may need to download on first use.
- QR UX explicitly explains that official OpenAI QR requires official ChatGPT app + same account/workspace; Tong GPT companion QR is an independent connection and its scanner can work without ChatGPT website login.
- Main Settings includes Log in to ChatGPT action and clearer scanner label.
- Native voice transcription prompts for Punjabi (pa-IN), Hindi (hi-IN), English (en-AU) or phone language. Selected language is remembered; unsuccessful recognition reports a useful message rather than silently doing nothing.
- No claim that Google scanner can pair Tong GPT with private OpenAI remote backend. An official QR should hand off to the actual first-party app, where the user approves and signs in.
- GitHub Actions debug APKs use ephemeral debug signing: uninstall/reinstall may erase Tong GPT WebView cookies/login. Stable upgrade-safe signing requires an owner-provided private signing key stored in GitHub Actions secrets, NEVER committed to the public repository.
- Actual QR camera detection and official-app handoff require device testing. Screenshot QR codes can contain sensitive enrollment information: do not commit or share decoded code or pairing tokens.

See https://developers.google.com/ml-kit/vision/barcode-scanning/code-scanner and https://learn.chatgpt.com/docs/remote-connections.
