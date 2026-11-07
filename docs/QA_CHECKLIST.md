# Tong GPT — manual physical device tests

- [ ] Install successful CI APK, open ChatGPT in full screen.
- [ ] Confirm no URL bar, tabs, browser search or bookmarks.
- [ ] Log in/out; record any WebView OAuth limitation.
- [ ] Send messages, scroll, use keyboard, rotate and return from background.
- [ ] Tiny settings star works and Keep Screen Awake defaults ON.
- [ ] Screen Awake ON prevents timeout while the app is visible.
- [ ] Screen Awake OFF uses normal device timeout.
- [ ] Background app does not keep device screen on.
- [ ] Unrelated links open a regular browser.
- [ ] Upload file using Android system picker.
- [ ] Mic permission appears only when requested by ChatGPT.
- [ ] Download file (Android 10+).
- [ ] Retry network outage and check external browser fallback.
- [ ] TLS certificate failures never proceed.
- [ ] Test on Android 10, 13, 15/16 and review accessibility.

External-browser sign-in does not share the embedded WebView cookie jar.

## Additional v0.2 regression checks
- [ ] ChatGPT input fits above Android 15/16 navigation handle when keyboard is closed and open.
- [ ] All header icons visible beside selfie camera and rounded screen corners.
- [ ] Portrait/landscape + gesture navigation + three-button navigation.
- [ ] Start Voice: Android microphone prompt, web permission response, WebRTC connects and audio output plays.
- [ ] If voice remains stuck: use browser fallback, compare Chrome against WebView on the same device.
- [ ] Check camera/video request grants only verified ChatGPT origin, never unrecognised resource types.
- [ ] Fullscreen web video opens/closes and Back returns to chat.
- [ ] Codex Web settings button stays within ChatGPT domain; do not confuse it with native Codex Remote.
- [ ] Verify site reload button and external sign-in fallback.


## Windows Remote regression
- [ ] Codex Windows PC private Tailscale Serve WSS pairing
- [ ] Tokens protected in Android Keystore, never shared in GitHub
- [ ] List and resume real Windows Codex CLI/App Server sessions
- [ ] Send text, see live agent and terminal progress
- [ ] Pause/stop task and approve/deny commands explicitly
- [ ] Voice transcription editable before send; optional TTS reply
- [ ] Private MeshCentral HTTPS desktop view/control with touch/keyboard
- [ ] Split mode shows Codex and Windows screen simultaneously
- [ ] App handles TLS error, incorrect token, no network, unavailable PC
- [ ] No proprietary ChatGPT Android features falsely claimed


## v0.4 UI, download and microphone
- [ ] No ✦ star overlaps ChatGPT download icon, header, share button or image viewer.
- [ ] Dedicated Tong GPT toolbar has menu, and Settings > About app appears.
- [ ] Settings scrolls on small phones and landscape.
- [ ] Chat input and image preview stay inside display cutout and navigation safe area.
- [ ] User-generated PNG/JPG WebP blob image saves to Downloads / Tong GPT.
- [ ] Normal HTTPS download still uses DownloadManager.
- [ ] Unsupported/oversized blob gives a useful error without crashing or freezing.
- [ ] Download from a non-ChatGPT origin does not activate privileged blob handler.
- [ ] Microphone diagnostic responds and releases capture promptly.
- [ ] If ChatGPT Voice shows another-app-microphone warning, close recorder apps
  and retest in Chrome to isolate Android WebView incompatibility.
- [ ] Windows Remote Codex/desktop panels have nonzero width in portrait.
- [ ] Split uses portrait stacked rows / landscape side-by-side columns.
