# Validation of this development build

Completed:

- Compiled all application Java and generated resource classes against Android API 35 with JDK 17.
- Compiled and linked Android resources with AAPT2.
- Converted bytecode with D8 for minimum API 26, aligned the APK and signed it with a personal debug certificate.
- Verified the final APK signature (v2 and v3).
- Inspected packaged manifest: application ID `com.jai.threadtabs`, minimum API 26, target API 35, launcher activity present, READ_SMS and READ_CONTACTS only. No INTERNET, SEND_SMS or SMS-write permission.
- Passed all 14 pure-Java grouping policy checks (`sh tests/run.sh`).
- Compiled the additional Android XML import tests. These require an Android runtime and were not executed.

Not completed:

- The emulator did not become ready in this environment; no successful launch, visual UI inspection or gesture test is claimed.
- Real-device permission flow, live SMS refresh, contact names, reply handoff and persistence through Android process death still require device testing.
- Android XML parsing/import tests are included but not runtime verified.
- Gradle dependency resolution was blocked in this environment; the delivered APK was built using `tools/build_sdk.py`, not Gradle. The Gradle project and wrapper are provided for Android Studio builds.

Suggested first-device checks:

1. Launch sample mode; hold Mom's conversation and move it from Family to Work.
2. Open Morgan's conversation; move only the cookout message to Friends. Verify both tabs include the relevant conversation and opening either shows full history.
3. Move the whole Morgan conversation to Family. Verify individual overrides clear. Tap Undo.
4. Create a group, edit its icon/color, reorder it, then delete it; verify texts remain.
5. Switch theme and size, close/reopen the app and verify saved group assignments.
6. Add a keyword rule and an exact-number rule; verify first-match priority and manual override behavior. Sample conversations start with manual groups, so tap a message and choose “Use rules for entire conversation” first.
7. Connect phone SMS and test both granting and denying access. Verify the app offers backup import if Android restricts access.
8. Import a small SMS XML file, repeat the import, and verify it does not duplicate messages or change the phone SMS database.
9. Open Reply on a real conversation and verify the correct recipient in the existing texting app before sending.

This is a personal development build, not a fully device-tested replacement SMS client.
