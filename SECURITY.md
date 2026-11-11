# Security policy

## Supported use

TruePrint is intended for private CUPS servers reachable over a trusted LAN or a private VPN. Do not expose CUPS or IPP port 631 directly to the public Internet for use with this app.

Use IPPS/HTTPS where your CUPS deployment supports it. If you use HTTP IPP, keep the route private and protect it with network controls such as a LAN firewall or private VPN.

## Reporting a security issue

Do not open a public issue containing a server address, VPN hostname, credentials, personal document, or log with sensitive content. Until this project has a published private reporting channel, open a minimal public issue that describes the category of concern without sensitive details.

## What must not be committed

- CUPS administrator credentials, tokens, or passwords
- private IP addresses, DNS names, or Tailscale hostnames
- signing keys, keystores, or signing-property files
- `local.properties`, APKs, build output, device captures, or personal documents
- screenshots or logs containing private infrastructure details
