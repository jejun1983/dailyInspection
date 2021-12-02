package com.idevel.dailyinspection.web

import android.content.Context
import android.webkit.JavascriptInterface
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.idevel.dailyinspection.beacon.BeaconInterfaceData
import com.idevel.dailyinspection.utils.DLog
import com.idevel.dailyinspection.web.constdata.*
import com.idevel.dailyinspection.web.interfaces.IWebBridgeApi

/**
 * web 과의 연동을 위한 interface Class.
 *
 */
class MyWebInterface(private val mContext: Context, private val api: IWebBridgeApi) {
    companion object {
        const val webInvoker = "NativeInvoker"
        const val NAME = "idevel_app"
    }

    private fun gson(): Gson {
        return GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    }

    @JavascriptInterface
    fun removeSplash() {
        DLog.e("bjj data: call removeSplash ")

        api.removeSplash()
    }

    @JavascriptInterface
    fun getPushRegId() {
        DLog.e("bjj data: call getPushRegId ")

        api.getPushRegId()
    }

    @JavascriptInterface
    fun restartApp() {
        DLog.e("bjj data: call restartApp ")

        api.restartApp()
    }

    @JavascriptInterface
    fun finishApp() {
        DLog.e("bjj data: call finishApp ")

        api.finishApp()
    }

    @JavascriptInterface
    fun getAppVersion() {
        DLog.e("bjj data: call getAppVersion ")

        api.getAppVersion()
    }

    @JavascriptInterface
    fun requestCallPhone(jsonData: String) {
        val data = gson().fromJson(jsonData, RequestCallPhoneInfo::class.java)
        DLog.e("bjj data: call requestCallPhone " + data)

        api.requestCallPhone(data)
    }

    @JavascriptInterface
    fun requestExternalWeb(jsonData: String) {
        val data = gson().fromJson(jsonData, RequestExternalWebInfo::class.java)
        DLog.e("bjj data: call requestExternalWeb " + data)

        api.requestExternalWeb(data)
    }

    @JavascriptInterface
    fun openSharePopup(jsonData: String) {
        val data = gson().fromJson(jsonData, OpenSharePopupInfo::class.java)
        DLog.e("bjj data: call openSharePopup " + jsonData)

        api.openSharePopup(data.text)
    }

    @JavascriptInterface
    fun pageClearHistory() {
        DLog.e("bjj data: call pageClearHistory ")

        api.pageClearHistory()
    }

    @JavascriptInterface
    fun getGpsInfo() {
        DLog.e("bjj data: call getLocation ")

        api.getGpsInfo()
    }

    @JavascriptInterface
    fun readyOneStoreBilling() {
        DLog.e("bjj data: call readyOneStoreBilling ")

        api.readyOneStoreBilling()
    }

    @JavascriptInterface
    fun requestBuyProduct(jsonData: String) {
        val data = gson().fromJson(jsonData, RequestBuyProductInfo::class.java)
        DLog.e("bjj data: call requestBuyProduct " + jsonData)

        api.requestBuyProduct(data)
    }

    @JavascriptInterface
    fun openCamera(type: String, param: String) {
        DLog.e("bjj data: call openCamera " + type + " ^ " + param)

        api.openCamera(type, param)
    }

    @JavascriptInterface
    fun openGallery(type: String, param: String) {
        DLog.e("bjj data: call openGallery " + type + " ^ " + param)

        api.openGallery(type, param)
    }

    @JavascriptInterface
    fun setPushVibrate(isBool: Boolean) {
        DLog.e("bjj data: call setPushVibrate ")

        api.setPushVibrate(isBool)
    }

    @JavascriptInterface
    fun setPushBeep(isBool: Boolean) {
        DLog.e("bjj data: call setPushBeep ")

        api.setPushBeep(isBool)
    }


    @JavascriptInterface
    fun setAutoLogin(isAuto: Boolean) {
        DLog.e("bjj data: call setAutoLogin " + isAuto)

        api.setAutoLogin(isAuto)
    }

    @JavascriptInterface
    fun getAutoLogin() {
        DLog.e("bjj data: call getAutoLogin ")

        api.getAutoLogin()
    }

    @JavascriptInterface
    fun setAccount(id: String, pw: String) {
        DLog.e("bjj data: call setAccount " + id + " ^ " + pw)

        api.setAccount(id, pw)
    }

    @JavascriptInterface
    fun getAccount() {
        DLog.e("bjj data: call getAccount ")

        api.getAccount()
    }

    @JavascriptInterface
    fun downloadFile(fileURL: String, fileName: String) {
        DLog.e("bjj data: call downloadFile " + fileURL + " ^ " + fileName)

        api.downloadFile(fileURL, fileName)
    }

    @JavascriptInterface
    fun setQrFlash(isBool: Boolean) {
        DLog.e("bjj data: call setQrFlash " + isBool)

        api.setQrFlash(isBool)
    }

    @JavascriptInterface
    fun openQR() {
        DLog.e("bjj data: call openQR ")

        api.openQR()
    }

    @JavascriptInterface
    fun startNFC(isBool: Boolean) {
        DLog.e("bjj data: call startNFC " + isBool)

        api.startNFC(isBool)
    }

    @JavascriptInterface
    fun getBattery() {
        DLog.e("bjj data: call getBattery ")

        api.getBattery()
    }


    @JavascriptInterface
    fun startBeacon(jsonData: String) {
        val data = gson().fromJson(jsonData, BeaconMacInfo::class.java)
        DLog.e("bjj data: call startBeacon " + data)

        api.startBeacon(data)
    }
}