package com.truongnt.fsd.nttads.admob

import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig

/**
 * Entry point duy nhất của AdLib.
 * Gọi AdLibConfig.install { ... } một lần trong Application.onCreate().
 *
 * Tất cả dependency nội bộ của app (RemoteConfig, SharePref, Tracking...)
 * được inject qua lambda — lib không phụ thuộc vào bất cứ class nào của app.
 */
object AdLibConfig {

    // ── Feature flags — app inject từ RemoteConfig ────────────────────────
    var isAdsEnabled: () -> Boolean = { true }
    var isInterEnabled: () -> Boolean = { true }
    var isBannerEnabled: () -> Boolean = { true }
    var isNativeEnabled: () -> Boolean = { true }
    var isOpenAdsEnabled: () -> Boolean = { true }
    var isRewardEnabled: () -> Boolean = { true }

    // ── Premium / subscription — app inject từ SharePref ─────────────────
    var isPremium: () -> Boolean = { false }
    var isSubscription: () -> Boolean = { false }
    var isTier1: () -> Boolean = { false }

    // ── Adjust revenue tracking — Adjust SDK được bundle trong lib ─────────
    /**
     * Tên source Adjust. Mặc định: AdjustConfig.AD_REVENUE_ADMOB
     * App có thể override nếu dùng mediation khác.
     */
    var adjustAdRevenueSource: String = AdjustConfig.AD_REVENUE_ADMOB

    /**
     * Hook thêm sau khi lib đã track Adjust.
     * App dùng để track Firebase, AppsFlyer, ROAS nội bộ...
     */
    var onAdRevenue: ((valueMicros: Long, currency: String, adType: String) -> Unit)? = null

    // ── Impression / click tracking — app inject từ TrackingManager ────────
    var onAdImpression: ((
        adType: String,
        adapter: String?,
        unitId: String?,
        screen: String?,
        valueMicros: Double
    ) -> Unit)? = null

    var onAdClick: ((
        adType: String,
        adapter: String?,
        unitId: String?,
        screen: String?
    ) -> Unit)? = null

    // ── Count tracking — app inject từ AppPreferences ─────────────────────
    /** adType: "interstitial" | "banner" | "native" | "rewarded" | "open" */
    var onAdCountIncreased: ((adType: String) -> Unit)? = null

    // ── Screen name mapping — app inject từ TrackingManager ───────────────
    var screenNameMapper: ((className: String) -> String) = { it }

    // ── Debug flag ────────────────────────────────────────────────────────
    var isDebug: Boolean = false

    // ── Cài đặt thời gian ─────────────────────────────────────────────────
    /** Delay giữa 2 lần show interstitial (ms). Mặc định 30 giây */
    var interDelayMs: Long = 30_000L

    /**
     * Gọi 1 lần trong Application.onCreate().
     *
     * Ví dụ:
     * ```
     * AdLibConfig.install {
     *     isDebug = BuildConfig.DEBUG
     *     isAdsEnabled = { RemoteConfigUtils.getBoolean(ADS_ENABLE) }
     *     isPremium = { SharePrefUtils.isPremium(context) }
     *     onAdRevenue = { _, _, _ -> StoryApplication.initROAS(...) }
     *     onAdImpression = { type, adapter, id, screen, value ->
     *         TrackingManager.logEventAdImpression(type, adapter, id, screen, value)
     *     }
     *     onAdClick = { type, adapter, id, screen ->
     *         TrackingManager.logEventAdClick(type, adapter, id, screen)
     *     }
     *     onAdCountIncreased = { type ->
     *         when(type) {
     *             "interstitial" -> AppPreferences.increaseInterCount(context)
     *             "native"       -> AppPreferences.increaseNativeCount(context)
     *             "rewarded"     -> AppPreferences.increaseRewardCount(context)
     *         }
     *     }
     *     screenNameMapper = { TrackingManager.mappingScreenName(it) }
     *     adjustAdRevenueSource = AdjustConfig.AD_REVENUE_ADMOB
     * }
     * ```
     */
    fun install(block: AdLibConfig.() -> Unit) = this.block()

    // ── Internal helpers (chỉ dùng trong lib) ─────────────────────────────

    internal fun getScreenName(className: String): String =
        screenNameMapper(className)

    /**
     * Track revenue qua Adjust (bundle trong lib) + gọi hook onAdRevenue cho app.
     */
    internal fun trackRevenue(valueMicros: Long, currency: String, adType: String) {
        try {
            val adRevenue = AdjustAdRevenue(adjustAdRevenueSource).apply {
                setRevenue(valueMicros / 1_000_000.0, currency)
            }
            Adjust.trackAdRevenue(adRevenue)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Gọi hook app (ROAS, Firebase...)
        onAdRevenue?.invoke(valueMicros, currency, adType)
    }

    internal fun notifyImpression(
        adType: String, adapter: String?, unitId: String?,
        screen: String?, valueMicros: Double
    ) {
        try {
            onAdImpression?.invoke(adType, adapter, unitId, screen, valueMicros)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal fun notifyClick(
        adType: String, adapter: String?, unitId: String?, screen: String?
    ) {
        try {
            onAdClick?.invoke(adType, adapter, unitId, screen)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal fun notifyCount(adType: String) {
        try {
            onAdCountIncreased?.invoke(adType)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal fun isUserPremium(): Boolean =
        isPremium() || isSubscription() || isTier1()
}
