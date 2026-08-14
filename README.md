# ReadSpeeder

The RSVP reading app for android

ReadSpeeder is a free and open-source Android app for reading text with rapid
serial visual presentation (RSVP) or a conventional scrolling reader. Use it to
work through books, articles, study material, and pasted text at your own pace.

## Features

- Import TXT, PDF, and EPUB documents, or paste text directly into the app.
- Read one word at a time with adjustable RSVP speed, or switch to standard reading.
- Navigate chapters and automatically resume from saved reading progress.
- Organize a local library with covers, search, sorting, metadata editing, and deletion.
- Customize reading typography, spacing, themes, and Android dynamic color.
- Read offline without ads or an account.

ReadSpeeder supports Android 7.0 and newer.

## Build and run

Install JDK 17 or newer and the Android SDK 37.1, then clone the repository and
run the Gradle wrapper from its root:

```shell
./gradlew assembleDebug
```

On Windows, use `.\gradlew.bat assembleDebug`. The APK is written to
`app/build/outputs/apk/debug/app-debug.apk` and can be installed on a connected,
authorized device with:

```shell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Run the automated checks with:

```shell
./gradlew testDebugUnitTest connectedDebugAndroidTest lintRelease
```

`connectedDebugAndroidTest` requires a running emulator or connected device.
Use `./gradlew assembleRelease` to create a minified release APK; release signing
must be configured separately before distribution.
