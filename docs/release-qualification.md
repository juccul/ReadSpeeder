# Release qualification

A public release is qualified only when the automated workflow passes and every manual item below has dated evidence attached to the release pull request or release issue. An unavailable required device or test is a failed gate, not a waiver.

## Automated gates

The `Release qualification` workflow runs on every pull request and on `main`:

- JVM tests, release lint, minified release APK, and release App Bundle build
- instrumentation tests on API 24, 28, 31, 34, and 36
- valid and malformed document-import coverage

The branch-protection rule for `main` must require the build job and all five instrumentation jobs.

## Release artifact

- [ ] Build the production AAB through the secret-backed upload signing configuration.
- [ ] Verify the upload certificate and archive its SHA-256 fingerprint.
- [ ] Install Play-generated split APKs from an internal-testing release.
- [ ] Confirm package name, version code/name, target SDK, permissions, and 64-bit libraries.
- [ ] Archive the R8 mapping and native debug symbols with the release.

## Installation and retained data

- [ ] Fresh install completes and every core flow works.
- [ ] Upgrade from the currently published/tester build with documents, covers, chapter metadata, progress, reader mode, and settings populated.
- [ ] Confirm all existing data remains readable after the upgrade.
- [ ] Exercise process recreation, background/foreground, screen lock, interruption, and rotation while reading.

## Core flows and layouts

- [ ] Import representative TXT, PDF, and EPUB files, including large and malformed samples.
- [ ] Exercise duplicate import, pasted text, search, every sort mode, edit, delete, and clear-library confirmation.
- [ ] Exercise RSVP at 1,000 WPM and the standard reader, chapter navigation, scrolling, and precise progress restoration.
- [ ] Test a small phone, standard phone, tablet, and foldable in portrait and landscape.
- [ ] Test gesture and three-button navigation, light/dark themes, and dynamic color.

## Accessibility

- [ ] Complete the core flows with TalkBack and verify traversal order and meaningful action labels.
- [ ] Complete the core flows using a hardware keyboard or switch-access equivalent.
- [ ] Verify 200% font scaling, touch targets, contrast, and no clipped or hidden controls.

## Performance and stability

- [ ] Run macrobenchmarks on a representative physical release device; emulator numbers are not release evidence.
- [ ] Record cold startup, library scrolling, large-document loading, reader playback, and standard-reader scrolling results.
- [ ] Exercise low-memory/background-process eviction and confirm recovery without data loss.
- [ ] Run a StrictMode-enabled qualification build and resolve disk/network work on the main thread.
- [ ] Review Play pre-launch reports for crashes, ANRs, accessibility, and compatibility failures.

## Play declarations

- [ ] Confirm the privacy policy and Data Safety declaration match backup, local document storage, and all production dependencies.
- [ ] Complete content rating, target-audience, ads, and app-access declarations.
- [ ] Record the approving tester, devices, OS versions, app version, date, and links to evidence.
