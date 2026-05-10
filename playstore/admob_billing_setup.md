# JarPick AdMob And Billing Setup

## Local Release Properties

Create `local.properties` from `local.properties.example` on the release machine.
Keep it out of git.

```properties
JARPICK_RELEASE_STORE_FILE=C:/secure/path/jarpick-upload-key.jks
JARPICK_RELEASE_STORE_PASSWORD=...
JARPICK_RELEASE_KEY_ALIAS=jarpick-upload
JARPICK_RELEASE_KEY_PASSWORD=...
ADMOB_APP_ID=ca-app-pub-your-production-publisher-id~your-production-app-id
ADMOB_BANNER_ID=ca-app-pub-your-production-publisher-id/your-production-banner-id
```

`bundleRelease` is intentionally blocked until these values are configured.
Debug builds continue to use Google's public AdMob test IDs.

## AdMob

- [ ] Create an Android app in AdMob for package `com.batb4016.jarpick`.
- [ ] Create a banner ad unit for JarPick.
- [ ] Put the AdMob Android app ID into `ADMOB_APP_ID`.
- [ ] Put the banner ad unit ID into `ADMOB_BANNER_ID`.
- [ ] Replace the publisher ID in `site/app-ads.template.txt`.
- [ ] Publish the result as `/app-ads.txt` on the Vercel developer website.
- [ ] Verify AdMob reports the app-ads.txt crawl as found and authorized.

## Play Billing

- [ ] In Play Console, create a one-time product with ID `remove_ads_premium`.
- [ ] Set the target price to `$2.99 USD`.
- [ ] Activate the product before testing.
- [ ] Add license testers and install JarPick through a Play testing track.
- [ ] Verify product details load and show a real Play price.
- [ ] Verify purchase, acknowledgement, restore, pending/unavailable messaging, and premium ad removal.

## Release Build Commands

```powershell
.\gradlew.bat clean
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat bundleRelease
```

Expected Play upload artifacts:

- `app/build/outputs/bundle/release/app-release.aab`
- `app/build/outputs/mapping/release/mapping.txt`
