# Haven Message

A personal Android SMS organizer built for Jai: make your own tabs and move texts whenever you want.

## Install the included APK

1. Download `Haven Message.apk` on an Android phone running Android 8.0 or later.
2. Open it from Downloads and, if Android asks, allow that app to install this downloaded APK. Follow any device security prompts; do not disable device-wide protections.
3. Launch **Haven Message**. It starts with clearly labeled sample messages.
4. Try holding a conversation, then tap **Move**. Open a conversation and hold individual messages to move only those.
5. Open **Settings → Connect phone SMS** to request read access to real SMS. Contact-name access is optional.

The APK previously bundled with this project was an earlier organizer-only build. Build the current source to get the messaging version: it includes an in-app SMS composer, replies, SMS sending, incoming SMS delivery, and the Android default-SMS-app role.

## Included

- Family, Work, Friends and Other tabs to start; create more.
- Rename groups, choose any emoji/short icon and RGB hex color, and reorder tabs.
- Delete custom groups without deleting texts. One catch-all group remains and can be renamed.
- Move one or many conversations, or one or many individual messages.
- Undo the most recent grouping, group-edit or rule-edit action during the current session.
- Exact-sender and case-insensitive keyword rules, with editable priority.
- Search contact names, numbers and message bodies.
- Light/dark theme and three text sizes; Android system font scaling also applies.
- Load phone SMS in the background and refresh when the SMS provider changes while the app is open.
- Import a standard SMS XML backup as an alternative to live phone access.
- Compose and reply inside Haven Message after making it the default SMS app and granting SMS permissions.
- Receive incoming SMS messages, save them to the system SMS provider, and receive Haven Message notifications.
- Home-screen widgets with selectable source, group, color, and preview visibility.
- Reply reminders with notification alerts.
- Optional Gmail and Outlook account connections using provider sign-in; account setup is documented in `EMAIL-SETUP.md` when generated for the build.
- Per-message bubble/text colors, fonts, corner radius, photo backgrounds, and muted video backgrounds.
- Local storage for groups, styling, reminders, and email tokens; no Haven Message server or analytics.

## How groups work

Resolution order: **individual message move → conversation move → first matching rule → Other**.

Moving an entire conversation clears its individual-message overrides and applies the chosen group to future SMS in that conversation. Moving one message changes only that message. Tap a message for options to remove its override or return the whole conversation to rules.

A tab shows conversations containing at least one message assigned to that group. Its preview and count reflect matching messages. Opening a conversation shows the full history, including messages assigned elsewhere. “All” shows every conversation. Search narrows the visible matching messages.

Rules do not infer family relationships. Assign people manually or add their exact sender number, including the country code as it appears in the phone. Punctuation in numbers is ignored; country codes are not guessed. Keyword rules apply per message and may split a conversation across tabs.

Sample, phone and imported modes have separate saved group configurations, so testing with fictional messages does not change your real SMS organization.

## Important boundaries

- **SMS-first default app.** Haven Message can be selected as the default SMS app and can send, receive, and display SMS. MMS media and RCS conversations are not parsed or sent by this build; the MMS receiver displays a clear notification instead of silently losing the event.
- MMS attachments, group MMS and RCS are not read or imported.
- Android classifies `READ_SMS` as a hard-restricted permission. Whether it can be granted depends on the installer/device policy. A downloaded APK may install successfully but still be unable to read phone SMS. The app handles denial and offers backup import. See [Android's READ_SMS reference](https://developer.android.com/reference/android/Manifest.permission#READ_SMS).
- For development, install through Android Studio or `adb install app-debug.apk`, then request SMS access in the app. This does not guarantee that a managed device will grant access.
- Imported backups are snapshots, not a live connection. Reimport to refresh. Import replaces the previous imported snapshot and retains matching grouping assignments. It never inserts SMS into the phone database.
- Original SMS are never modified, deleted or marked read by this app. Group assignments exist only in Haven Message.
- Uninstalling clears app settings and imported messages. System cloud backup is disabled. Original phone SMS are unaffected.
- No settings export/cloud sync is included. Back up your source messages with your existing texting/backup tools before changing phones.

## Build in Android Studio

Open this folder as a project, let Gradle sync, and choose **Build → Build APK(s)**, or run:

```sh
./gradlew assembleDebug
```

Use JDK 17, Android SDK Platform 35, Build Tools 35.0.0 and Gradle 8.11.1. The project pins Android Gradle Plugin 8.9.2. First-time Gradle sync requires access to Google's and Maven's repositories. See [AGP compatibility](https://developer.android.com/build/releases/agp-8-9-0-release-notes).

Output: `app/build/outputs/apk/debug/app-debug.apk`. Install that APK on an Android 8.0+ phone, then choose Haven Message as the default SMS app from its Settings screen. The project uses the machine's normal debug signing key; it does not include a private signing key.

The included GitHub Actions workflow can build a downloadable debug APK if you place the project in your own GitHub repository. No repository has been created or published for you.

## Build from a terminal

The same project can be built from a terminal without opening Android Studio:

```sh
./gradlew assembleDebug
```

Requires JDK 17, Android Platform 35, Build Tools 35.0.0, and access to the Gradle/Google/Maven repositories on the first build. Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Tests

```sh
sh tests/run.sh
```

The 14 pure-Java policy checks cover rule order, message overrides, conversation overrides, future texts, deletion fallback, normalized sender numbers and clearing only the moved conversation's overrides. See `VALIDATION.md` for the delivered build's checks and remaining device tests.

## SMS XML format

Use **Settings → Import SMS backup (XML)** and select a file like:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<smses count="1">
  <sms address="+15550100101" date="1789084800000" type="1"
       body="Hello from an imported SMS" contact_name="Example" />
</smses>
```

Dates are Unix milliseconds. `type="1"` is received and `type="2"` is sent. Other types and MMS records are skipped. Identical records are deduplicated. Imports above 100,000 SMS are rejected. XML special characters must be escaped.

## Code map

- `MainActivity.java`: native Android UI and user actions.
- `Grouping.java`: independently tested precedence policy.
- `Store.java`: private on-device group/rule/override storage.
- `SmsData.java`: read-only SMS and contact access, samples, XML import.
- `tools/build_sdk.py`: reproducible direct SDK packaging.

SMS sending is performed inside Haven Message after it becomes the default SMS app and receives the required Android permissions.

## Security and privacy

- The app has no Haven Message server, analytics, advertising, or cleartext network traffic. Gmail and Outlook requests use HTTPS and provider OAuth.
- Outlook tokens and temporary OAuth state are encrypted with AES-GCM using a non-exportable Android Keystore key. Gmail uses Google's authorization flow and does not store a Gmail password.
- Groups, reminders, styles, imported SMS, and cached email are stored in the app's private Android data directory. They are sandboxed from ordinary apps, but the message data is not separately encrypted with a user passcode.
- Phone SMS remains in Android's system SMS provider. Standard carrier SMS is not end-to-end encrypted; Haven Message does not add encryption to SMS or MMS.
- This is a debug/development build. Anyone with the source can inspect or modify it, so use a release build with your own private signing key before distributing it or relying on it for sensitive communications.
- The ZIP contains source code only and intentionally does not contain a private signing key.
