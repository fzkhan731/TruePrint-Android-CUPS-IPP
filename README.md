# TruePrint

TruePrint is an Android app for sending PDFs and images directly to a private CUPS printer with IPP. It is designed for printers reachable over a local network or a private VPN such as Tailscale.

```
Android phone -> private network or Tailscale -> CUPS -> printer
```

TruePrint does not use cloud printing, printer discovery, public port forwarding, analytics, or stored CUPS credentials. Your documents are submitted directly from the phone to the CUPS queue you configure.

> **Project status:** Development build. Test the app with a non-critical document before relying on it for important work.

## What it supports

- Print PDFs and JPEG/PNG images through a configured CUPS queue
- Pick a document from the phone or share one to TruePrint
- Test a printer connection with IPP `Get-Printer-Attributes`
- Copies, custom PDF page ranges, paper size and media type
- Portrait/landscape, duplex printing, and 2-up/4-up layouts when the queue supports them
- Local job history stored in the app's private data

## Before you start

You need all of the following:

1. An Android device running Android 8.0 (API 26) or newer.
2. A working CUPS server and printer queue. Confirm that it can print from the CUPS host before troubleshooting the app.
3. A private route from the phone to the CUPS server, such as the same LAN or Tailscale.
4. The CUPS server address, port, and queue name.

Do **not** expose CUPS to the public Internet or create router port forwarding for port 631. See [the CUPS setup guide](docs/CUPS_SETUP.md) for a private-network setup and [SECURITY.md](SECURITY.md) for the project security policy.

## Install the app

### Build it yourself

1. Install the current stable Android Studio and its Android SDK.
2. Clone this repository and open it in Android Studio.
3. Connect an Android phone with USB debugging enabled, or select an emulator.
4. Choose the `app` configuration and press **Run**.

From a terminal on Windows, a debug APK can also be built with:

```powershell
.\gradlew.bat assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### First launch

The app deliberately starts without a printer configured. Tap **Set up printer** (or open **Settings**) and enter the values for your own CUPS queue:

| Field | What to enter | Example |
| --- | --- | --- |
| Printer name | A label shown only in the app | `Office printer` |
| Protocol | `IPP` for a private HTTP IPP endpoint, or `IPPS` when your server supports HTTPS | `IPP` |
| Server | Server IP address or private DNS name; no `http://`, path, or slash | `100.x.x.x` or `cups.example.tailnet.ts.net` |
| Port | The CUPS IPP port | `631` |
| Queue | Exact queue name shown by CUPS | `Office_Laser` |

For a conventional CUPS queue, the generated URI is:

```
ipp://SERVER:631/printers/QUEUE
```

Tap **Test connection** before printing. A successful connection shows information actually returned by CUPS; it does not submit a print job.

## Set up CUPS privately

Follow [docs/CUPS_SETUP.md](docs/CUPS_SETUP.md) to install CUPS, add a printer, find the queue name, test it locally, and make it safely reachable only through a LAN or private VPN.

## Printer-specific queue options

TruePrint preserves one CUPS PPD option in every submitted job:

```text
Job Attributes group
nameWithoutLanguage  Resolution = 600dpi
```

This was physically verified for a development printer/queue whose PPD required the `Resolution=600dpi` option to select the correct raster path. It is not a substitute for capability discovery and is not a universal CUPS recommendation. Do not replace it with only the standard IPP `printer-resolution` attribute without testing the target queue.

## Troubleshooting

- **Cannot reach CUPS:** Confirm the phone's Wi-Fi/mobile data route, private VPN connection, server address, and port. Test the CUPS queue from its host first.
- **Queue unavailable:** Check the exact queue name with `lpstat -p` on the CUPS server.
- **Printer unavailable:** Use **Test connection** to inspect the returned CUPS state, then inspect the printer and CUPS queue.
- **PDF cannot be opened:** Select it again with the document picker or share it to TruePrint. The source app must grant access to the file.
- **Print settings have no effect:** CUPS queues may use driver/PPD-specific option names. Inspect the queue capabilities with `lpoptions -p QUEUE -l`.

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md). Never include a real server address, Tailscale hostname, credentials, personal document, or APK in an issue, commit, test fixture, or screenshot.

## License

This project is licensed under the [MIT License](LICENSE).
