package com.truongnt.fsd.nttads.admob.ads

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.truongnt.fsd.nttads.R
import com.truongnt.fsd.nttads.admob.AdLibConfig
import java.util.Date

object InterAds {

    val TIME_DELAY get() = AdLibConfig.interDelayMs

    private var adObserver = MutableLiveData<InterstitialAd?>(null)
    private var mInterstitialAd: InterstitialAd?
        get() = adObserver.value
        set(value) { adObserver.value = value }

    var timeShowed = 0L
    private var isLoading = false
    private var isShowing = false
    private var loadTimeAd: Long = 0
    private var lastTimeShowAds = 0L
    private var loadingDialog: Dialog? = null

    // ── Load ──────────────────────────────────────────────────────────────

    /**
     * @param adUnitId  truyền từ app: getString(R.string.int_inapp) hoặc test id
     */
    fun initInterAds(ac: Context, adUnitId: String, callback: () -> Unit) {
        if (AdLibConfig.isUserPremium()) { callback(); return }
        if (!AdLibConfig.isAdsEnabled() || !AdLibConfig.isInterEnabled()) { callback(); return }
        if (!isCanLoadAds()) { callback(); return }

        mInterstitialAd = null
        isLoading = true
        InterstitialAd.load(ac, adUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    mInterstitialAd = ad
                    ad.onPaidEventListener = OnPaidEventListener { adValue ->
                        AdLibConfig.trackRevenue(adValue.valueMicros, adValue.currencyCode, "interstitial")
                        AdLibConfig.notifyImpression(
                            "interstitial",
                            ad.responseInfo?.mediationAdapterClassName,
                            ad.adUnitId,
                            AdLibConfig.getScreenName(ac::class.java.simpleName),
                            adValue.valueMicros / 1_000_000.0
                        )
                    }
                    loadTimeAd = Date().time
                    callback()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    mInterstitialAd = null
                    callback()
                }
            })
    }

    // ── Show ──────────────────────────────────────────────────────────────

    fun showAdsSplash(activity: Activity, adUnitId: String, callback: (Long) -> Unit) {
        if (AdLibConfig.isUserPremium()) { callback(0L); return }
        if (isCanShowAds() && AdLibConfig.isAdsEnabled()) {
            try {
                showDialog(activity)
                Handler(Looper.getMainLooper()).postDelayed(
                    { showAdsFull(activity, adUnitId, callback, false) }, 800
                )
            } catch (e: Exception) {
                e.printStackTrace()
                dismissAdDialog()
                callback(0L)
            }
        } else {
            callback(0L)
        }
    }

    fun showAds(
        activity: FragmentActivity,
        adUnitId: String,
        callback: (Long) -> Unit,
        needLoadAfterShow: Boolean = true
    ) {
        if (AdLibConfig.isUserPremium()) { callback(0L); return }
        if (isCanShowAds() && AdLibConfig.isAdsEnabled()) {
            try {
                showAdsFull(activity, adUnitId, callback, needLoadAfterShow)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(0L)
            }
        } else {
            callback(0L)
        }
    }

    fun loadAndShowAds(activity: FragmentActivity, adUnitId: String, callback: (Long) -> Unit) {
        if (AdLibConfig.isUserPremium()) { callback(0L); return }

        dismissAdDialog()
        val canLoadAndShow = !isShowing && !isInDelayTime()
        if (!canLoadAndShow) { callback(0L); return }

        showDialog(activity)
        if (isLoading) {
            adObserver.observe(activity) { interAd ->
                if (interAd == null) {
                    if (!isLoading) { dismissAdDialog(); callback(0L) }
                } else {
                    dismissAdDialog()
                    showAds(activity, adUnitId, callback)
                }
            }
        } else {
            initInterAds(activity, adUnitId) {
                dismissAdDialog()
                showAds(activity, adUnitId, callback, false)
            }
        }
    }

    // ── State helpers ─────────────────────────────────────────────────────

    fun isCanShowAds(): Boolean =
        !isLoading && !isShowing && !isInDelayTime() && mInterstitialAd != null && !isAdsOverdue()

    fun isInDelayTime(): Boolean =
        System.currentTimeMillis() - lastTimeShowAds < TIME_DELAY

    fun isShowing(): Boolean = isShowing

    fun startDelay() {
        lastTimeShowAds = System.currentTimeMillis()
    }

    private fun isCanLoadAds(): Boolean =
        !isLoading && !isShowing && (mInterstitialAd == null || isAdsOverdue())

    private fun isAdsOverdue(): Boolean =
        Date().time - loadTimeAd > 4 * 3600000L

    // ── Dialog ────────────────────────────────────────────────────────────

    private fun showDialog(activity: Activity) {
        try {
            if (loadingDialog == null) {
                loadingDialog = Dialog(activity).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setContentView(R.layout.fsd_ads_dialog_loading)
                    setCancelable(false)
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    window?.setLayout(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
            }
            if (!activity.isDestroyed && !activity.isFinishing && loadingDialog?.isShowing == false) {
                loadingDialog?.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissAdDialog() {
        loadingDialog?.takeIf { it.isShowing }?.dismiss()
    }

    // ── Private show ──────────────────────────────────────────────────────

    private fun showAdsFull(
        context: Activity,
        adUnitId: String,
        callback: (Long) -> Unit,
        needLoadAfterShow: Boolean = true
    ) {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                mInterstitialAd = null
                isShowing = false
                dismissAdDialog()
                callback(0L)
            }

            override fun onAdShowedFullScreenContent() {
                dismissAdDialog()
                isShowing = true
                AdLibConfig.notifyCount("interstitial")
                timeShowed = System.currentTimeMillis()
            }

            override fun onAdDismissedFullScreenContent() {
                isShowing = false
                mInterstitialAd = null
                val time = System.currentTimeMillis() - timeShowed
                startDelay()
                if (needLoadAfterShow) initInterAds(context, adUnitId) {}
                callback(time)
            }

            override fun onAdClicked() {
                AdLibConfig.notifyClick(
                    "interstitial",
                    mInterstitialAd?.responseInfo?.mediationAdapterClassName,
                    mInterstitialAd?.adUnitId,
                    AdLibConfig.getScreenName(context::class.java.simpleName)
                )
            }
        }
        mInterstitialAd?.show(context)
    }

    interface Callback {
        fun callback()
    }
}
