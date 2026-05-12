package com.truongnt.fsd.nttads.admob.ads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.truongnt.fsd.nttads.admob.AdLibConfig
import java.util.Date

object OpenAds {

    private var appOpenAd: AppOpenAd? = null
    private var isOpenShowingAd = false
    private var loadTimeOpenAd: Long = 0
    private var lastTimeShowAds = 0L

    /** App có thể thêm class Activity vào đây để tắt open ads cho màn đó */
    val disableClasses: ArrayList<Class<*>> = arrayListOf()

    // ── Load ──────────────────────────────────────────────────────────────

    /**
     * @param adUnitId  app truyền vào — debug/release id do app quản lý
     */
    fun initOpenAds(context: Activity, adUnitId: String, callback: () -> Unit) {
        if (AdLibConfig.isUserPremium()) { callback(); return }

        if (appOpenAd == null && isOpenAdsNotOverdue()
            && AdLibConfig.isAdsEnabled()
            && AdLibConfig.isOpenAdsEnabled()
        ) {
            appOpenAd = null
            AppOpenAd.load(
                context, adUnitId, AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        ad.setOnPaidEventListener { adValue ->
                            AdLibConfig.trackRevenue(adValue.valueMicros, adValue.currencyCode, "open")
                            AdLibConfig.notifyImpression(
                                "Openad",
                                ad.responseInfo?.mediationAdapterClassName,
                                ad.responseInfo?.responseId,
                                AdLibConfig.getScreenName(context::class.java.simpleName),
                                adValue.valueMicros / 1_000_000.0
                            )
                        }
                        loadTimeOpenAd = Date().time
                        callback()
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        appOpenAd = null
                        callback()
                    }
                }
            )
        } else {
            callback()
        }
    }

    // ── Show ──────────────────────────────────────────────────────────────

    fun showOpenAds(context: Activity, adUnitId: String, callback: () -> Unit) {
        if (AdLibConfig.isUserPremium()) { callback(); return }
        if (disableClasses.contains(context::class.java)) { callback(); return }

        val isAfterInterDelay = System.currentTimeMillis() - lastTimeShowAds > InterAds.TIME_DELAY

        if (isAfterInterDelay && isCanShowOpenAds()) {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    callback()
                }

                override fun onAdShowedFullScreenContent() {
                    isOpenShowingAd = true
                }

                override fun onAdDismissedFullScreenContent() {
                    isOpenShowingAd = false
                    appOpenAd = null
                    initOpenAds(context, adUnitId) {}
                    InterAds.startDelay()
                    startDelay()
                    callback()
                }

                override fun onAdClicked() {
                    AdLibConfig.notifyClick(
                        "Open_ads",
                        appOpenAd?.responseInfo?.mediationAdapterClassName,
                        appOpenAd?.adUnitId,
                        AdLibConfig.getScreenName(context::class.java.simpleName)
                    )
                }
            }
            appOpenAd?.show(context)
        } else {
            callback()
        }
    }

    // ── State helpers ─────────────────────────────────────────────────────

    fun isCanShowOpenAds(): Boolean =
        appOpenAd != null && !isOpenShowingAd && isOpenAdsNotOverdue()

    fun startDelay() {
        lastTimeShowAds = System.currentTimeMillis()
    }

    fun disableAdsOpenForActivity(activityClass: Class<*>) {
        if (!disableClasses.contains(activityClass)) disableClasses.add(activityClass)
    }

    fun enableAdsOpenForActivity(activityClass: Class<*>) {
        disableClasses.remove(activityClass)
    }

    private fun isOpenAdsNotOverdue(): Boolean {
        val diff = Date().time - loadTimeOpenAd
        return diff < 4 * 3600000L
    }
}
