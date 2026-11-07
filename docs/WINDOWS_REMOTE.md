# Tong GPT v0.3.0-dev — Windows Remote guide

## Features being added
- **Codex:** Native Android client for the official Codex App Server. View available CLI/App Server threads, resume them, send text, observe streamed agent replies, command output and stop tasks.
- **Voice:** Android speech recognition turns an utterance into editable text; after you press Send, Codex works on your Windows PC. Android text-to-speech can read its reply aloud.
- **Live Desktop:** An embedded authenticated MeshCentral HTTPS portal streams your Windows desktop and enables mouse/keyboard control.
- **Split View:** Codex and the remote desktop can be displayed at the same time in Tong GPT.
- **Approvals:** One-time command/file-change approvals with explicit Approve/Deny; no automatic approval of unfamiliar requests.

This is **NOT** the proprietary Remote pairing protocol in the official ChatGPT Android app.

## First set up Windows Codex

1. Install official Codex CLI: https://github.com/openai/codex and sign in.
2. Install Tailscale on Windows and Android, using the same private tailnet. Enable tailnet HTTPS certificates.
3. Run the script from this project on YOUR Windows computer: windows/start-codex-remote.ps1
4. The script generates a strong local capability token and configures Tailscale Serve, not Funnel.
5. Copy the WSS URL displayed by Tailscale and the capability token shown locally into Tong GPT > Settings > Windows Remote > Codex. The URL resembles wss://your-pc.example.ts.net:8443/
6. Leave the Windows Codex App Server running and the PC online.

The script requires a recent Codex version with WebSocket capability-token support. The server binds to 127.0.0.1:4500; only the private tailnet should reach it via TLS. Never open port 4500 to the public Internet.

For background service operation you may later configure Windows Task Scheduler or a secure service, but do not enable unattended access without deliberate consent.

## Set up Windows Desktop streaming

1. Install the official MeshCentral server and its Windows agent, and create your own account: https://docs.meshcentral.com/install/windows/
2. Restrict its network exposure to trusted devices; enable a strong password, MFA and HTTPS. A no-login control panel must never be exposed publicly.
3. Make the MeshCentral HTTPS endpoint reachable only from your Tailscale tailnet, using a **different** HTTPS port than Codex (for example 8444).
4. In Tong GPT > Windows Remote > Desktop, enter your own https:// address, sign in on the MeshCentral portal, choose your Windows computer and open Desktop.
5. Select Split in Tong GPT to see live PC screen and Codex chats together.

MeshCentral is **not embedded or copied into our repository**. Tong GPT loads your own server's authenticated browser UI, and the Windows service must be configured separately.

## Voice behaviour

- Tap microphone in the Codex pane to open Android system speech recognition.
- Review/correct the transcript; pressing Send is required before execution.
- Live agent messages and terminal events appear in the Codex pane.
- Text-to-speech reads the final response if sound is enabled.
- This is turn-based push-to-talk control, not ChatGPT Live's proprietary full-duplex call.

## Security boundaries

- Secure WSS + bearer token, private tailnet, phone-side Android Keystore encrypted token storage.
- No API keys in public GitHub, no direct unauthenticated control ports.
- No automatic approval of commands or file edits. Unknown RPC approvals are rejected.
- Desktop loads only user-configured HTTPS host as its top-level website.
- No bypass of TLS errors, no JavaScript injection into ChatGPT.
- Use normal Windows sandbox and least permissions; never run server as admin unnecessarily.

## Important limits

- Existing official ChatGPT desktop-native chats are not guaranteed to appear in Codex CLI history.
- WebSocket App Server is experimental; compatibility depends on installed Codex CLI version.
- The APK alone cannot turn on a remote screen: Windows services must be configured and running.
- Performance and actual voice/login behaviour need physical-device testing.
- Successful CI does not prove real Windows-to-Android pairing has worked.

## Testing checklist

- [ ] Pair and authorize with PC using real private WSS URL
- [ ] List actual Windows Codex sessions
- [ ] Read old session, resume and submit new prompt
- [ ] See live output and stop tasks
- [ ] Review each approval prompt and deny safely
- [ ] Dictate prompt, edit, send; hear TTS reply
- [ ] Connect to MeshCentral and view/control Windows desktop
- [ ] Split mode shows both components
- [ ] Test network interruptions, certificate failures and keyboard fitting

## Sources
- https://learn.chatgpt.com/docs/app-server
- https://github.com/openai/codex
- https://github.com/Ylianst/MeshCentral
- https://docs.meshcentral.com/install/windows/
- https://tailscale.com/docs/reference/tailscale-cli/serve
