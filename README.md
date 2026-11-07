<div align="center">

# ✦ TONG GPT

### ChatGPT Web, in a dedicated Android experience.

**A minimal, independent Android project by [ParmTech](https://parmtech.au).**

[![Android](https://img.shields.io/badge/Android-App-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Open Source](https://img.shields.io/badge/Source-Public-2367A6?style=for-the-badge)](https://github.com/parmtech/tong-gpt)
[![Status](https://img.shields.io/badge/Status-In%20Development-FF9800?style=for-the-badge)](https://github.com/parmtech/tong-gpt)

**One app. One destination. No extra browser clutter.**

---

</div>

## The story

I originally wanted a simpler way to use ChatGPT on my Android phone. After running into issues with the native Android app on my device, I preferred the mobile website and decided to make a dedicated app for it.

**Tong GPT started as a personal project.** I then made the repository public so other people could follow its development, learn from it, and potentially use or improve it.

This is not a new AI model or a reimplementation of ChatGPT. It is a focused Android application intended to display the **official [ChatGPT website](https://chatgpt.com/)** in an app-like, full-screen experience.

## What Tong GPT aims to offer

| Experience | Planned behaviour |
|:--|:--|
| 🎯 **Single purpose** | Opens only the ChatGPT mobile website |
| 📱 **App-first interface** | No URL bar, tabs, bookmarks, search engine, or general browser controls |
| 🖥️ **Full-screen presentation** | Immersive, responsive layout around the website |
| ☀️ **Keep Screen Awake** | Optional user-controlled toggle to keep the display on while the app is visible |
| ↩️ **Natural navigation** | Android back handling that respects in-page navigation |
| ⚡ **Lean architecture** | Minimal native overhead and sensible WebView lifecycle management |
| 🔐 **Website-managed sign-in** | ChatGPT authentication remains with the website; no separate Tong GPT account |

> **Project status:** Android source code is now available, with automated GitHub Actions build and tests. Real-device validation is still pending. A GitHub prerelease may appear only after CI builds successfully.

## Design philosophy

**Just ChatGPT. Nothing else.**

Tong GPT is intended to feel like opening a dedicated app rather than launching a conventional browser. The native shell should stay out of the way. There is no plan for a custom chatbot, proxy, ad injection, extra browsing tools, or a separate AI backend.

The app's own visual identity is **Tong GPT by ParmTech**. It is not presented as an official OpenAI app.

## Technical direction

- **Android:** Kotlin and a dedicated Android WebView.
- **Destination:** `https://chatgpt.com/`.
- **Navigation:** Restrict embedded browsing to the expected ChatGPT destination; send unrelated links to an external browser.
- **Display:** Use Android's screen-awake flag only while the user enables the setting and the activity is visible.
- **Web features:** Carefully handle permissions, file selection, downloads, lifecycle events, and system back navigation where supported.
- **Privacy:** No custom account database, unnecessary analytics, or collection of ChatGPT passwords by the native app.

### Sign-in compatibility

Some sign-in providers, including Google, may limit authentication inside embedded WebViews. Tong GPT cannot bypass those restrictions. If a login flow is blocked, a supported external-browser flow may be needed. Compatibility with ChatGPT's mobile site and third-party sign-in methods can change over time.

## Roadmap

- [x] Set up the Android project and app identity
- [x] Implement the dedicated ChatGPT WebView
- [x] Add safe navigation and external-link handling
- [x] Add optional Keep Screen Awake control
- [ ] Test sign-in, keyboard, uploads, and Android back navigation
- [ ] Improve startup speed, error states, and accessibility
- [ ] Publish a device-tested production APK in GitHub Releases

## Android source and experimental APK

The Android project is in [app/](app/). Every source update runs tests, Android Lint and an automated debug APK build.

- [GitHub Actions — build status](https://github.com/parmtech/tong-gpt/actions)
- [Releases — experimental APK when available](https://github.com/parmtech/tong-gpt/releases)
- [Technical handoff](docs/HANDOFF.md)
- [Physical device QA checklist](docs/QA_CHECKLIST.md)

**Important:** a CI-generated debug APK is experimental and may not support OAuth inside Android WebView. It is not an official OpenAI app.

## New in v0.4.0-dev

The optional Tong GPT toolbar now occupies its own small safe row above ChatGPT,
instead of covering the website's own top-right controls. About app and a native
microphone diagnostic are under Settings. Image downloads using `blob:` URLs on
trusted ChatGPT pages can be saved to Android Downloads / Tong GPT (Android 10+,
up to 10 MB, supported image/PDF formats); ordinary HTTPS downloads still use
Android DownloadManager. The Codex Remote panel's portrait/split sizing was fixed.

This is an experimental version: actual Android device testing still matters.
If ChatGPT says **Close other apps using the microphone**, the operating system
may have another active voice recorder, or WebRTC may not work reliably in
Android WebView. Use Settings > Test microphone to distinguish the cases, close
other recording apps and try Voice in Chrome if necessary. Tong GPT cannot
force microphone ownership or guarantee first-party voice feature parity.

## Windows Remote beta

Open Tong GPT → Settings (✦) → **Windows Codex Remote · Voice + Desktop**.
The included [Windows starter script](windows/start-codex-remote.ps1) connects your local Codex App Server
through private Tailscale Serve HTTPS; credentials are generated on your own Windows PC.
For live desktop screen/control, install and configure your own authenticated
[MeshCentral](https://github.com/Ylianst/MeshCentral) Windows service, then open its HTTPS portal
in Tong GPT's integrated **Desktop** tab. **Split** lets you view Codex events and the Windows desktop together.

Voice control uses Android speech-to-text and text-to-speech, **not** OpenAI's private
full-duplex Live Voice protocol. Approvals require an explicit user decision.
The official ChatGPT desktop app's proprietary sessions may not appear in the Codex
App Server's local thread listing.

See [Windows Remote setup, security and QA](docs/WINDOWS_REMOTE.md).

## Contributing

Suggestions, bug reports, and pull requests are welcome. Please avoid uploading credentials, session cookies, or private conversations into issues.

## Important disclaimer

**Tong GPT is an unofficial, independent project developed by ParmTech. It is not created, sponsored, approved, or endorsed by OpenAI.**

ChatGPT and OpenAI are trademarks of OpenAI. The website, AI models, accounts, subscription features, and availability are provided and controlled by OpenAI. This project does not promise to fix bugs in OpenAI's apps or services.

---

<div align="center">

**Built by ParmTech · Sydney, Australia**

*Originally for personal use. Shared with the community.*

</div>


**Experimental debug signing:** APK update may require uninstalling the previous build; this clears the in-app WebView login session. Use a persistent secure release signing key for upgrade-safe public distribution.


## New in v0.5.0-dev (experimental)
This iteration responds to actual Android screenshots:
- Windows Remote history and live desktop width bug fixed, with safe insets around Android status/navigation bars.
- Codex Web selection dismisses the settings panel before navigation.
- Native microphone button in Tong GPT's top bar supports **speech-to-text** via Android's installed recognizer. You can review and insert text into the ChatGPT composer, or copy it. It does not claim to replace OpenAI Live full-duplex Voice.
- ChatGPT trusted HTTPS file downloads are also retried with a native, signed-in-cookie-aware streaming downloader that saves to **Downloads / Tong GPT**; long-press on direct links for Save link. Generated image/data/blob handler accepts supported ZIP/PDF/DOCX/TXT/image types up to 25 MB.
- Experimental browser compatibility toggle in Settings, OFF by default. Switch it on only to test website voice/download issues and turn it off if login breaks.

**Limitations:** A screenshot showing "Failed to download file" is often a ChatGPT website-level failure before the Android download listener sees the link. Native fallback can help with valid downloadable links; it cannot restore files that no longer exist or links whose authorization expired. An ordinary app cannot force other applications to release the Android microphone. Try the in-app native 🎙 typing control, or a browser/official ChatGPT app for full Voice.

**Design reference:** [WebGPT](https://github.com/MrHuaweiFan/WebGPT) (MIT) informed the origin-specific Android WebView download and user-agent compatibility approach; this project's implementation is independent.
