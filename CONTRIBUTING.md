# Contributing to TruePrint

Thank you for helping improve TruePrint.

## Before opening a change

1. Keep the app's direct private CUPS/IPP architecture intact.
2. Do not add cloud upload, public relay services, printer discovery dependencies, or embedded CUPS credentials without first discussing the design.
3. Keep network work out of composable functions and preserve the existing ViewModel/repository boundaries.
4. Run `testDebugUnitTest` and `assembleDebug` for code changes.

## Privacy rules

Never commit or attach real printer profiles, IP addresses, queue names, Tailscale names, documents, APKs, signing keys, `local.properties`, credentials, or sensitive logs. Use generic fixtures such as `Example_Printer` and `203.0.113.10` instead.

## Printer-specific behavior

The project includes a protected CUPS PPD job attribute, `Resolution = 600dpi`, which was required by a verified driver-backed queue. Do not simplify it to only a generic IPP resolution attribute without a targeted test and documentation of the queue behavior.
