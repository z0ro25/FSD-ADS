package com.truongnt.fsd.nttads.admob

import android.app.Activity
import android.content.Context

/**
 * Entry point duy nhất của thư viện.
 *
 * Cách dùng:
 * 1. Gọi [init] một lần trong Application.onCreate()
 * 2. Gọi [showBanner], [showInterstitial], [showRewarded] ở bất kỳ đâu
 * 3. Gọi [destroy] trong onDestroy() của Activity gốc
 */
object AdsManager {

    private lateinit var provider: AdProvider
    private lateinit var config: AdsConfig
    private var isInitialized = false

    // ─── Init ────────────────────────────────────────────────────────────────

    /**
     * Khởi tạo thư viện. Phải gọi trước khi dùng bất kỳ hàm nào khác.
     *
     * Ví dụ:
     * ```kotlin
     * AdsManager.init(
     *     context  = this,
     *     config   = AdsConfig(
     *         bannerId       = "ca-app-pub-xxx/xxx",
     *         interstitialId = "ca-app-pub-xxx/xxx",
     *         rewardedId     = "ca-app-pub-xxx/xxx",
     *         isTestMode     = BuildConfig.DEBUG
     *     ),
     *     provider = AdMobProvider()
     * )
     * ```
     */
    fun init(context: Context, config: AdsConfig, provider: AdProvider) {
        this.config = config
        this.provider = provider
        provider.initialize(context.applicationContext, config)
        isInitialized = true
    }

    // ─── Banner ──────────────────────────────────────────────────────────────

    /**
     * Load và hiển thị Banner vào ViewGroup có id là [containerId].
     *
     * Ví dụ:
     * ```kotlin
     * AdsManager.showBanner(this, R.id.banner_container)
     * ```
     */
    fun showBanner(
        activity: Activity,
        containerId: Int,
        callback: AdsCallback = object : AdsCallback {}
    ) {
        checkInit()
        provider.loadBanner(activity, containerId, callback)
    }

    // ─── Interstitial ────────────────────────────────────────────────────────

    /**
     * Load rồi tự động hiển thị Interstitial.
     *
     * Ví dụ:
     * ```kotlin
     * AdsManager.showInterstitial(this, object : AdsCallback {
     *     override fun onAdClosed() { navigateToNextScreen() }
     *     override fun onAdFailed(error: String) { navigateToNextScreen() }
     * })
     * ```
     */
    fun showInterstitial(
        activity: Activity,
        callback: AdsCallback = object : AdsCallback {}
    ) {
        checkInit()
        provider.loadInterstitial(activity, object : AdsCallback {
            override fun onAdLoaded() {
                provider.showInterstitial(activity, callback)
            }
            override fun onAdFailed(error: String) {
                callback.onAdFailed(error)
            }
        })
    }

    // ─── Rewarded ────────────────────────────────────────────────────────────

    /**
     * Load rồi tự động hiển thị Rewarded.
     *
     * Ví dụ:
     * ```kotlin
     * AdsManager.showRewarded(this, object : AdsCallback {
     *     override fun onRewarded(type: String, amount: Int) {
     *         giveUserReward(amount)
     *     }
     *     override fun onAdFailed(error: String) { /* xử lý thất bại */ }
     * })
     * ```
     */
    fun showRewarded(
        activity: Activity,
        callback: AdsCallback
    ) {
        checkInit()
        provider.loadRewarded(activity, object : AdsCallback {
            override fun onAdLoaded() {
                provider.showRewarded(activity, callback)
            }
            override fun onAdFailed(error: String) {
                callback.onAdFailed(error)
            }
        })
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    /**
     * Giải phóng tài nguyên. Gọi trong onDestroy() của Activity gốc.
     */
    fun destroy() {
        if (isInitialized) {
            provider.destroy()
        }
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    private fun checkInit() {
        check(isInitialized) {
            "AdsManager chưa được init. Hãy gọi AdsManager.init() trong Application.onCreate() trước."
        }
    }
}
