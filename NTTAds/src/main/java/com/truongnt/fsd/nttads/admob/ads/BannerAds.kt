package com.truongnt.fsd.nttads.admob.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.truongnt.fsd.nttads.R
import com.truongnt.fsd.nttads.admob.AdLibConfig

@SuppressLint("StaticFieldLeak")
object BannerAds {

    fun getAdSize(activity: Activity): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getMetrics(outMetrics)
        val adWidth = (outMetrics.widthPixels / outMetrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    /**
     * Load banner vào [R.id.adView_container] bên trong [R.id.banner_ad] của Activity.
     *
     * @param adUnitId  truyền từ app — debug dùng test id, release dùng real id
     * @param collapsible  true = dùng collapsible banner (bottom)
     */
    fun initBannerAds(
        ctx: Activity,
        adUnitId: String,
        collapsible: Boolean = false
    ) {
        try {
            val adBanner: ViewGroup? = ctx.findViewById(R.id.banner_ad)

            if (!AdLibConfig.isAdsEnabled() || !AdLibConfig.isBannerEnabled()) {
                adBanner?.visibility = View.GONE
                hideBannerLoading(ctx, false)
                return
            }

            if (AdLibConfig.isUserPremium()) {
                adBanner?.visibility = View.GONE
                return
            }

            val adViewContainer: LinearLayout = ctx.findViewById(R.id.adView_container) ?: return
            val mAdViewBanner = AdView(ctx).apply {
                setAdSize(getAdSize(ctx))
                this.adUnitId = adUnitId
            }

            val adRequest = if (collapsible) {
                val extras = Bundle().apply { putString("collapsible", "bottom") }
                AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
            } else {
                AdRequest.Builder().build()
            }

            adViewContainer.removeAllViews()
            adViewContainer.addView(mAdViewBanner)
            mAdViewBanner.loadAd(adRequest)

            mAdViewBanner.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    mAdViewBanner.setOnPaidEventListener { adValue ->
                        AdLibConfig.trackRevenue(adValue.valueMicros, adValue.currencyCode, "banner")
                        AdLibConfig.notifyImpression(
                            "Banner",
                            mAdViewBanner.responseInfo?.mediationAdapterClassName,
                            mAdViewBanner.adUnitId,
                            AdLibConfig.getScreenName(ctx::class.java.simpleName),
                            adValue.valueMicros / 1_000_000.0
                        )
                        AdLibConfig.notifyCount("banner")
                    }
                    adViewContainer.visibility = View.VISIBLE
                    hideBannerLoading(ctx, false)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    adViewContainer.visibility = View.GONE
                    hideBannerLoading(ctx, true)
                }

                override fun onAdClicked() {
                    AdLibConfig.notifyClick(
                        "Banner",
                        mAdViewBanner.responseInfo?.mediationAdapterClassName,
                        mAdViewBanner.adUnitId,
                        AdLibConfig.getScreenName(ctx::class.java.simpleName)
                    )
                }
            }

            hideBannerLoading(ctx, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Overload dùng ViewGroup trực tiếp thay vì tìm từ Activity.
     */
    fun initBannerAds(
        ctx: Activity,
        adBanner: ViewGroup?,
        adUnitId: String,
        collapsible: Boolean = false
    ) {
        try {
            if (AdLibConfig.isUserPremium()) { adBanner?.visibility = View.GONE; return }
            if (adBanner == null) return

            val adViewContainer: LinearLayout = adBanner.findViewById(R.id.adView_container) ?: return
            val mAdViewBanner = AdView(ctx).apply {
                setAdSize(getAdSize(ctx))
                this.adUnitId = adUnitId
            }

            val adRequest = if (collapsible) {
                val extras = Bundle().apply { putString("collapsible", "bottom") }
                AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
            } else {
                AdRequest.Builder().build()
            }

            adViewContainer.removeAllViews()
            adViewContainer.addView(mAdViewBanner)
            mAdViewBanner.loadAd(adRequest)

            mAdViewBanner.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    mAdViewBanner.setOnPaidEventListener { adValue ->
                        AdLibConfig.trackRevenue(adValue.valueMicros, adValue.currencyCode, "banner")
                    }
                    adViewContainer.visibility = View.VISIBLE
                    hideBannerLoading(adBanner, false)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    adViewContainer.visibility = View.GONE
                    hideBannerLoading(adBanner, true)
                }

                override fun onAdClicked() {
                    hideBannerLoading(adBanner, true)
                }
            }

            hideBannerLoading(adBanner, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideBannerLoading(bannerAd: ViewGroup, hide: Boolean) {
        try {
            val shimmer: ShimmerFrameLayout = bannerAd.findViewById(R.id.shimmer_layout)
            val vis = if (hide) View.GONE else View.VISIBLE
            shimmer.visibility = vis
            bannerAd.findViewById<View>(R.id.view_d).visibility = vis
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideBannerLoading(ctx: Activity, hide: Boolean) {
        try {
            val shimmer: ShimmerFrameLayout = ctx.findViewById(R.id.shimmer_layout)
            val vis = if (hide) View.GONE else View.VISIBLE
            shimmer.visibility = vis
            ctx.findViewById<View>(R.id.view_d).visibility = vis
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
