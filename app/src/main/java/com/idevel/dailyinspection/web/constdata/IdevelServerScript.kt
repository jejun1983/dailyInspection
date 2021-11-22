package com.idevel.dailyinspection.web.constdata

/**
 * The MusicServerScript
 * @company : medialog
 * @author  : jjbae
 */
enum class IdevelServerScript(val scriptName: String) {
    GET_BATTERY("getBattery"),
    SET_BATTERY("setBattery"),


    GET_GPS_INFO("getGpsInfo"),
    GET_PUSH_REG_ID("getPushRegId"),
    GET_APP_VERSION("getAppVersion"),

    GET_READT_ONESTORE_BILLING_INFO("readyOneStoreBilling"),
    GET_REQUEST_BUY_PRODUCT_INFO("requestBuyProduct"),

    GET_REQUEST_FILE_UPLOAD_INFO("requestFileUploadInfo"),


    SET_APP_STATUS("appStatus"),
    GET_AUTO_LOGIN("getAutoLogin"),
    GET_ACCOUNT("getAccount"),

    SET_DOWNLOAD_FILE("downloadFile"),

    SET_NFC("setNFC"),
    OPEN_QR("openQR"),

    SET_BEACON_ON_CHECK_OUT("setBeaconOnCheckOut"),
    SET_BEACON_ON_CHECK_IN("setBeaconOnCheckIn"),
    SET_BEACON_NEAR_BY("setBeaconNearBy"),
    SET_BEACON_STATE_CHANGE("setBeaconStateChange"),
    SET_BLE("setBle")
}