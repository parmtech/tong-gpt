<div align="center">

# ✦ TONG GPT

### ChatGPT Web, in a dedicated Android experience.

**A minimal, independent Android project by [ParmTech](https://parmtech.au).**

[![Android](https://img.shields.io/badge/Android-App-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Planned-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
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

> **Project status:** The app and the features above are in development. This README describes the intended experience, **not** a claim that a tested APK is available.

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

- [ ] Set up the Android project and app identity
- [ ] Implement the dedicated ChatGPT WebView
- [ ] Add safe navigation and external-link handling
- [ ] Add optional Keep Screen Awake control
- [ ] Test sign-in, keyboard, uploads, and Android back navigation
- [ ] Improve startup speed, error states, and accessibility
- [ ] Publish a tested APK in GitHub Releases

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
