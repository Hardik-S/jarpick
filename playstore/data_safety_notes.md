# Data Safety Notes

JarPick is local-first. Jars, choices, and pick history are stored on the user's device with Room and DataStore.

No account is required. No backend receives jar contents, choices, or history.

The app includes Google Mobile Ads SDK for banner ads in the free version. Google Mobile Ads may collect data for advertising, analytics, and fraud prevention. Premium removes ads.

The app includes Google Play Billing for the one-time `remove_ads_premium` product. Payment processing and purchase records are handled by Google Play.

First-party manifest permission:
- `android.permission.INTERNET` for ads and Play Billing services.

SDK-merged permissions to validate in Play Console:
- Google Play Billing may add `com.android.vending.BILLING`.
- Google Mobile Ads may add network state, advertising ID, and Android ad-services attribution permissions for ads, analytics, and fraud prevention.

Sensitive permissions intentionally not requested:
- Location
- Camera
- Microphone
- Contacts
- Calendar

JarPick is not positioned for children and is not for gambling, lotteries, raffles, betting, prize drawings, health, finance, medical, legal, dating, or regulated use.
