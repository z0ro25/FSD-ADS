package com.truongnt.fsd.nttads.admob.ads

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.truongnt.fsd.nttads.R
import com.truongnt.fsd.nttads.admob.AdLibConfig
import java.text.SimpleDateFormat
import java.util.Locale

object RewardAds {

    private const val REWARDS_DAILY_COUNT = 3
    private const val DATE_FORMAT = "dd/MM/yyyy"

    private var mRewardAds: RewardedAd? = null
    private var isLoading = false
    private var isShowing = false
    var mLoadingDialog: Dialog? = null

    // ── Load ──────────────────────────────────────────────────────────────

    /**
     * @param adUnitId  app truyền vào — debug/release id do app quản lý
     */
    fun initRewardAds(context: Activity, adUnitId: String) {
        if (!isCanLoadAds()) return

        mRewardAds = null
        isLoading = true

        RewardedAd.load(context, adUnitId, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    mRewardAds = ad
                    ad.setOnPaidEventListener { adValue ->
                        AdLibConfig.trackRevenue(adValue.valueMicros, adValue.currencyCode, "rewarded")
                        AdLibConfig.notifyImpression(
                            "Reward",
                            ad.responseInfo?.mediationAdapterClassName,
                            ad.responseInfo?.responseId,
                            AdLibConfig.getScreenName(context::class.java.simpleName),
                            adValue.valueMicros / 1_000_000.0
                        )
                    }
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    mRewardAds = null
                    isLoading = false
                }
            })
    }

    // ── Show ──────────────────────────────────────────────────────────────

    fun showAds(activity: Activity, adUnitId: String, callback: RewardCallback) {
        if (AdLibConfig.isUserPremium()) { callback.onPremium(); return }

        if (isCanShowAds()) {
            try {
                showAdsFull(activity, adUnitId, callback)
            } catch (e: Exception) {
                callback.onAdFailedToShow()
            }
        } else {
            callback.onAdFailedToShow()
        }
    }

    /**
     * Load nếu chưa có rồi show luôn — tự hiện dialog loading trong lúc chờ.
     */
    fun loadAndShowAds(context: Activity, adUnitId: String, callback: RewardCallback) {
        if (AdLibConfig.isUserPremium()) { callback.onPremium(); return }

        if (isCanLoadAds() && mRewardAds == null) {
            isLoading = true
            mLoadingDialog = createLoadingDialog(context)
            mLoadingDialog?.show()

            RewardedAd.load(context, adUnitId, AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        mRewardAds = ad
                        ad.setOnPaidEventListener { adValue ->
                            AdLibConfig.trackRevenue(adValue.valueMicros, adValue.currencyCode, "rewarded")
                            AdLibConfig.notifyImpression(
                                "Reward",
                                ad.responseInfo?.mediationAdapterClassName,
                                ad.responseInfo?.responseId,
                                AdLibConfig.getScreenName(context::class.java.simpleName),
                                adValue.valueMicros / 1_000_000.0
                            )
                        }
                        isLoading = false

                        mRewardAds?.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                dismissAdsDialog()
                                mRewardAds = null; isShowing = false
                                callback.onAdFailedToShow()
                            }

                            override fun onAdShowedFullScreenContent() {
                                dismissAdsDialog()
                                isShowing = true
                                AdLibConfig.notifyCount("rewarded")
                                callback.onAdShowed()
                            }

                            override fun onAdDismissedFullScreenContent() {
                                isShowing = false; mRewardAds = null
                                initRewardAds(context, adUnitId)
                                callback.onAdDismiss()
                            }

                            override fun onAdClicked() {
                                AdLibConfig.notifyClick(
                                    "Reward",
                                    mRewardAds?.responseInfo?.mediationAdapterClassName,
                                    mRewardAds?.responseInfo?.responseId,
                                    AdLibConfig.getScreenName(context::class.java.simpleName)
                                )
                            }
                        }

                        mRewardAds?.show(context) { callback.onEarnedReward() }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        dismissAdsDialog()
                        mRewardAds = null; isLoading = false
                        callback.onAdFailedToShow()
                    }
                })
        } else if (mRewardAds != null) {
            showAds(context, adUnitId, callback)
        } else {
            callback.onAdFailedToShow()
        }
    }

    // ── State ─────────────────────────────────────────────────────────────

    fun isCanShowAds(): Boolean = !isLoading && !isShowing && mRewardAds != null

    fun isShowing(): Boolean = isShowing

    private fun isCanLoadAds(): Boolean = !isLoading && !isShowing

    // ── Dialog ────────────────────────────────────────────────────────────

    private fun createLoadingDialog(context: Activity): Dialog = Dialog(context).apply {
        setContentView(R.layout.fsd_ads_dialog_loading)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(false)
    }

    fun dismissAdsDialog() {
        mLoadingDialog?.dismiss()
    }

    // ── Private show ──────────────────────────────────────────────────────

    private fun showAdsFull(context: Activity, adUnitId: String, callback: RewardCallback) {
        mRewardAds?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                mRewardAds = null; isShowing = false
                callback.onAdFailedToShow()
            }

            override fun onAdShowedFullScreenContent() {
                isShowing = true
                AdLibConfig.notifyCount("rewarded")
                callback.onAdShowed()
            }

            override fun onAdDismissedFullScreenContent() {
                isShowing = false; mRewardAds = null
                initRewardAds(context, adUnitId)
                callback.onAdDismiss()
            }

            override fun onAdClicked() {
                AdLibConfig.notifyClick(
                    "Reward",
                    mRewardAds?.responseInfo?.mediationAdapterClassName,
                    mRewardAds?.responseInfo?.responseId,
                    AdLibConfig.getScreenName(context::class.java.simpleName)
                )
            }
        }
        mRewardAds?.show(context) { callback.onEarnedReward() }
    }

    // ── Daily reward helpers ──────────────────────────────────────────────

    /**
     * Ghi nhận user đã xem reward hôm nay.
     * App tự lưu vào SharedPreference — lib chỉ tính key theo ngày.
     *
     * @param saveCount  lambda app cung cấp để lưu count: (key: String, newCount: Int) -> Unit
     * @param getCount   lambda app cung cấp để đọc count: (key: String) -> Int
     */
    fun watchDailyReward(
        getCount: (key: String) -> Int,
        saveCount: (key: String, count: Int) -> Unit
    ) {
        val key = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis())
        val today = getCount(key) + 1
        saveCount(key, today)
    }

    fun isTodayRewardAvailable(getCount: (key: String) -> Int): Boolean {
        val key = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis())
        return getCount(key) < REWARDS_DAILY_COUNT
    }

    // ── Callback interface ────────────────────────────────────────────────

    interface RewardCallback {
        fun onAdShowed()
        fun onAdDismiss()
        fun onAdFailedToShow()
        fun onEarnedReward()
        fun onPremium()
    }
}
