# TruePrint

TruePrint is an Android document-printing application for a private CUPS printer reachable through your network or Tailscale.

## Architecture

Android device → private network/Tailscale → CUPS → printer

The app sends IPP Print-Job requests directly to the configured CUPS queue. Documents stay on the device and private network; TruePrint has no cloud upload, analytics, public port forwarding, or stored credentials.

## Supported baseline

- PDF selection through Android's Storage Access Framework
- Share a PDF to TruePrint
- Copies from 1 to 99
- Per-job paper-size selection, defaulting to A4
- Configurable local printer display name, server, port, protocol, and queue
- IPP Get-Printer-Attributes connection test

## Printer-specific options

The current printing engine includes a verified per-job PPD option:

```text
nameWithoutLanguage  Resolution = 600dpi
```

This selects a 600 dpi raster path for the printer profile used during development. It is not a universal CUPS requirement. Future generic-printer support needs capability discovery or printer-profile options so PPD-specific settings can be selected safely per queue.

## Troubleshooting

- **Cannot reach CUPS:** Check Tailscale, the configured server address, port, and mobile or Wi-Fi network.
- **Queue unavailable:** Confirm the queue name in Settings and that CUPS is running.
- **Printer unavailable:** Use Test Connection to inspect the queue state, then check the printer and its USB connection.
- **PDF cannot be opened:** Re-select it from the picker or share it to TruePrint again; the source app must grant URI access.

## Security

CUPS should remain private. TruePrint uses Android's document picker and share URI grants instead of broad storage permission. HTTP CUPS endpoints may require Android cleartext support; use HTTPS/IPPS where your deployment supports it.
