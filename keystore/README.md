# Release signing

This folder holds the release signing key. Only this file is committed — everything else
here is gitignored because it's a secret:

- `healthwidget-release.jks` — the actual keystore (PKCS12), containing a self-signed RSA
  4096 certificate valid until 2056, alias `healthwidget`.
- `keystore.properties` — `storeFile`/`storePassword`/`keyAlias`/`keyPassword` read by
  `app/build.gradle.kts` to sign the `release` build type.

The filename and alias still say `healthwidget` — that predates the rename to SapGlance and
is deliberately left alone. Neither is user-visible: under Play App Signing this key is only
the *upload* key, and its certificate is seen by nobody outside Play Console. Renaming the
file would mean editing `keystore.properties` in lockstep, and renaming the alias
(`keytool -changealias`) would invalidate every backup copy already made of the old one — all
risk, no benefit. Leave it.

**Back up `healthwidget-release.jks` and the passwords in `keystore.properties` somewhere
durable outside this repo (a password manager, encrypted cloud storage) right now.** If
this file is lost after the app has been published, you cannot upload an update to the
existing Play Store listing under the same app — Google ties updates to the signing
certificate (or, under Play App Signing, to the upload key, which Google can reset via a
slower identity-verification process, but losing the key is still a real problem best
avoided).

## Building a signed release

With `keystore/keystore.properties` present, the normal release tasks pick it up
automatically:

```bash
./gradlew bundleRelease   # app/build/outputs/bundle/release/app-release.aab — upload this to Play Console
./gradlew assembleRelease # app/build/outputs/apk/release/app-release.apk
```

If `keystore/keystore.properties` is absent (e.g. on CI, or a fresh clone), the release
build type simply builds unsigned — `app/build.gradle.kts` checks for the file's existence
before applying the signing config, so this doesn't break `./gradlew build` in CI.

## Verifying a build's signature

```bash
<path-to-android-sdk>/build-tools/<version>/apksigner verify --print-certs -v app/build/outputs/apk/release/app-release.apk
```
