# Keep Room generated schema/data classes stable for release shrinking.
-keep class com.batb4016.jarpick.data.** { *; }

# Google Mobile Ads and Play Billing publish their own consumer rules; this file is
# intentionally small so release builds do not hide accidental secret handling.
