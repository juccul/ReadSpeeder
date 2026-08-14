# Contributing to ReadSpeeder

Thanks for helping improve ReadSpeeder.

## Before opening a change

- Search existing issues and pull requests to avoid duplicate work.
- Open an issue first for large features or behavior changes.

## Development setup

Install JDK 17 or newer and Android SDK 37.1, then build from the repository
root:

```shell
./gradlew assembleDebug
```

Use `.\gradlew.bat assembleDebug` on Windows.

## Verification

Run the checks relevant to your change. Before submitting a pull request, run:

```shell
./gradlew testDebugUnitTest lintRelease assembleRelease
```

When an emulator or device is available, also run:

```shell
./gradlew connectedDebugAndroidTest
```

Add or update tests for behavior changes. Keep commits focused and avoid
including generated build outputs, signing files, credentials, or local IDE
configuration.

## Pull requests

Explain the user-visible effect, how the change was tested, and any remaining
limitations. CI must pass before a change is merged.

By submitting a contribution, you agree that it may be distributed under the
project's [GNU General Public License version 3](LICENSE) (`GPL-3.0-only`).
