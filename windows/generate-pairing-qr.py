#!/usr/bin/env python3
"""Generate a PRIVATE Tong GPT companion pairing QR on your own Windows PC.

This QR is NOT the official ChatGPT Desktop pairing format.
Requires: python -m pip install "qrcode[pil]"
No internet service is contacted by this script.
The QR contains a sensitive bearer token: never upload or share it.
"""
import argparse
from pathlib import Path
from urllib.parse import urlencode, urlparse


def main() -> None:
    parser = argparse.ArgumentParser(description="Create a private Tong GPT Windows QR")
    parser.add_argument("--endpoint", required=True, help="Private Tailscale WSS endpoint e.g. wss://pc.tailnet.ts.net:8443/")
    parser.add_argument("--desktop", help="Optional private MeshCentral https:// address")
    args = parser.parse_args()

    address = urlparse(args.endpoint)
    if address.scheme != "wss" or not address.hostname or address.username or address.password:
        parser.error("--endpoint must be a secure wss:// URL with no embedded credentials")
    if address.fragment or address.query:
        parser.error("--endpoint must not contain a URL fragment or query")

    if args.desktop:
        desktop = urlparse(args.desktop)
        if desktop.scheme != "https" or not desktop.hostname or desktop.username or desktop.password:
            parser.error("--desktop must be a secure https:// URL with no embedded credentials")

    token_path = Path.home() / ".codex" / "tong-gpt-remote.token"
    if not token_path.is_file():
        parser.error("Token missing. Run windows/start-codex-remote.ps1 first.")
    token = token_path.read_text(encoding="ascii").strip()
    if len(token) < 32 or any(c.isspace() for c in token):
        parser.error("Invalid token; regenerate with start-codex-remote.ps1 -RegenerateToken")

    try:
        import qrcode
    except ImportError:
        parser.error('QR library is missing. Install locally: python -m pip install "qrcode[pil]"')

    fields = {"v": "1", "endpoint": args.endpoint, "token": token}
    if args.desktop:
        fields["desktop"] = args.desktop

    payload = "tonggpt://pair?" + urlencode(fields)
    image = qrcode.make(payload)
    # Saved under your profile, never inside the public GitHub working tree.
    output = Path.home() / ".codex" / "tong-gpt-pairing.png"
    image.save(output)
    print("Private QR saved locally:", output)
    print("Open that image on your PC, then in Tong GPT choose Scan Windows Codex QR.")
    print("The QR is a password-equivalent capability. Do not upload/screenshare it.")
    print("Delete the PNG after pairing. Regenerate the token if the QR is exposed.")


if __name__ == "__main__":
    main()
