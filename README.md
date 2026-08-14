# ReadSpeeder

The RSVP reading app for android

[![Release qualification](https://github.com/juccul/ReadSpeeder/actions/workflows/release-qualification.yml/badge.svg)](https://github.com/juccul/ReadSpeeder/actions/workflows/release-qualification.yml)

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

## Privacy

ReadSpeeder has no in-app network access and does not collect or transmit user
data. Imported documents, settings, and reading progress are stored in the
app's private storage. See the [privacy policy](PRIVACY.md) for backup and
deletion details.

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

## Contributing and support

Bug reports and feature requests are welcome through
[GitHub Issues](https://github.com/juccul/ReadSpeeder/issues). See
[CONTRIBUTING.md](CONTRIBUTING.md) before submitting a change.

If ReadSpeeder is useful to you, you can support its development on
[Ko-fi](https://ko-fi.com/juckul).

## License

ReadSpeeder is free software licensed under the
[GNU General Public License version 3](LICENSE) (`GPL-3.0-only`). Third-party
components remain subject to their respective licenses and notices.
