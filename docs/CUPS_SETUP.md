# Private CUPS setup guide

This guide explains a generic CUPS setup for TruePrint. The exact printer-driver and service-management steps vary by operating system, NAS appliance, and container image. Keep the service private; this is not a guide to publishing CUPS on the Internet.

## 1. Choose a private network path

Your phone must be able to reach the CUPS server on its private address. Typical safe options are:

- the same home or office LAN;
- a private VPN such as Tailscale;
- a private subnet routed through an existing VPN.

Do not create router port forwarding for TCP port 631. Do not put a public IP address into TruePrint.

## 2. Install and start CUPS

On Debian or Ubuntu, install CUPS with:

```bash
sudo apt update
sudo apt install cups
sudo usermod -aG lpadmin "$USER"
sudo systemctl enable --now cups
```

Sign out and back in after changing group membership. For TrueNAS, Docker, macOS, or another platform, use that platform's supported CUPS package or application workflow instead of copying the Debian commands.

## 3. Add the printer and create a queue

1. Connect the printer to the CUPS host or ensure the host can reach it.
2. Open the local CUPS administration page, normally `http://localhost:631`, from the CUPS host.
3. Choose **Administration** then **Add Printer**.
4. Select the connected/network printer and an appropriate driver or driverless option.
5. Give the queue a short, recognizable name, for example `Office_Laser`.
6. Set the correct default paper size and complete CUPS' test page locally.

For USB printers, this command can help show the device URI:

```bash
lpinfo -v
```

## 4. Record the queue name and supported options

On the CUPS host, run:

```bash
lpstat -p -d
lpoptions -p QUEUE_NAME -l
```

Replace `QUEUE_NAME` with the name created above. The first command lists queues and the default destination. The second command lists driver/PPD options such as paper size, media type, duplex, and resolution.

Test the configured queue on the host before using Android:

```bash
lp -d QUEUE_NAME /path/to/a-test-document.pdf
```

## 5. Make CUPS reachable privately

Allow the phone to reach CUPS only from the LAN or private VPN that you control. The preferred configuration depends on your platform:

- restrict firewall rules to the private LAN/VPN interface or subnet;
- prefer HTTPS/IPPS if the service is configured with a certificate trusted by the device;
- if you use HTTP IPP, keep it inside the private network/VPN only;
- keep the CUPS administration interface local or otherwise access-controlled.

Do not disable authentication broadly or bind the service to the public Internet merely to make a mobile connection work.

## 6. Configure TruePrint

In TruePrint, open **Settings** and enter:

| Setting | Example |
| --- | --- |
| Printer name | `Office printer` |
| Protocol | `IPP` |
| Server | `100.x.x.x` or `cups.example.tailnet.ts.net` |
| Port | `631` |
| Queue | `Office_Laser` |

Enter only the host/IP in **Server**—not `http://`, `ipp://`, `/printers/`, or the queue path. Use **Test connection** and confirm the printer data returned by CUPS before submitting a document.

## Troubleshooting checklist

1. Confirm CUPS prints locally from its own host.
2. Confirm the phone is on the intended LAN or private VPN.
3. Confirm the server address and port are reachable from the phone.
4. Confirm the queue spelling from `lpstat -p`.
5. Inspect supported options with `lpoptions -p QUEUE_NAME -l` before assuming that a generic option name works for a driver-backed queue.
