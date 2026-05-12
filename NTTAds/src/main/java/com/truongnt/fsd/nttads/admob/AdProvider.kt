package com.truongnt.fsd.nttads.admob

import android.app.Activity
import android.content.Context

/**
 * Interface chuẩn hoá mọi mạng quảng cáo.
 * Implement interface này để thêm bất kỳ Ad Network nào (AdMob, Facebook, Unity...).
 */
interface AdProvider {

    /**
     * Khởi tạo Ad Network SDK.
     * Gọi một lần duy nhất trong Application.onCreate().
     */
    fun initialize(context: Context, config: AdsConfig)

    /**
     * Load và hiển thị Banner vào [containerId].
     * Banner tự load lại khi refresh, không cần gọi lại.
     */
    fun loadBanner(activity: Activity, containerId: Int, callback: AdsCallback)

    /**
     * Load Interstitial vào bộ nhớ.
     * Callback [AdsCallback.onAdLoaded] khi sẵn sàng.
     */
    fun loadInterstitial(context: Context, callback: AdsCallback)

    /**
     * Hiển thị Interstitial đã load.
     * Phải gọi [loadInterstitial] trước.
     */
    fun showInterstitial(activity: Activity, callback: AdsCallback)

    /**
     * Load Rewarded vào bộ nhớ.
     * Callback [AdsCallback.onAdLoaded] khi sẵn sàng.
     */
    fun loadRewarded(context: Context, callback: AdsCallback)

    /**
     * Hiển thị Rewarded đã load.
     * Phải gọi [loadRewarded] trước.
     */
    fun showRewarded(activity: Activity, callback: AdsCallback)

    /**
     * Giải phóng tài nguyên. Gọi trong onDestroy() của Activity gốc.
     */
    fun destroy()
}
