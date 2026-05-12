package com.truongnt.fsd.nttads.admob.ads

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.truongnt.fsd.nttads.R
import com.truongnt.fsd.nttads.admob.AdLibConfig

object NativeAds {

    // Preload map — key = alias do app đặt
    var preloadMap = mutableMapOf<String, MutableLiveData<NativeAdWrapper>>()

    class NativeAdWrapper(var nativeAd: NativeAd?) {
        /** -1 = error/premium, 0 = loading, 1 = loaded */
        var state: Int = -1
    }

    interface CallBackNativeAds {
        fun onLoaded()
        fun onError()
    }

    var mNativeAds: NativeAd? = null

    // ── Populate view ─────────────────────────────────────────────────────

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
        adView.mediaView?.mediaContent = nativeAd.mediaContent
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.tvActionBtnTitle)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)

        (adView.headlineView as? TextView)?.text = nativeAd.headline

        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as? TextView)?.text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as? Button)?.text = nativeAd.callToAction
        }

        nativeAd.icon?.let {
            (adView.iconView as? ImageView)?.setImageDrawable(it.drawable)
            adView.iconView?.visibility = View.VISIBLE
        } ?: run {
            adView.iconView?.visibility = View.GONE
        }

        adView.setNativeAd(nativeAd)

        nativeAd.mediaContent?.videoController?.let { vc ->
            if (vc.hasVideoContent()) {
                vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {}
            }
        }
    }

    // ── Load trực tiếp vào FrameLayout ────────────────────────────────────

    /**
     * @param adUnitId  app truyền vào — debug/release id do app quản lý
     * @param layoutResId  layout native ad, mặc định R.layout.ads_native_large
     */
    fun initNativeAds(
        activity: Activity,
        frameLayout: FrameLayout,
        callBackNativeAds: CallBackNativeAds,
        adUnitId: String,
        layoutResId: Int = R.layout.fsd_ads_native_large,
    ) {
        if (!AdLibConfig.isAdsEnabled() || !AdLibConfig.isNativeEnabled()) {
            callBackNativeAds.onLoaded(); return
        }
        if (AdLibConfig.isUserPremium()) {
            callBackNativeAds.onLoaded(); return
        }

        val adLoader = AdLoader.Builder(activity, adUnitId)
            .forNativeAd { nativeAd ->
                mNativeAds = nativeAd
                callBackNativeAds.onLoaded()

                if (activity.isDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                    nativeAd.destroy(); return@forNativeAd
                }

                nativeAd.setOnPaidEventListener { adValue ->
                    AdLibConfig.trackRevenue(adValue.valueMicros, adValue.currencyCode, "native")
                    AdLibConfig.notifyImpression(
                        "Native",
                        nativeAd.responseInfo?.mediationAdapterClassName,
                        nativeAd.responseInfo?.responseId,
                        AdLibConfig.getScreenName(activity::class.java.simpleName),
                        adValue.valueMicros / 1_000_000.0
                    )
                    AdLibConfig.notifyCount("native")
                }

                val cardView = activity.layoutInflater.inflate(layoutResId, null) as FrameLayout
                val adView = cardView.findViewById<NativeAdView>(R.id.uniform)
                populateNativeAdView(nativeAd, adView)
                frameLayout.removeAllViews()
                frameLayout.addView(cardView)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    callBackNativeAds.onError()
                }

                override fun onAdClicked() {
                    AdLibConfig.notifyClick(
                        "Native",
                        mNativeAds?.responseInfo?.mediationAdapterClassName,
                        mNativeAds?.responseInfo?.responseId,
                        AdLibConfig.getScreenName(activity::class.java.simpleName)
                    )
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    // ── Preload ───────────────────────────────────────────────────────────

    /**
     * Preload native ad gắn với [alias].
     * Gọi showPreloadNative() để hiển thị sau khi load xong.
     */
    fun preloadNativeAds(
        activity: Activity,
        alias: String,
        adUnitId: String,
        action: (NativeAd?) -> Unit
    ) {
        val mutableLiveData = MutableLiveData(NativeAdWrapper(null))
        preloadMap[alias] = mutableLiveData

        if (!AdLibConfig.isAdsEnabled()) return

        val wrapper = preloadMap[alias]?.value
        if (AdLibConfig.isUserPremium()) {
            wrapper?.state = -1
            preloadMap[alias]?.postValue(wrapper)
            return
        }

        wrapper?.state = 0
        preloadMap[alias]?.postValue(wrapper)

        val adLoader = AdLoader.Builder(activity, adUnitId)
            .forNativeAd { nativeAd ->
                mNativeAds = nativeAd
                nativeAd.setOnPaidEventListener { adValue ->
                    AdLibConfig.trackRevenue(adValue.valueMicros, adValue.currencyCode, "native")
                }
                wrapper?.apply {
                    this.nativeAd = nativeAd
                    this.state = 1
                }
                preloadMap[alias]?.postValue(wrapper)
                action(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    wrapper?.state = -1
                    preloadMap[alias]?.postValue(wrapper)
                    action(null)
                }

                override fun onAdClicked() {
                    AdLibConfig.notifyClick(
                        "Native",
                        mNativeAds?.responseInfo?.mediationAdapterClassName,
                        mNativeAds?.responseInfo?.responseId,
                        AdLibConfig.getScreenName(activity::class.java.simpleName)
                    )
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    // ── Show preloaded ────────────────────────────────────────────────────

    fun showPreloadNative(
        activity: FragmentActivity,
        alias: String,
        nativeView: ViewGroup,
        onLoadDone: (() -> Unit)? = null,
        onLoadFailed: (() -> Unit)? = null,
        layoutResId: Int = R.layout.fsd_ads_native_large,
    ): Boolean {
        if (AdLibConfig.isUserPremium()) {
            nativeView.visibility = View.GONE
            return false
        }

        mNativeAds?.setOnPaidEventListener { adValue ->
            AdLibConfig.notifyImpression(
                "Native",
                mNativeAds?.responseInfo?.mediationAdapterClassName,
                mNativeAds?.responseInfo?.responseId,
                AdLibConfig.getScreenName(activity::class.java.simpleName),
                adValue.valueMicros / 1_000_000.0
            )
        }

        val ob = preloadMap[alias] ?: return false

        ob.observe(activity) { wrapper ->
            try {
                if (activity.isDestroyed || activity.isFinishing) {
                    ob.removeObservers(activity); return@observe
                }
                wrapper?.let {
                    when (it.state) {
                        1 -> {
                            ob.removeObservers(activity)
                            it.nativeAd?.let { ads ->
                                val cardView = activity.layoutInflater
                                    .inflate(layoutResId, null) as FrameLayout
                                val adView = cardView.findViewById<NativeAdView>(R.id.uniform)
                                AdLibConfig.notifyCount("native")
                                populateNativeAdView(ads, adView)
                                nativeView.removeAllViews()
                                nativeView.addView(cardView)
                                onLoadDone?.invoke()
                            }
                            if (it.nativeAd == null) onLoadFailed?.invoke()
                        }
                        -1 -> {
                            ob.removeObservers(activity)
                            onLoadFailed?.invoke()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return true
    }
}
