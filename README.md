# JarPick - Random Choice Jars

JarPick is an Android-only Kotlin app for creating jars of everyday choices and randomly picking one when a decision is low-stakes. It is built with Jetpack Compose, Material 3, Room, DataStore, Google Mobile Ads banner ads, and Google Play Billing for a one-time premium unlock.

## Product Scope

JarPick is intentionally local-only:

- No backend.
- No accounts.
- No AI.
- No push notifications.
- No external payments or subscriptions.
- No sensitive permissions.
- No location, camera, microphone, contacts, or calendar access.
- No gambling, lottery, raffle, betting, or prize drawing positioning.

The package name is `com.batb4016.jarpick`.

## Run A Debug Build

Use JDK 21 for this project. On this machine, Gradle is pinned in `gradle.properties`:

```powershell
.\gradlew clean
.\gradlew assembleDebug
```

The debug build uses Google's official AdMob test banner unit:

```text
ca-app-pub-3940256099942544/9214589741
```

Debug builds may expose a hidden premium override for development. Release builds set `ALLOW_DEBUG_PREMIUM_OVERRIDE=false`.

The banner wrapper lives at `app/src/main/java/com/batb4016/jarpick/monetization/AdMobBanner.kt`. It should remain the only ad entry point so the premium entitlement check and screen allowlist stay centralized.

## Tests

Run unit tests with:

```powershell
.\gradlew test
```

Current unit coverage focuses on the decision engine, free/premium limit logic, ad visibility policy, and history snapshot behavior.

## Configure AdMob

JarPick uses banner ads only in v1. Ads are blocked on onboarding, pick animation, result, premium, and settings screens.

For release, put both the production AdMob Android app ID and production banner unit in `local.properties` or Gradle/environment properties:

```properties
ADMOB_APP_ID=ca-app-pub-your-production-id~your-app-id
ADMOB_BANNER_ID=ca-app-pub-your-production-id/your-banner-id
```

Do not commit AdMob account secrets, Play Console credentials, keystores, or passwords.

Gradle injects the production AdMob app ID into the Android manifest and exposes the production banner through `BuildConfig.ADMOB_PRODUCTION_BANNER_ID`. Debug builds always use Google's public test AdMob app and banner IDs. Release builds run `validatePlayReleaseConfig` and fail if either production AdMob value is missing or still set to a public test ID.

## Configure Play Billing

Create a one-time in-app product in Play Console:

```text
Product ID: remove_ads_premium
Type: one-time product
Target price: $2.99 USD
```

The app uses Google Play Billing Library `8.3.0`. `BillingManager` connects to Google Play, queries product details, launches the purchase flow, restores purchases, acknowledges purchases, and exposes state through `StateFlow`.

`BillingManager` is intentionally client-only for the MVP. It grants premium only after Google Play reports `Purchase.PurchaseState.PURCHASED`; pending purchases stay locked until Play confirms them. A production backend can replace the entitlement decision later without changing the UI-facing `StateFlow` contract.

If product details cannot load, the app shows the fallback copy:

```text
Premium is temporarily unavailable.
```

The last known premium state is kept locally in DataStore.

## Release Signing

Release signing is configured from environment variables or `local.properties`.

```properties
JARPICK_RELEASE_STORE_FILE=C:/secure/path/jarpick-release.jks
JARPICK_RELEASE_STORE_PASSWORD=...
JARPICK_RELEASE_KEY_ALIAS=...
JARPICK_RELEASE_KEY_PASSWORD=...
```

The repository ignores keystores, `local.properties`, and signing property files. Never commit keystores or passwords. Use `local.properties.example` as the local release-machine template.

JarPick's Play upload path assumes Play App Signing is enabled in Play Console with Google's generated app signing key. The local keystore should be the upload key for signing `app-release.aab` before upload.

## Build The App Bundle

Build the Google Play Android App Bundle with:

```powershell
.\gradlew bundleRelease
```

Expected output:

```text
app/build/outputs/bundle/release/app-release.aab
```

`bundleRelease` is intentionally blocked until the release keystore and production AdMob values are configured. This prevents accidentally uploading a debug-signed or test-ad build to Play. The matching R8 mapping file for Play Console is:

```text
app/build/outputs/mapping/release/mapping.txt
```

## Google Play Submission Caveats

Before submission, validate:

- The first-party manifest only requests `INTERNET`; Ads/Billing SDK merged permissions must be reviewed in the final Play Console Data Safety form.
- AdMob app ID and production banner unit are configured.
- Billing product `remove_ads_premium` is active.
- The app is not listed as children-directed.
- Store copy avoids gambling, raffle, lottery, betting, prize, health, finance, medical, legal, dating, or regulated claims.
- Data Safety answers match the final SDK list and shipped behavior.
- New personal developer accounts complete the required closed test with at least 12 opted-in testers for 14 continuous days before applying for production access.
- The hosted privacy policy and support site are live on Vercel before the Play listing is submitted.

## Play Store Drafts

Play Store files live in `playstore/`:

- `playstore/title.txt`
- `playstore/short_description.txt`
- `playstore/full_description.txt`
- `playstore/privacy_policy_draft.md`
- `playstore/data_safety_notes.md`
- `playstore/screenshot_plan.md`
- `playstore/feature_graphic_brief.md`
- `playstore/play_console_checklist.md`
- `playstore/release_qa_checklist.md`
- `playstore/admob_billing_setup.md`

Additional working notes under `playstore/listing/`, `playstore/privacy/`, and `playstore/assets/` were kept as drafts, but the root `playstore/` files above are the submission-facing artifacts requested for this MVP.

The nested drafts are organized by Play Console area:

- `playstore/listing/` contains listing text drafts.
- `playstore/privacy/` contains privacy policy and data-safety drafts.
- `playstore/assets/` contains screenshot and feature graphic production notes.
- `playstore/assets/feature_graphic.svg` is the editable 1024 x 500 source graphic.
- `playstore/assets/feature_graphic.png` is the current 1024 x 500 Play upload candidate.

## Vercel Support Site

The static developer site lives in `site/` and is configured by `vercel.json`.

Routes:

- `/` - JarPick overview.
- `/privacy` - Play Console privacy policy URL.
- `/support` - support contact page.

Before launch, replace the publisher placeholder in `site/app-ads.template.txt`, publish it as `/app-ads.txt`, and verify AdMob's app-ads.txt crawler sees the authorized seller line.

## Architecture Notes

Room stores jars, options, pick history, and round state. DataStore stores onboarding, settings, limits, animation preferences, and the local purchase state. The domain picker is pure Kotlin so the random selection rules can be tested without Android framework dependencies.

Premium unlocks remove ads, unlimited jars, unlimited choices, weighted pick mode, no-repeat-until-empty mode, elimination mode, premium themes, full history, and future JSON import/export backup work.
