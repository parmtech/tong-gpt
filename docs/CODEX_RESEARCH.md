# Codex remote compatibility — research and integration boundaries

## Official and community source code

| Project | License | Potential use | Decision |
| --- | --- | --- | --- |
| https://github.com/openai/codex | Apache-2.0 | Open-source Codex CLI, app-server and agent protocol on user-owned host | Approved reference for a separate future native remote companion |
| https://github.com/gronxb/codex-relay | Apache-2.0 | Local-first phone-to-computer agent transport design | Review security model before reusing; no wholesale code copied |
| https://github.com/nokryong/mobile-codex | GPL-3.0 | Independent mobile Codex and on-device tools | NOT copied into MIT Tong GPT; incompatible licence obligations and app security review required |
| https://github.com/Lqm1/pi-codex-app-server | Apache-2.0 | Example app-server compatible protocol in another agent | Reference only |

The full proprietary OpenAI ChatGPT Android source was not found in a public official repository.
Codex open source is **not** the same as native ChatGPT Android or its official mobile pairing protocol.

## What is possible now
- Tong GPT optionally navigates inside its one WebView to https://chatgpt.com/codex.
- Which Codex features appear there depends on the website, subscription, and mobile support.
- It does not grant access to the native ChatGPT Android Codex Remote tab.

## Future Codex Remote sidecar (not implemented)
- Run official Codex CLI + app-server on a user-owned Windows/Mac/Linux machine.
- Use authenticated, encrypted end-to-end remote transport and explicit device pairing.
- Build a separate optional Android screen for run state, chat threads, diffs, and approval prompts.
- Deny unsafe filesystem actions by default, scope each workspace, and expire device keys.
- Local app-server endpoints must NEVER be exposed as unauthenticated public HTTP.
- Do not ship credentials or import proprietary ChatGPT app pairing services.
- Codex CLI's official ChatGPT login may be available for eligible subscriptions; this does not prove eligibility for a custom client or any private OpenAI pairing scheme.

Docs:
- https://github.com/openai/codex/tree/main/codex-rs/app-server
- https://openai.com/index/work-with-codex-from-anywhere/
- https://help.openai.com/en/articles/20001275-chatgpt-work-and-codex

## Priority
Focus the v0.2 release on safety, fit, voice and reliability, not on bundling the entire coding agent
on a phone. Remote control should be a distinct user-approved project milestone with a threat model.
