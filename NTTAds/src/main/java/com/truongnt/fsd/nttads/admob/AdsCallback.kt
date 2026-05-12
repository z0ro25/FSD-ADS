package com.truongnt.fsd.nttads.admob

/**
 * Callback nhận event từ quảng cáo.
 * Tất cả hàm đều có default implementation rỗng,
 * chỉ override những gì bạn cần.
 */
interface AdsCallback {
    /** Quảng cáo đã load xong, sẵn sàng hiển thị */
    fun onAdLoaded() {}

    /** Load quảng cáo thất bại */
    fun onAdFailed(error: String) {}

    /** User đóng quảng cáo */
    fun onAdClosed() {}

    /** User hoàn thành xem Rewarded, nhận phần thưởng */
    fun onRewarded(type: String, amount: Int) {}

    /** Quảng cáo bắt đầu hiển thị */
    fun onAdShown() {}

    /** User nhấn vào quảng cáo */
    fun onAdClicked() {}
}
