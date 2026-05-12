package com.truongnt.fsd.nttads.admob

/**
 * Cấu hình cho thư viện Ads.
 *
 * @param bannerId          Ad Unit ID cho Banner
 * @param interstitialId    Ad Unit ID cho Interstitial
 * @param rewardedId        Ad Unit ID cho Rewarded
 * @param isTestMode        Nếu true, dùng Test ID của Google thay vì ID thật
 */
data class AdsConfig(
    val bannerId: String,
    val interstitialId: String,
    val rewardedId: String,
    val isTestMode: Boolean = false
)
