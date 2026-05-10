# Google Play Data Safety Draft

Use this as a Play Console working draft. Confirm final answers against the implemented app, SDK versions, and AdMob configuration before submission.

## Data Collection

- App activity: possible through AdMob analytics/advertising signals for non-premium users.
- Financial info: purchases are handled by Google Play Billing; JarPick receives purchase entitlement status but does not collect payment card details.
- User-provided content: jars and options are intended to remain local on device in the MVP.

## Data Sharing

- Google AdMob may receive advertising-related data for non-premium ad serving.
- Google Play Billing processes purchase data for `remove_ads_premium`.

## Security Practices

- No account system is planned for the MVP.
- No custom backend is planned for the MVP.
- Avoid entering sensitive personal data into jar names or options.

## Deletion

- Users can delete local data in app settings.
- Users can uninstall the app to remove local app data from the device.
