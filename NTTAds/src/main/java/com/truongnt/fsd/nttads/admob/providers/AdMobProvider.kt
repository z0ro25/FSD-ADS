package com.truongnt.fsd.nttads.admob.providers

import android.app.Activity
import android.content.Context
import android.widget.FrameLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.truongnt.fsd.nttads.admob.AdProvider
import com.truongnt.fsd.nttads.admob.AdsCallback
import com.truongnt.fsd.nttads.admob.AdsConfig

/**
 * AdProvider cho Google AdMob.
 *
 * Dependency cần thêm vào build.gradle của app:
 * ```
 * implementation 'com.google.android.gms:play-services-ads:23.0.0'
 * ```
 *
 * AndroidManifest.xml:
 * ```xml
 * <meta-data
 *     android:name="com.google.android.gms.ads.APPLICATION_ID"
 *     android:value="ca-app-pub-xxxxxxxxxxxxxxxx~xxxxxxxxxx"/>
 * ```
 */
class AdMobProvider : AdProvider {

    private lateinit var config: AdsConfig
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    // Test IDs chính thức của Google
    companion object {
        const val TEST_BANNER_ID        = "ca-app-pub-3940256099942544/6300978111"
        const val TEST_INTERSTITIAL_ID  = "ca-app-pub-3940256099942544/1033173712"
        const val TEST_REWARDED_ID      = "ca-app-pub-3940256099942544/5224354917"
    }

    // ─── Init ────────────────────────────────────────────────────────────────

    override fun initialize(context: Context, config: AdsConfig) {
        this.config = config
        MobileAds.initialize(context)
    }

    // ─── Banner ──────────────────────────────────────────────────────────────

    override fun loadBanner(activity: Activity, containerId: Int, callback: AdsCallback) {
        val adUnitId = if (config.isTestMode) TEST_BANNER_ID else config.bannerId

        val adView = AdView(activity).apply {
            this.adUnitId = adUnitId
            setAdSize(AdSize.BANNER)
            adListener = object : AdListener() {
                override fun onAdLoaded() = callback.onAdLoaded()
                override fun onAdClicked() = callback.onAdClicked()
                override fun onAdFailedToLoad(error: LoadAdError) = callback.onAdFailed(error.message)
            }
        }

        val container = activity.findViewById<FrameLayout>(containerId)
        container.removeAllViews()
        container.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    // ─── Interstitial ────────────────────────────────────────────────────────

    override fun loadInterstitial(context: Context, callback: AdsCallback) {
        val adUnitId = if (config.isTestMode) TEST_INTERSTITIAL_ID else config.interstitialId

        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    callback.onAdLoaded()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    callback.onAdFailed(error.message)
                }
            }
        )
    }

    override fun showInterstitial(activity: Activity, callback: AdsCallback) {
        val ad = interstitialAd
        if (ad == null) {
            callback.onAdFailed("Interstitial chưa được load")
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() = callback.onAdShown()
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                callback.onAdClosed()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                callback.onAdFailed(error.message)
            }
        }
        ad.show(activity)
    }

    // ─── Rewarded ────────────────────────────────────────────────────────────

    override fun loadRewarded(context: Context, callback: AdsCallback) {
        val adUnitId = if (config.isTestMode) TEST_REWARDED_ID else config.rewardedId

        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    callback.onAdLoaded()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    callback.onAdFailed(error.message)
                }
            }
        )
    }

    override fun showRewarded(activity: Activity, callback: AdsCallback) {
        val ad = rewardedAd
        if (ad == null) {
            callback.onAdFailed("Rewarded chưa được load")
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() = callback.onAdShown()
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                callback.onAdClosed()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                callback.onAdFailed(error.message)
            }
        }

        ad.show(activity) { rewardItem ->
            callback.onRewarded(rewardItem.type, rewardItem.amount)
        }
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun destroy() {
        interstitialAd = null
        rewardedAd = null
    }
}
