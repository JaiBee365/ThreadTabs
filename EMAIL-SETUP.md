# Gmail and Outlook setup

Email connections are optional. SMS grouping, widgets, reminders, and message styling work without an account.

## Gmail

1. Create or select a Google Cloud project.
2. Enable the Gmail API.
3. Configure the OAuth consent screen and add your Google account as a test user while the app is in testing.
4. Create an Android OAuth client for package `com.jai.threadtabs` and the SHA-1 certificate used to sign your build.
5. Enter the client information in Haven Message under **Settings → Gmail + Outlook accounts**.

The app requests read access to mail and uses Google's authorization flow. It does not ask for or store your Google password.

## Outlook

1. Register a public-client application in Microsoft Entra/Azure.
2. Add redirect URI `com.jai.threadtabs://oauth/outlook`.
3. Enable delegated `Mail.Read` and `User.Read` permissions.
4. Enter the application ID in Haven Message under **Settings → Gmail + Outlook accounts**.

Some work or school tenants require administrator approval. The app uses the access and refresh tokens returned by Microsoft and does not ask for or store your Microsoft password.

Email is displayed and grouped inside Haven Message. Moving an email changes its Haven Message group; it does not apply Gmail labels or Outlook folders.
