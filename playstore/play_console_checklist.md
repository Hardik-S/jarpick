# JarPick Play Console Checklist

This file is the operator checklist for the Google-account-bound launch work.
It intentionally does not contain credentials, keystore passwords, AdMob account
IDs, or Play Console secrets.

## App Setup

- [ ] Create the Play Console app with package `com.batb4016.jarpick`.
- [ ] App name: `JarPick`.
- [ ] Default language: English.
- [ ] App or game: App.
- [ ] Free or paid: Free.
- [ ] Declarations: contains ads and in-app purchases.
- [ ] Category: Tools or Lifestyle. Prefer Tools if Play Console accepts the positioning.
- [ ] Enable Play App Signing. Use Google's generated app signing key.
- [ ] Keep the upload key outside git and configure it through `local.properties` or environment variables.

## Store Listing

- [ ] Title: use `playstore/title.txt`.
- [ ] Short description: use `playstore/short_description.txt`.
- [ ] Full description: use `playstore/full_description.txt`.
- [ ] What's new: use `playstore/listing/whats_new.txt`.
- [ ] Upload at least two phone screenshots from the release-like build.
- [ ] Upload the 1024 x 500 feature graphic from `playstore/assets/feature_graphic.png`.
- [ ] Privacy policy URL: `https://jarpick.vercel.app/privacy`.
- [ ] Developer website URL: `https://jarpick.vercel.app/`.
- [ ] Support email: `hshrestha.hba2026@ivey.ca`.

## App Content

- [ ] Data Safety: use `playstore/data_safety_notes.md` and `playstore/privacy/data_safety_draft.md` as the working draft, then confirm against the Play Console SDK disclosures.
- [ ] Ads declaration: Yes, the app contains ads.
- [ ] Content rating: utility app, no gambling, betting, lottery, raffle, prize drawing, medical, financial, dating, or regulated-use claims.
- [ ] Target audience: general audience, not designed for children.
- [ ] News, health, financial features, government, COVID-19, and other special app categories: not applicable unless Play Console asks a narrower question.
- [ ] App access: no login or restricted app access.
- [ ] Data deletion: local app data can be deleted in app settings, Android storage settings, or uninstall.

## Monetization

- [ ] Create one-time product `remove_ads_premium`.
- [ ] Set target price to `$2.99 USD` and allow Play's local price conversion unless a country-specific price is intentionally chosen.
- [ ] Activate the product before Play Billing validation.
- [ ] Add license testers before validating purchases.
- [ ] Confirm premium removes ads, unlocks premium modes, and restores after reinstall.

## Testing And Release

- [ ] Upload `app/build/outputs/bundle/release/app-release.aab`.
- [ ] Upload the matching R8 mapping file from `app/build/outputs/mapping/release/mapping.txt`.
- [ ] Run Play pre-launch report and resolve any crash, policy, or SDK warning.
- [ ] Create an internal testing track first.
- [ ] Because this is assumed to be a new personal developer account, run a closed test with at least 12 opted-in testers for 14 continuous days.
- [ ] Apply for production access after the closed test gate is satisfied.
- [ ] Start production as a staged rollout, then expand after Android vitals and reviews look clean.
