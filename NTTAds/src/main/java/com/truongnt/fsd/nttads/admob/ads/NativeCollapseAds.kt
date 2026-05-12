package com.truongnt.fsd.nttads.admob.ads

import android.app.Activity
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.truongnt.fsd.nttads.R

object NativeCollapseAds {

    private const val ALIAS = "native_collapse"

    /**
     * Preload native collapse ad.
     * @param adUnitId  app truyền vào getString(R.string.nt_clp) hoặc test id
     */
    fun loadAds(context: Activity, adUnitId: String) {
        NativeAds.preloadNativeAds(context, ALIAS, adUnitId) { /* preloaded */ }
    }

    /**
     * Show preloaded native collapse ad vào [container].
     * Tự reload sau khi show hoặc fail.
     *
     * @param adUnitId   cần truyền lại để reload sau khi show
     * @param layoutResId  mặc định R.layout.ads_native_large
     */
    fun showAds(
        context: AppCompatActivity,
        container: ViewGroup,
        adUnitId: String,
        layoutResId: Int = R.layout.fsd_ads_native_large
    ) {
        NativeAds.showPreloadNative(
            activity = context,
            alias = ALIAS,
            nativeView = container,
            onLoadDone = { loadAds(context, adUnitId) },
            onLoadFailed = {
                container.removeAllViews()
                container.isVisible = false
                loadAds(context, adUnitId)
            },
            layoutResId = layoutResId
        )
    }
}
