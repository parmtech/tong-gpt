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


## v0.6 — Embedded QR scanner (experimental)
Tong GPT now includes a native on-device barcode/QR scanner under **Settings → Scan Windows Codex QR**
and **Windows Remote → Scan pairing QR / QR**.

**Two QR types must not be confused:**
1. A **ChatGPT Desktop official QR**, as shown by Settings → Connections → Control this PC,
   belongs to OpenAI's private pairing service. Scanning it in Tong GPT recognizes the
   official link and offers to hand off to the installed official ChatGPT Android app.
   It DOES NOT authenticate Tong GPT or sync official remote chats inside Tong GPT.
   OpenAI documents pairing from the official mobile app:
   https://learn.chatgpt.com/docs/remote-connections
2. A **Tong GPT Windows companion QR**, generated locally from our own WSS address
   and capability token, can prefill and initiate our independent Codex App Server
   connection after explicit user approval. It connects only to the locally configured
   Codex CLI/App Server, not the official ChatGPT remote-control backend.

### Generate the Tong GPT QR locally on Windows
- First run `windows/start-codex-remote.ps1` and set up the authenticated
  Codex app-server behind **private** Tailscale Serve (never Funnel).
- Make sure the resulting private URL uses `wss://` and tailnet HTTPS.
- Install Python and the offline QR generator dependency, if not already installed:
  `python -m pip install "qrcode[pil]"`
- In the checked-out Tong GPT repository, run:
  `python windows/generate-pairing-qr.py --endpoint "wss://YOUR-HOST.YOUR-TAILNET.ts.net:8443/"`
- The QR PNG is saved to your own Windows user profile under
  `%USERPROFILE%\.codex\tong-gpt-pairing.png`, not to GitHub.
- Open the PNG on the Windows screen, open Tong GPT → Scan Windows Codex QR on the
  Android phone, point camera and inspect the host shown by the confirmation.
- Approve Connect only for a computer you own/trust. The token is encrypted at rest
  in Android Keystore. The PC must be online, app-server running and tailnet reachable.
- Delete the QR PNG after pairing. Regenerate the token if anyone else saw it.

Never upload/screenshot/share the private Tong GPT QR. It holds a bearer token
that can grant remote access. The original official ChatGPT QR should also be kept
private and scanned only with an authorised mobile app.

### Scanner privacy and security
The native QR decoder runs entirely on the Android device using
`zxing-android-embedded`. QR contents are not sent to any scan service.
An unknown QR is rejected; it does not automatically open websites or execute commands.
Native camera permission is requested when opening the scanner. No remote access
is initiated until a user confirms the target hostname. TLS remains required.
