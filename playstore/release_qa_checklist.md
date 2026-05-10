# JarPick Release QA Checklist

Run this on a release-like build from Play internal testing whenever possible.
The debug build is useful for fast iteration, but Play Billing and production
ad behavior must be checked through a Play-distributed build.

## Device Baseline

- Device: Samsung S25 FE or newer equivalent.
- Package: `com.batb4016.jarpick`.
- Build: release/internal test.
- Start with clean app data.

## Core Flow

- [ ] Launch app and confirm no crash.
- [ ] Complete onboarding with "Create my first jar".
- [ ] Create `Dinner Jar`.
- [ ] Add `Pizza`.
- [ ] Pick from jar.
- [ ] Confirm result screen shows `Your pick: Pizza`.
- [ ] Accept result.
- [ ] Confirm jar list shows `Last pick: Pizza`.
- [ ] Confirm history shows `Pizza / Dinner Jar / FAIR`.

## Limits And Premium Gates

- [ ] Create starter jars and confirm the app handles the free jar limit clearly.
- [ ] Add choices until the free option limit messaging appears.
- [ ] Confirm `Fair` is available to free users.
- [ ] Confirm `Weighted`, `No repeat`, and `Elimination` route free users to Premium.
- [ ] Confirm release builds do not show the debug premium override.
- [ ] Confirm premium users can select all premium modes.
- [ ] Confirm premium users do not see ads.

## Picking Behavior

- [ ] Fair mode can pick from active choices.
- [ ] Weighted mode respects visible weights in repeated manual checks.
- [ ] No-repeat mode avoids immediate reuse until the pool resets.
- [ ] Elimination mode removes picked choices from the active round.
- [ ] `Hide For Now` removes the last pick from the current round.
- [ ] `Mark Used` records the pick as used and returns to the jar.
- [ ] Reset eliminated options makes hidden/eliminated options available again.

## Settings And Destructive Actions

- [ ] Settings privacy copy matches the hosted privacy policy at a high level.
- [ ] Restore purchase is visible and does not crash.
- [ ] Delete all local data shows a confirmation dialog.
- [ ] Confirming delete removes jars, choices, history, and local settings expected for v1.
- [ ] Cancelling delete preserves data.

## Ads And Billing

- [ ] Free users see banner ads only on jar list and jar detail.
- [ ] Ads are absent on onboarding, edit jar, edit option, pick animation, result, premium, settings, and history.
- [ ] Banner failures do not crash the app or block the main flow.
- [ ] Premium product details load for `remove_ads_premium`.
- [ ] Purchase flow completes for a license tester.
- [ ] Restore purchase restores premium state after reinstall.
- [ ] Unavailable billing state shows `Premium is temporarily unavailable.`

## Evidence To Keep

- [ ] Screenshot of jar list with populated jar.
- [ ] Screenshot of jar detail with all mode chips visible.
- [ ] Screenshot of result screen.
- [ ] Screenshot of history.
- [ ] Screenshot of premium screen with Play price.
- [ ] Screenshot of settings/privacy copy.
- [ ] `adb logcat` crash buffer showing no JarPick crash during the smoke flow.
