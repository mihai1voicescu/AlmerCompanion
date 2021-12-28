package io.almer.companionshared.server

sealed class DeviceScanViewState {
    object ActiveScan : DeviceScanViewState()
    object Done : DeviceScanViewState()
    class Error(val message: String) : DeviceScanViewState()
    object AdvertisementNotSupported : DeviceScanViewState()
}