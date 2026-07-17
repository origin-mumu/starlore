# Android update publishing

`app/version.properties` is the single source of truth for Android versions.

For every published build:

1. Increment `VERSION_CODE`. Android will reject an update whose code is not higher.
2. Update `VERSION_NAME` for the user-facing version.
3. Build the APK with the same signing key as previous releases.
4. Upload the APK under an immutable versioned name in MinIO.
5. Update `update/latest.json`, then upload it last to `releases/android/latest.json`.

Uploading the manifest last prevents clients from seeing a release before its APK is available.
