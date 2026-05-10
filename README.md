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

For release, put the production banner unit in `local.properties` or a Gradle property:

```properties
ADMOB_BANNER_ID=ca-app-pub-your-production-id/your-banner-id
```

Do not commit AdMob account secrets, Play Console credentials, keystores, or passwords.

Gradle exposes the production banner through `BuildConfig.ADMOB_PRODUCTION_BANNER_ID`. The wrapper also accepts `ADMOB_BANNER_AD_UNIT_ID` as a future generic fallback, but the current skeleton uses `ADMOB_PRODUCTION_BANNER_ID`.

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

The repository ignores keystores, `local.properties`, and signing property files. Never commit keystores or passwords.

## Build The App Bundle

Build the Google Play Android App Bundle with:

```powershell
.\gradlew bundleRelease
```

Expected output:

```text
app/build/outputs/bundle/release/app-release.aab
```

If no release keystore is configured, debug builds still work. A Play-ready release AAB requires valid signing material outside git.

## Google Play Submission Caveats

Before submission, validate:

- Manifest only requests `INTERNET`.
- AdMob app ID and production banner unit are configured.
- Billing product `remove_ads_premium` is active.
- The app is not listed as children-directed.
- Store copy avoids gambling, raffle, lottery, betting, prize, health, finance, medical, legal, dating, or regulated claims.
- Data Safety answers match the final SDK list and shipped behavior.

## Play Store Drafts

Play Store files live in `playstore/`:

- `playstore/title.txt`
- `playstore/short_description.txt`
- `playstore/full_description.txt`
- `playstore/privacy_policy_draft.md`
- `playstore/data_safety_notes.md`
- `playstore/screenshot_plan.md`
- `playstore/feature_graphic_brief.md`

Additional working notes under `playstore/listing/`, `playstore/privacy/`, and `playstore/assets/` were kept as drafts, but the root `playstore/` files above are the submission-facing artifacts requested for this MVP.

The nested drafts are organized by Play Console area:

- `playstore/listing/` contains listing text drafts.
- `playstore/privacy/` contains privacy policy and data-safety drafts.
- `playstore/assets/` contains screenshot and feature graphic production notes.

## Architecture Notes

Room stores jars, options, pick history, and round state. DataStore stores onboarding, settings, limits, animation preferences, and the local purchase state. The domain picker is pure Kotlin so the random selection rules can be tested without Android framework dependencies.

Premium unlocks remove ads, unlimited jars, unlimited choices, weighted pick mode, no-repeat-until-empty mode, elimination mode, premium themes, full history, and future JSON import/export backup work.
