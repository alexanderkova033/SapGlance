# Privacy Policy

**Last updated:** 2026-07-24

SapGlance is built around one rule: SapGlance itself never sends your data anywhere.
The one exception is entirely outside SapGlance's own code — Android's own device backup
system, covered in "Backups" below.

## What SapGlance collects

Nothing. SapGlance does not collect, store remotely, transmit, or sell any personal
data, usage data, or analytics of any kind.

## What SapGlance stores, and where

The app stores a small amount of data **only on your device**, using Android's local
DataStore mechanism:

- The most recently shown tips (up to the last 160), so the same one doesn't come up again
  too soon. The last 100 are never repeated at all; the rest are remembered only so the app
  can favour tips you haven't seen for a while over ones it showed you recently. This is a
  list of tip texts and nothing else — no timestamps, no counts, no record of whether you
  looked at any of them.

SapGlance itself never transmits any of this anywhere. There is no server for it to go
to: the app has no network permission at all (it does not request `INTERNET`), so it is
technically incapable of sending data off the device, even if it wanted to. The one path
this data can travel that isn't SapGlance's own code is Android's built-in device backup
system — see "Backups" below.

## Tip source links

The settings screen shows the research citation behind the currently displayed tip, with a
button to read the primary source. Tapping it hands off to your device's own web browser —
SapGlance itself never makes a network request (it can't; see above). The browser is a
separate app with its own network access and its own privacy policy, outside SapGlance's
control.

## Third parties

There are none. SapGlance contains no analytics SDK, no crash-reporting SDK, no
advertising SDK, and no other third-party tracking library of any kind.

## Accounts

There is no account system. SapGlance does not know who you are.

## Permissions SapGlance requests, and why

- **Receive boot completed:** used only to reschedule the widget's refresh after your device
  restarts.

SapGlance sends no notifications and requests no notification permission. It also does
not request location, contacts, storage, camera, microphone, health, or any other sensitive
permission.

## Backups

SapGlance opts in to Android's built-in backup system and explicitly includes its local
settings and tip history in it (both the legacy pre-Android-12 backup rules and the modern
Android 12+ rules cover the same data). What actually happens with that data depends
entirely on your device's own backup configuration, not on anything SapGlance does:

- If you have Android device backup turned on (Settings > System > Backup, tied to your
  Google account) or use device-to-device transfer when setting up a new phone, your widget
  style and recent tip history are included, encrypted in transit and at rest by Android's
  backup service.
- **This is the one case where this data leaves your device.** Android's backup service
  uploads it to your Google account's backup storage. SapGlance does not operate, and has
  no access to, that storage — it's part of the operating system, governed by your Google
  account's own settings and privacy controls — but it is a genuine exception to "nothing
  SapGlance stores ever leaves the device," so it's called out here explicitly rather than
  glossed over.
- If you have device backup turned off, none of this data goes anywhere.
- You can turn Android backup off at any time in your device's system settings, independent
  of SapGlance, and it can be turned off there with no effect on the app's functionality.

None of this involves any SapGlance-operated server or infrastructure — there is none —
and SapGlance's own code never transmits this data itself; the only way it can leave the
device is through Android's own OS-level backup mechanism, entirely under your control.

## Children's privacy

Because SapGlance collects no data at all, it does not collect data from anyone,
including children.

## Changes to this policy

If this policy ever changes, the "Last updated" date above will change too, and the new
policy will be included in the app's next release.

## Contact

For questions about this policy, open an issue on the project's repository.
