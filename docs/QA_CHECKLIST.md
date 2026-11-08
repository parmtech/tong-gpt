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


## v0.5 critical QA based on user screenshots
- [ ] Windows Codex Remote top tabs appear below Android status bar (5G/time icons).
- [ ] Chat messages and desktop web contents display (no 0-width pane).
- [ ] Pairing form collapses after successful connection; Pair re-opens it.
- [ ] Keyboard does not obscure Codex composer; portrait and landscape Split work.
- [ ] Selecting Codex Web / Windows Remote / Reload from Settings closes menu first.
- [ ] Native top 🎙 prompts system recognizer; editable speech transcript can insert into ChatGPT input, else copies to clipboard. Nothing sends automatically.
- [ ] Test microphone diagnostic still runs; mic release is immediate.
- [ ] Browser compatibility toggle switches UA, reloads, and can be reverted.
- [ ] Download 2.7MB JPG via ChatGPT inline link (including when site displays failed-download banner); test long-press Save linked file.
- [ ] Download ZIP and PDF and verify actual bytes are not HTML login pages.
- [ ] Test signed link redirect and logged-in cookie use; no cookies sent to third-party host.
- [ ] Test revoked/expired file shows clear actionable error; regenerate rather than falsely claiming success.
- [ ] Test 25MB blob size limit; unsupported MIME gracefully falls back.
- [ ] No change in chat session on upgrade unless Android requires uninstall for debug signing.


## v0.6 native QR scanner
- [ ] Native QR scanner launches inside Tong GPT and asks for camera permission.
- [ ] QR view respects camera notch, navigation bar, and keyboard safe areas.
- [ ] Unknown QR is rejected without opening any link.
- [ ] Official ChatGPT Desktop remote QR shows honest handoff prompt, not
  a fake 'paired' success; explicit action opens official ChatGPT mobile app.
- [ ] Tong GPT companion QR shows only the destination hostname and asks for
  confirmation before connecting; token remains hidden.
- [ ] Scan actual locally-generated Windows QR; endpoint and token connect using WSS.
- [ ] Close scanner, deny permission, cancel confirmation: no connection starts.
- [ ] QR credentials are not included in GitHub, logs or screenshots.
- [ ] Tailscale private-only access, no public TCP exposure and TLS verification.


## v0.7 camera and voice regression
- [ ] Enhanced Google Scanner automatically opens, auto-zooms and reads stylized official QR or offers fallback.
- [ ] First-time Google Play services module download/unsupported device leads to usable embedded camera scanner.
- [ ] ZXing fallback tries both normal/inverted QR; user can hold steady and rescan.
- [ ] The scanner distinguishes QR decoding from pairing: official QR NEVER claims Tong GPT remote is connected.
- [ ] Official pairing requests user sign in to the same ChatGPT account/workspace in the official mobile app.
- [ ] QR scanner opening while ChatGPT Web is logged out explains that pairing and web sign-in are separate.
- [ ] Settings > Log in to ChatGPT navigates to the website's login page.
- [ ] Native speech allows Punjabi, Hindi, English Australia, and device-language selection.
- [ ] Failed Google recognition shows useful error and does not auto-send anything.
- [ ] Compare Android native microphone diagnostic with official ChatGPT Voice and Chrome on device.
- [ ] Uninstalling previous debug build may log out user; note until stable signing is in place.
