# ============================================================
# OBFUSCATION — gom toàn bộ internal class vào package 'x'
# ============================================================
-repackageclasses 'x'
-allowaccessmodification
-overloadaggressively

# Xóa thông tin debug / source map
-renamesourcefileattribute SourceFile
-keepattributes !SourceFile,!LineNumberTable
-keepattributes Signature,InnerClasses,EnclosingMethod

# ============================================================
# XÓA TOÀN BỘ LOG — không lộ thông tin runtime
# ============================================================
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# ============================================================
# GIỮ PUBLIC API — chỉ expose những gì cần thiết cho app
# ============================================================

# Config entry point
-keep public class com.truongnt.fsd.nttads.admob.AdLibConfig { public *; }
-keep public class com.truongnt.fsd.nttads.admob.AdLibConfig$* { public *; }

# Các ads object — giữ tên class & public method
-keep public class com.truongnt.fsd.nttads.admob.ads.InterAds { public *; }
-keep public class com.truongnt.fsd.nttads.admob.ads.BannerAds { public *; }
-keep public class com.truongnt.fsd.nttads.admob.ads.NativeAds { public *; }
-keep public class com.truongnt.fsd.nttads.admob.ads.OpenAds { public *; }
-keep public class com.truongnt.fsd.nttads.admob.ads.RewardAds { public *; }
-keep public class com.truongnt.fsd.nttads.admob.ads.NativeCollapseAds { public *; }

# Các interface callback public
-keep public interface com.truongnt.fsd.nttads.admob.ads.InterAds$Callback { *; }
-keep public interface com.truongnt.fsd.nttads.admob.ads.NativeAds$CallBackNativeAds { *; }
-keep public interface com.truongnt.fsd.nttads.admob.ads.RewardAds$RewardCallback { *; }

# NativeAdWrapper (cần cho showPreloadNative)
-keep public class com.truongnt.fsd.nttads.admob.ads.NativeAds$NativeAdWrapper { public *; }

# ============================================================
# GIỮ INTERNAL — cần cho runtime reflection/lifecycle
# ============================================================
-keepclassmembers class * implements androidx.lifecycle.Observer {
    public void onChanged(...);
}

# Adjust SDK
-keep class com.adjust.sdk.** { *; }
-keep interface com.adjust.sdk.** { *; }

# Google Ads SDK
-keep class com.google.android.gms.ads.** { *; }