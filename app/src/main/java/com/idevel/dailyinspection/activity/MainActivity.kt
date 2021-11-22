package com.idevel.dailyinspection.activity


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.Window
import android.view.WindowManager
import android.webkit.*
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import api.OnResultListener
import com.idevel.dailyinspection.BuildConfig
import com.idevel.dailyinspection.MyApplication
import com.idevel.dailyinspection.R
import com.idevel.dailyinspection.beacon.ble.GasBleData
import com.idevel.dailyinspection.broadcast.DataSaverChangeReceiver
import com.idevel.dailyinspection.broadcast.NetworkChangeReceiver
import com.idevel.dailyinspection.dialog.AgentPopupDialog
import com.idevel.dailyinspection.dialog.CustomAlertDialog
import com.idevel.dailyinspection.fcm.PushPreferences.IS_NOTI
import com.idevel.dailyinspection.fcm.PushPreferences.PUSH_DATA_LINK_TYPE
import com.idevel.dailyinspection.fcm.PushPreferences.PUSH_DATA_LINK_URL
import com.idevel.dailyinspection.fcm.PushPreferences.PUSH_DATA_SHOWTIME
import com.idevel.dailyinspection.interfaces.IDataSaverListener
import com.idevel.dailyinspection.interfaces.NetworkChangeListener
import com.idevel.dailyinspection.utils.*
import com.idevel.dailyinspection.utils.wrapper.LocaleWrapper
import com.idevel.dailyinspection.web.BaseWebView
import com.idevel.dailyinspection.web.MyWebChromeClient
import com.idevel.dailyinspection.web.MyWebViewClient
import com.idevel.dailyinspection.web.UrlData.*
import com.idevel.dailyinspection.web.constdata.*
import com.idevel.dailyinspection.web.interfaces.IWebBridgeApi
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.limei.indoorcommon.consts.LMEnum
import com.limei.indoorcommon.model.beaconMgr.LMBeacon
import com.limei.indoorcommon.model.configMgr.LMConfig
import com.limei.positioningengine.ICheckInListener
import com.limei.positioningengine.IPositioningEngineStub
import com.limei.positioningengine.IPositioningInfoListener
import com.limei.positioningengine.PositioningEngine
import kr.co.medialog.ApiManager
import kr.co.medialog.SettingInfoData
import kr.co.medialog.UploadInfoData
import okhttp3.ResponseBody
import java.io.*
import java.lang.Exception
import java.lang.ref.WeakReference
import java.net.URISyntaxException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess


/**
 * main activity class.
 *
 * @author : jjbae
 */
class MainActivity : FragmentActivity(), ICheckInListener, IPositioningInfoListener {
    private var mAgentPopupDialog: AgentPopupDialog? = null
    private var mSplashView: View? = null //초기 로딩시 보여주기 위한 view.
    private var mErrorView: View? = null //The network error view.
    private var mWebview: BaseWebView? = null //mobile view page 연결을 위한 webview.
    private var mWebViewClient: MyWebViewClient? = null //The web view client.
    private var mWebChromeClient: MyWebChromeClient? = null //The web chrome client.
    private var isRestartApp = false //The is restart app.
    private var isMain = false

    private var mWebviewSub: RelativeLayout? = null // sub webView parent
    private var mSettingdata: SettingInfoData? = null

    private val mHandler = WeakHandler(this) //UI handler

    private var mReTry: Int = 0
    private var mLocationManager: LocationManager? = null

    private var mApiManager: ApiManager? = null
    private val mNetworkChangeReceiver = NetworkChangeReceiver() //네트워크 check
    private val mDataSaverChangeReceiver = DataSaverChangeReceiver()
    var isIntoNotiLandingUrl: Boolean = false

    //camera & gallery
    private var mCameraReturnParam: String? = null
    private var mCameraReturnType: String? = null
    private var camera_test_btn: Button? = null
    private var gallery_test_btn: Button? = null

    private var nfcAdapter: NfcAdapter? = null
    private var isQrFlash: Boolean = false
    private var isNFCenable: Boolean = false

    private var m_positioningEngine: IPositioningEngineStub? = null

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //캡쳐 방지
//        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        mApiManager = ApiManager.getInstance(this@MainActivity)
        CookieSyncManager.createInstance(this@MainActivity)

        mWebviewSub = findViewById(R.id.webview_sub)
        mWebview = findViewById(R.id.webview_main)
        mSplashView = findViewById(R.id.view_splash)
        mSplashView?.visibility = View.VISIBLE
        mErrorView = findViewById(R.id.view_error)
        mErrorView?.visibility = View.GONE

        cleanCookie()

        //네트워크 연결 여부
        if (!isNetworkConnected(this)) {
            showErrorDlg(NETWORK_CONNECTION_ERROR)
            return
        }

        // 앱 사용 중 data saver 사용 Listener
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mDataSaverChangeReceiver.setListener(dataSaverListener)
        }

        // 앱 사용 중 data network 변경 Listener
        mNetworkChangeReceiver.setListener(networkListener)

        checkSettingInfo()

        //Initialise NFC
        val nfcManager = getSystemService(Context.NFC_SERVICE) as NfcManager
        nfcAdapter = nfcManager.defaultAdapter

        if (nfcAdapter == null) {
            Toast.makeText(this, "NO NFC Capabilities", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToDevActivity() {
        val i = Intent(this, DevActivity::class.java)
        startActivityForResult(i, DEV_REQUEST_CODE)
    }

    // push 클릭
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val isNoti = intent.getIntExtra(IS_NOTI, -1)
        DLog.e("bjj onNewIntent :: " + isNoti + " ^ " + intent.getAction())

        if (isNoti == 1) {
            checkPushData(intent)
        } else if (isNoti == 0) {
            val linkType = intent.getStringExtra(PUSH_DATA_LINK_TYPE)
            val link = intent.getStringExtra(PUSH_DATA_LINK_URL)

            if (!linkType.isNullOrEmpty()) {
                if (linkType.contains("_webview")) {
                    isIntoNotiLandingUrl = true

                    if (mWebview != null) {
                        mWebview!!.loadUrl(link!!)
                    }
                }
            }
        } else {
            // NFC
            if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.getAction() ||
                    NfcAdapter.ACTION_TECH_DISCOVERED == intent.getAction() ||
                    NfcAdapter.ACTION_NDEF_DISCOVERED == intent.getAction()) {
                showNFC(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(mDataSaverChangeReceiver, IntentFilter(ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED))
        }

        registerReceiver(mNetworkChangeReceiver, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))

        mWebview?.sendEvent(IdevelServerScript.SET_APP_STATUS, AppStatusInfo("onResume").toJsonString())
        mWebview?.onResume()

        try {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

            nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        } catch (ex: IllegalStateException) {
            DLog.e("bjj Error enabling NFC foreground dispatch" + ex)
        }

        //battery
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onPause() {
        super.onPause()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            unregisterReceiver(mDataSaverChangeReceiver)
        }

        unregisterReceiver(batteryReceiver)
        unregisterReceiver(mNetworkChangeReceiver)

        mWebview?.sendEvent(IdevelServerScript.SET_APP_STATUS, AppStatusInfo("onPause").toJsonString())
        mWebview?.onPause()

        nfcAdapter?.disableForegroundDispatch(this)

        try {
            nfcAdapter?.disableForegroundDispatch(this)
        } catch (ex: IllegalStateException) {
            DLog.e("bjj Error disabling NFC foreground dispatch" + ex)
        }
    }

    private fun checkSettingInfo() {
        //임시
        mHandler.sendEmptyMessageDelayed(HANDLER_SPLASH, 2000L)


//        var storeInfoStr = if (MyApplication.instance.isGoogleMarket) {
//            "?store=google"
//        } else {
//            "?store=onestore"
//        }
//
//        val url = URL(getSettingUrl(this@MainActivity) + storeInfoStr)
//        mApiManager?.getSettingInfo(url.toString(), object : OnResultListener<Any> {
//            override fun onResult(result: Any, flag: Int) {
//                if (result == null) {
//                    return
//                }
//
//                mSettingdata = result as SettingInfoData
//
//                DLog.e("bjj checkSettingInfo :: "
//                        + mSettingdata?.main_url + " ^ "
//                        + mSettingdata?.os + " ^ "
//                        + mSettingdata?.version)
//
//                NORMAL_SERVER_URL = mSettingdata?.main_url?.replace("\\\\", "")
//
//                if (isIntoNotiLandingUrl) {
//                    val link = intent.getStringExtra(PUSH_DATA_LINK_URL)
//
//                    NORMAL_SERVER_URL = if (link.isNullOrEmpty()) {
//                        mSettingdata?.main_url?.replace("\\\\", "")
//                    } else {
//                        link
//                    }
//                }
//
//                if (mSettingdata?.version.isNullOrEmpty()) {
//                    appVersionCal("", true)
//                } else {
//                    appVersionCal(mSettingdata?.version ?: "")
//                }
//            }
//
//            override fun onFail(error: Any, flag: Int) {
//                appVersionCal("", true)
//            }
//        })
    }

    private fun appVersionCal(version: String, isError: Boolean = false) {
        if (isError) {
            showAlertDlg(0, R.string.popup_msg_error_version, APP_VERSION_CHECK)
        } else {
            mReTry = 0

            val serverVersion = version.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val appVersion = getVersionName(this)!!.split("\\.".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()

            DLog.d("bjj appVersionCal "
                    + appVersion?.size + " ^ "
                    + serverVersion[0] + " ^ "
                    + serverVersion[1] + " ^ "
                    + serverVersion[2] + " ^ "
                    + appVersion[0]!! + " ^ "
                    + appVersion[1]!! + " ^ "
                    + appVersion[2]!!)

            if (Integer.parseInt(appVersion[0] + appVersion[1]) < Integer.parseInt(serverVersion[0] + serverVersion[1])) {
                //MAJOR
                DLog.d("bjj appVersionCal MAJOR")
                showOtherAppVersionDlg()

            } else if (Integer.parseInt(appVersion[2]) < Integer.parseInt(serverVersion[2]) &&
                    Integer.parseInt(appVersion[0] + appVersion[1]) == Integer.parseInt(serverVersion[0] + serverVersion[1])) {
                //MINOR
                DLog.d("bjj appVersionCal MINOR")
                showOtherAppVersionDlg()
            } else {
                DLog.d("bjj appVersionCal NOTTING")
                mHandler.sendEmptyMessage(HANDLER_SPLASH)
            }
        }
    }

    /**
     * 스플래시 애니메이션 설정
     */
    private fun setSplash() {
        mHandler.sendEmptyMessageDelayed(HANDLER_SPLASH_DELAY, 300L)
    }

    /**
     * Sets the main view.
     */
    private fun setMainView() {
        if (mWebview == null) {
            return
        }

        mHandler.sendEmptyMessageDelayed(HANDLER_NETWORK_TIMER, PING_TIME.toLong())

        //모든 파일 접근 가능 권한
        if (!isCanAllFileAcess()) {
            gotoAllFileAcessPermission()
        }

        mWebview?.setBackgroundColor(Color.WHITE)
        mWebview?.setJSInterface(iWebBridgeApi)
        mWebview?.loadUrl(NORMAL_SERVER_URL)

        mWebViewClient = object : MyWebViewClient(this) {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                DLog.e("bjj mWebViewClient onPageStarted : $url")
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                if (url == "https://lgdc.wtest.biz/login/checkForm.php") {
                    removeSplash()
                }

                DLog.e("bjj mWebViewClient onPageFinished : $url, ${mSettingdata?.main_url}")
            }

            override fun showErrorPage() {
                DLog.e("bjj mWebViewClient showErrorPage : ")
                showErrorView()
            }

            override fun setUntouchableProgress(visible: Int) {
                DLog.e("bjj mWebViewClient setUntouchableProgress : $visible")
            }

            @SuppressWarnings("deprecation")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return urlLoading(view, Uri.parse(url))
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    urlLoading(view, request?.getUrl())
                } else {
                    super.shouldOverrideUrlLoading(view, request)
                }
            }
        }

        mWebChromeClient = object : MyWebChromeClient(this, findViewById<View>(R.id.mainview) as RelativeLayout) {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (View.GONE == mSplashView?.visibility) {
                    super.onProgressChanged(view, newProgress)
                }
            }

            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                return super.onJsAlert(view, url, message, result)
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                return super.onConsoleMessage(consoleMessage)
            }
        }

        mWebview?.webViewClient = mWebViewClient!!
        mWebview?.webChromeClient = mWebChromeClient!!
    }

    private fun checkPushData(intent: Intent) {
        // 푸시를 통해 진입한 경우가 아님

        DLog.e("bjj checkPushData :: " + MyApplication.instance.isChating + " ^ " + intent.extras)

        if (null == intent.extras) {
            return
        }

        if (mAgentPopupDialog != null) {
            mAgentPopupDialog!!.dismiss()
            mAgentPopupDialog = null
        }

        // 8.0미만 버전에서 뱃지 카운트 0 으로 설정
        setIconBadge(this, 0)

        val linkUrl = intent.getStringExtra(PUSH_DATA_LINK_URL)
        mAgentPopupDialog = AgentPopupDialog(this, intent)

        mAgentPopupDialog!!.setOkClickListener(OnClickListener {
            mAgentPopupDialog!!.dismiss()

            if (linkUrl == null || linkUrl.equals("", ignoreCase = true)) {
                return@OnClickListener
            } else {
                if (mWebview != null) {
                    mWebview!!.loadUrl(linkUrl)
                }
            }
        })

        mAgentPopupDialog!!.show()

        val showTime = intent.getStringExtra(PUSH_DATA_SHOWTIME)

        if (!showTime.isNullOrEmpty()) {
            Handler().postDelayed({
                if (mAgentPopupDialog!!.isShowing) {
                    mAgentPopupDialog!!.setDisappearClose()
                }
            }, showTime.toLong())
        }
    }

    /**
     * Show error view.
     */
    fun showErrorView() {
        mErrorView?.visibility = View.VISIBLE

        val homeBtn = mErrorView?.findViewById<Button>(R.id.homeBtn)
        homeBtn?.setOnClickListener { finish() }
    }

    /**
     * Show main view.
     */
    private fun showMainView(isNotiLandingFinissh: Boolean = false) {
        mHandler.removeMessages(HANDLER_NETWORK_TIMER)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        mSplashView?.visibility = View.GONE
        mErrorView?.visibility = View.GONE

        val isNoti = intent.getIntExtra(IS_NOTI, -1)
        DLog.e("bjj showMainView " + isNotiLandingFinissh + " ^ " + isNoti + " ^ " + mWebview)

        if (!isNotiLandingFinissh && isNoti == 0) {
            val linkType = intent.getStringExtra(PUSH_DATA_LINK_TYPE)
            val link = intent.getStringExtra(PUSH_DATA_LINK_URL)

            if (!linkType.isNullOrEmpty()) {
                if (linkType.contains("_webview")) {
                    isIntoNotiLandingUrl = true

                    if (mWebview != null) {
                        mWebview!!.loadUrl(link!!)
                    }
                }
            }
        }

        //TODO camera & gallery
        camera_test_btn = findViewById(R.id.camera_test_btn) // QR
        camera_test_btn?.setOnClickListener {
//            doTakePhotoAction("profile", "")

            val intent = Intent(this@MainActivity, QrcodeScanActivity::class.java)
            intent.putExtra(QrcodeScanActivity.IS_FLASH, isQrFlash)

            startActivityForResult(intent, REQUEST_QRSCAN_ACTIVITY)
        }
        gallery_test_btn = findViewById(R.id.gallery_test_btn) // NFC
        gallery_test_btn?.setOnClickListener {
//            doTakeAlbumAction("profile", "")

            isNFCenable = true
        }

        if (BuildConfig.DEBUG) {
//            billing_subscribe_test_btn?.visibility = View.VISIBLE
//            billing_single_test_btn?.visibility = View.VISIBLE
//
//            camera_test_btn?.visibility = View.VISIBLE
//            gallery_test_btn?.visibility = View.VISIBLE
        }

        // 비콘 초기화
        initBeacon()
        initGasBle()
    }

    private fun showAppFinishPopup() {
        val alertDialog = CustomAlertDialog(this)
        alertDialog.setCancelable(true)
        alertDialog.setDataSaveLayout(0, R.string.popup_app_finish_message)
        alertDialog.setButtonString(R.string.popup_app_finish_ok, R.string.popup_app_finish_cancel)

        alertDialog.setOkClickListener(OnClickListener { v ->
            when (v.id) {
                R.id.btn_ok -> {
                    alertDialog.dismiss()

                    finish()
                }
                R.id.btn_cancel -> {
                    alertDialog.dismiss()
                }
            }
        })

        if (!isFinishing) {
            alertDialog.show()
        }
    }

    /**
     * Show other app version dlg.
     */
    private fun showOtherAppVersionDlg() {
        val alertDialog = CustomAlertDialog(this)
        alertDialog?.setOkClickListener(OnClickListener { v ->
            alertDialog?.dismiss()
            when (v.id) {
                R.id.btn_ok -> gotoPlayStore()
                R.id.btn_cancel -> finish()
            }
        })

        if (!isFinishing && !isDestroyed) {
            alertDialog?.show()
        }
    }

    private fun showDataSaveDlg(title: Int, content: Int) {
        val alertDialog = CustomAlertDialog(this)
        alertDialog?.setCancelable(false)
        alertDialog?.setDataSaveLayout(title, content)
        alertDialog?.setButtonString(R.string.popup_btn_ok_dta_save, R.string.popup_btn_cancel_dta_save)

        alertDialog?.setOkClickListener(OnClickListener { v ->
            alertDialog!!.dismiss()
            when (v.id) {
                R.id.btn_cancel -> finish()
                R.id.btn_ok -> {
                    val intent = Intent()
                    intent.action = Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri

                    startActivity(intent)
                    finish()
                }
            }
        })

        if (!isFinishing) {
            alertDialog?.show()
        }
    }

    /**
     * Show alert dlg.
     */
    private fun showAlertDlg(title: Int, content: Int, errorType: Int) {
        val alertDialog = CustomAlertDialog(this)
        alertDialog?.setErrorLayout(title, content)

        alertDialog?.setOkClickListener(OnClickListener { v ->
            alertDialog?.dismiss()

            if (v.id == R.id.btn_error) {
                when (errorType) {
                    NETWORK_CONNECTION_ERROR, TIMEOUT_ERROR -> {
                        finish()
                    }
                    APP_VERSION_CHECK -> {
                        if (mReTry > 2) {
                            finish()
                        } else {
                            mReTry++
                            checkSettingInfo()
                        }
                    }
                    else -> {
                    }
                }
            }
        })

        if (!isFinishing) {
            alertDialog?.show()
        }
    }

    /**
     * Show PermissionDenyDialog dlg.
     */
    private fun showPermissionDenyDialog(permissionStr: String) {
        val alertDialog = CustomAlertDialog(this)
        alertDialog.setCancelable(false)

//        if (permissionStr.contains("READ_PHONE_NUMBERS")) {
//            alertDialog.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg1)
//        } else if (permissionStr.contains("READ_PHONE_STATE")) {
//            alertDialog.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg1)
//        } else if (permissionStr.contains("MANAGE_EXTERNAL_STORAGE")) {
//            alertDialog.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg5)
//        } else if (permissionStr.contains("READ_EXTERNAL_STORAGE")) {
//            alertDialog.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg2)
//        } else if (permissionStr.contains("WRITE_EXTERNAL_STORAGE")) {
//            alertDialog.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg2)
//        } else if (permissionStr.contains("READ_CALL_LOG")) {
//            alertDialog.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg3)
//        } else {
        alertDialog.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg4)
//        }

        alertDialog.setButtonString(R.string.popup_btn_ok_dta_save, R.string.popup_btn_cancel_dta_save)

        alertDialog.setOkClickListener(OnClickListener { v ->
            alertDialog.dismiss()
            when (v.id) {
                R.id.btn_cancel -> finish()
                R.id.btn_ok -> {
                    if (permissionStr.contains("MANAGE_EXTERNAL_STORAGE")) {
                        gotoAllFileAcessPermission()
                    } else {
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    }

                    finish()
                }
            }
        })

        if (!isFinishing) {
            alertDialog.show()
        }
    }

    private fun gotoAllFileAcessPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri

        ContextCompat.startActivity(this@MainActivity, intent, ActivityOptionsCompat.makeBasic().toBundle())
    }

    /**
     * Show network error dlg.
     *
     * @param errorType the error type
     */
    private fun showErrorDlg(errorType: Int) {
        var titleRes = R.string.popup_title_server_error
        var msgRes = R.string.popup_msg_server_error

        when (errorType) {
            NETWORK_CONNECTION_ERROR -> {
                titleRes = R.string.popup_title_network_error
                msgRes = R.string.popup_msg_network_error
            }
            TIMEOUT_ERROR -> {
                titleRes = R.string.popup_title_server_error
                msgRes = R.string.popup_msg_server_error
            }
        }

        showAlertDlg(titleRes, msgRes, errorType)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            DLog.e("bjj onKeyDown ==>> " + isMain + " ^ " + mWebview!!.canGoBack())

            mWebview?.let {
                if (it.canGoBack()) {
                    if (isMain) {
                        showAppFinishPopup()
                    } else {
                        it.goBack()
                    }
                } else {
                    showAppFinishPopup()
                }

                return false
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun webviewDestroy(webview: BaseWebView) {
        webview?.let {
            it.stopLoading()
            it.removeAllViews()
            it.clearHistory()
            it.clearCache(true)
            it.destroy()
        }
    }

    /**
     * Goto play store.
     */
    private fun gotoPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW)

        if (MyApplication.instance.isGoogleMarket) {
            intent.data = Uri.parse("market://details?id=$packageName")
        } else {
            intent.data = Uri.parse("https://onesto.re/0000748084")
        }

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        mWebview?.sendEvent(IdevelServerScript.SET_APP_STATUS, AppStatusInfo("onDestroy").toJsonString())

        cleanCookie()

        mWebview?.let {
            webviewDestroy(it)
        }

        super.onDestroy()
    }

    /**
     * Restart app.
     */
    fun restartApp() {
        cleanCookie()
        isRestartApp = true

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        startActivity(mainIntent)
        exitProcess(0)
    }

    /**
     * Clean cookie.
     */
    private fun cleanCookie() {
        DLog.e("isRestartApp ==>> $isRestartApp")

        if (!isRestartApp) {
            this@MainActivity.runOnUiThread {
                mWebview?.clearCache(true)
                mWebview?.clearHistory()

                val cookieSyncMngr = CookieSyncManager.createInstance(this@MainActivity)
                cookieSyncMngr.startSync()
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookie()
                cookieManager.removeSessionCookie()
                cookieSyncMngr.stopSync()
            }
        }
    }

    public override fun onStart() {
        super.onStart()
//        FirebaseUserActions.getInstance().start(firebaseAction)
    }

    public override fun onStop() {
        super.onStop()
//        FirebaseUserActions.getInstance().end(firebaseAction)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        // google inapp result
//        if (MyApplication.instance.isGoogleMarket) {
//            val isGoogleBpResult = googleBp?.handleActivityResult(requestCode, resultCode, intent)
//            val googleBpResultCode = intent?.extras?.get("RESPONSE_CODE")
//            val googleBpResultData = intent?.extras?.get("INAPP_PURCHASE_DATA")
//
//            DLog.e("bjj BILLING onActivityResult "
//                    + isGoogleBpResult + " ^ "
//                    + intent + " ^ "
//                    + intent?.extras + " ^ "
//                    + googleBpResultCode + " ^ "
//                    + requestCode + " ^ "
//                    + resultCode)
//
////            if(googleBpResultCode == Constants.BILLING_RESPONSE_RESULT_OK){
//            //TODO 이걸로 보낼지
////                Bundle[{INAPP_PURCHASE_DATA={"orderId":"GPA.3353-4786-9413-58565","packageName":"com.idevel.dailyinspection","productId":"single_month","purchaseTime":1597240260045,"purchaseState":0,"developerPayload":"inapp:single_month:0075b5a1-6a7a-417c-ac00-6c993441b85c","purchaseToken":"dgkbeeoiclkjpdoafoogehim.AO-J1OzUdS_d7A_lyCiKHUC825T4DDQu86PdwB0R0tRgrrv28mBL4YRbY1v_hoXsWW8gNt-34QRjgIPLyBvXeEoPf4FbDwytbUTjd0hSzOMw9pEIhU3NO6XQhC3s8F_L81UwzFOOnM4x"}, INAPP_DATA_SIGNATURE=CITI8oTFUphcpTels23QpNxcWIZzdBULJK5VZwtIta9dL46mOlTTfMQeYM13Wk5NUdhbxQ3D1vPsYxEed+nEb30jkDqQyX7UMnFhIiTp6T8VI1PIsuQzhoIyWoCfh0Q3eP7W+yYjw8VluD2ebTo69T6x4LqO6y3lcQxlkMrKL9pRbG6m7NgqoBRrREBm8H9fPexplvmc//AEmQUmGIpoGoE8MRMoxEPkC0+3l+HhFk3qktZX1pzb4vufUtHjpCevygI2qQfil0Nt1gtccE3/6p8bUu5MDf1vNct+qvsQ5X1QkF02a9d+xuDmnIxXg+CFqu5GIBA3p/iI2Np2s1knrw==, RESPONSE_CODE=0}]
////                mWebview?.sendEvent(tatalkServerScript.GET_REQUEST_BUY_PRODUCT_INFO, ReturnRequestBuyProductInfo(true, googleBpResultData.toString()).toJsonString())
////            }
//
//            if (isGoogleBpResult == true) {
//                return
//            }
//        }

        super.onActivityResult(requestCode, resultCode, intent)

        when (requestCode) {
            PICK_FROM_ALBUM -> {
                if (resultCode == Activity.RESULT_OK) {

                    if (intent?.data != null) { //1개 이미지
                        DLog.e("bjj PICK_FROM_ALBUM AA " + intent.data)

                        sendGalleryImage(intent.data!!)
                    } else {
                        val clipData: ClipData = intent?.clipData!!
                        if (clipData.itemCount > 3) {
                            Toast.makeText(this, "3장이상 업로드 할 수 없습니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            if (clipData.itemCount > 0) {
                                for (index in 0 until clipData.itemCount) {
                                    DLog.e("bjj PICK_FROM_ALBUM BB " + index + " ^ " + clipData.getItemAt(index).uri)

                                    sendGalleryImage(clipData.getItemAt(index).uri, index, clipData.itemCount)
                                }
                            }
                        }
                    }
                }
            }

            PICK_FROM_CAMERA -> {
                if (resultCode == Activity.RESULT_OK) {
//                    if (intent?.extras?.get("data") != null) {
//                        if (intent?.extras?.get("data") is Bitmap) {
//                            sendCameraImage(intent?.extras?.get("data") as Bitmap)
//                        }
//                    }

                    sendCameraImage(photoURI!!)
                    photoURI = null
                }
            }

            DEV_REQUEST_CODE -> {
                checkSettingInfo()
            }

            TEL_REQUEST_CODE -> {
            }

            KAKAO_LOGIN_REQUEST_CODE -> {
                //TODO 카카오 로그인 이후
            }

            X_PAY_REQUEST_CODE -> {
            }

//            ONESTORE_PURCHASE_REQUEST_CODE -> {
//                /*
//                 * launchPurchaseFlowAsync API 호출 시 전달받은 intent 데이터를 handlePurchaseData를 통하여 응답값을 파싱합니다.
//                 * 파싱 이후 응답 결과를 launchPurchaseFlowAsync 호출 시 넘겨준 PurchaseFlowListener 를 통하여 전달합니다.
//                 */
//                if (resultCode == Activity.RESULT_OK) {
//                    if (mPurchaseClient != null) {
//                        if (!mPurchaseClient!!.handlePurchaseData(intent)) {
//                            DLog.e("bjj onActivityResult handlePurchaseData false ");
//                            // listener is null
//                        }
//                    }
//                } else {
//                    DLog.e("bjj onActivityResult user canceled");
//                    // user canceled , do nothing..
//                }
//            }
//
//            ONESTORE_LOGIN_REQUEST_CODE -> {
//                // TODO onestore 로그인 이후
//            }

            REQUEST_QRSCAN_ACTIVITY -> { //QR
                if (resultCode == Activity.RESULT_OK) {
                    val qrResult = intent?.getStringExtra(QrcodeScanActivity.CALL_BACK)

                    DLog.e("bjj REQUEST_QRSCAN_ACTIVITY :: " + qrResult)

                    if (qrResult.isNullOrEmpty()) {
                        mWebview?.sendEvent(IdevelServerScript.OPEN_QR, QrInfo("").toJsonString())
                    } else {
                        mWebview?.sendEvent(IdevelServerScript.OPEN_QR, QrInfo(qrResult).toJsonString())
                    }
                } else {
                    mWebview?.sendEvent(IdevelServerScript.OPEN_QR, QrInfo("").toJsonString())
                }
            }
        }
    }

    /**
     * 권한 체크
     */
    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Dexter.withContext(this).withPermissions(MyApplication.PERMISSIONS).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    report.let {
                        DLog.e("bjj PERMISSION checkPermission onPermissionsChecked aa ==>> "
                                + it.areAllPermissionsGranted() + " ^ "
                                + it.isAnyPermissionPermanentlyDenied
                        )

                        if (it.areAllPermissionsGranted()) { // 모든 권한 허용
                            setMainView()
                        } else if (it.isAnyPermissionPermanentlyDenied) {
                            DLog.e("bjj PERMISSION checkPermission onPermissionsChecked bb ==>> "
                                    + Build.VERSION.SDK_INT)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                DLog.e("bjj PERMISSION checkPermission onPermissionsChecked bb ==>> "
                                        + Build.VERSION.SDK_INT + " ^ "
                                        + Environment.isExternalStorageManager())
                                if (Environment.isExternalStorageManager()) {
                                    setMainView()
                                } else {
                                    showPermissionDenyDialog("MANAGE_EXTERNAL_STORAGE")
                                }

                                return
                            }

                            showPermissionDenyDialog("")
                        } else {
                            finish()
                        }

                    }
                }

                override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
                    DLog.e("bjj PERMISSION checkPermission onPermissionRationaleShouldBeShown ==>> " + permissions)

                    token?.continuePermissionRequest()
                }
            }).onSameThread().check()

            return false
        }

        return true
    }


    private fun removeSplash() {
        if (isFinishing) {
            return
        }

        (this@MainActivity as Activity).runOnUiThread {
            setSplash()
        }
    }

    //공유 팝업노출
    private fun openSharePopup(url: String) {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, url)
        sendIntent.type = "text/plain"

        startActivity(Intent.createChooser(sendIntent, ""))
    }

    // web page clear history
    private fun pageClearHistory() {
        mWebview?.clearHistory()
        mWebview?.removeAllViews()
    }

//    private fun initOneStore() {
//        // PurchaseClient 초기화 - context 와 Signature 체크를 위한 public key 를 파라미터로 넘겨줍니다.
//        mPurchaseClient = PurchaseClient(this, AppSecurity.getPublicKey())
//        // 원스토어 서비스로 인앱결제를 위한 서비스 바인딩을 요청합니다.
//        mPurchaseClient?.connect(mServiceConnectionListener)
//    }

    /**
     * WEB INTERFACE
     */
    private val iWebBridgeApi = object : IWebBridgeApi {
        override fun pageClearHistory() {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.pageClearHistory()
            }
        }

        override fun openSharePopup(url: String) {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.openSharePopup(url)
            }
        }

        override fun getPushRegId() {
            (this@MainActivity as Activity).runOnUiThread {
                val regId = SharedPreferencesUtil.getString(this@MainActivity, SharedPreferencesUtil.Cmd.PUSH_REG_ID)
                mWebview?.sendEvent(IdevelServerScript.GET_PUSH_REG_ID, getPushRegIdInfo(regId!!, "AOS").toJsonString())
            }
        }

        override fun restartApp() {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.restartApp()
            }
        }

        override fun finishApp() {
            (this@MainActivity as Activity).runOnUiThread {
                System.exit(0)
            }
        }

        override fun getAppVersion() {
            (this@MainActivity as Activity).runOnUiThread {
                val version = getVersionName(this@MainActivity)
                mWebview?.sendEvent(IdevelServerScript.GET_APP_VERSION, GetAppVersionInfo(version!!).toJsonString())
            }
        }

        override fun requestCallPhone(data: RequestCallPhoneInfo) {
            (this@MainActivity as Activity).runOnUiThread {
                try {
                    val callIntent = Intent(Intent.ACTION_DIAL)
                    callIntent.data = Uri.parse("tel:${data.phoneNumber}")
                    startActivity(callIntent)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }

        override fun requestExternalWeb(data: RequestExternalWebInfo) {
            (this@MainActivity as Activity).runOnUiThread {
                intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.url))
                startActivity(intent)
            }
        }

        override fun removeSplash() {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.removeSplash()
            }
        }

        override fun getGpsInfo() {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.getGPSLoacation()
            }
        }

        override fun readyOneStoreBilling() {
//            (this@MainActivity as Activity).runOnUiThread {
//                this@MainActivity.initOneStore()
//            }
        }

        override fun requestBuyProduct(data: RequestBuyProductInfo) {
//            (this@MainActivity as Activity).runOnUiThread {
//                val productType = getItemType(data.productId)
//                this@MainActivity.requestBilling(data.productId, productType)
//            }
        }

        override fun openCamera(type: String, param: String) {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.doTakePhotoAction(type, param)
            }
        }

        override fun openGallery(type: String, param: String) {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.doTakeAlbumAction(type, param)
            }
        }

        override fun setPushVibrate(isBool: Boolean) {
            (this@MainActivity as Activity).runOnUiThread {
                SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PUSH_VIBRATE, isBool)
            }
        }

        override fun setPushBeep(isBool: Boolean) {
            (this@MainActivity as Activity).runOnUiThread {
                SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PUSH_BEEP, isBool)
            }
        }


        override fun setAutoLogin(isAuto: Boolean) {
            (this@MainActivity as Activity).runOnUiThread {
                SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN, isAuto)
            }
        }

        override fun getAutoLogin() {
            (this@MainActivity as Activity).runOnUiThread {
                val isAuto = SharedPreferencesUtil.getBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN)

                mWebview?.sendEvent(IdevelServerScript.GET_AUTO_LOGIN, AutoLoginInfo(isAuto).toJsonString())
            }
        }

        override fun setAccount(id: String, pw: String) {
            (this@MainActivity as Activity).runOnUiThread {
                SharedPreferencesUtil.setString(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN_ID, id)
                SharedPreferencesUtil.setString(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN_PW, pw)
            }
        }

        override fun getAccount() {
            (this@MainActivity as Activity).runOnUiThread {
                val id = SharedPreferencesUtil.getString(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN_ID)
                val pw = SharedPreferencesUtil.getString(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN_PW)

                mWebview?.sendEvent(IdevelServerScript.GET_ACCOUNT, AccountInfo(id!!, pw!!).toJsonString())
            }
        }

        override fun downloadFile(fileURL: String, fileName: String) {
            (this@MainActivity as Activity).runOnUiThread {
                testRecordDownload(fileURL, fileName)
            }
        }

        override fun setQrFlash(isBool: Boolean) {
            (this@MainActivity as Activity).runOnUiThread {
                isQrFlash = isBool
            }
        }

        override fun openQR() {
            (this@MainActivity as Activity).runOnUiThread {
                val intent = Intent(this@MainActivity, QrcodeScanActivity::class.java)
                intent.putExtra(QrcodeScanActivity.IS_FLASH, isQrFlash)

                startActivityForResult(intent, REQUEST_QRSCAN_ACTIVITY)
            }
        }

        override fun startNFC(isBool: Boolean) {
            (this@MainActivity as Activity).runOnUiThread {
                if (isBool) {
                    Toast.makeText(this@MainActivity, "NFC 인식이 활성화되었습니다.", Toast.LENGTH_SHORT).show()
                }

                isNFCenable = isBool
            }
        }

        override fun getBattery() {
            (this@MainActivity as Activity).runOnUiThread {
                val battery = getBatteryPercentage(this@MainActivity)
                mWebview?.sendEvent(IdevelServerScript.GET_BATTERY, BatteryInfo(battery).toJsonString())
            }
        }
    }

    companion object {
        private val HANDLER_NETWORK_TIMER = 1 // The network timer handler.
        private val HANDLER_SPLASH = 2 // 스플래시 종료 핸들러.
        private val HANDLER_SPLASH_DELAY = 3 // 스플래시 delay.

        private val DEV_REQUEST_CODE = 1000 // 히든 메뉴에서 돌아왔을 때 flag값.
        private val X_PAY_REQUEST_CODE = 999
        private val KAKAO_LOGIN_REQUEST_CODE = 998
        private val TEL_REQUEST_CODE = 997
        private val PICK_FROM_ALBUM = 996
        private val PICK_FROM_CAMERA = 995
        private val REQUEST_QRSCAN_ACTIVITY = 994


        private val PING_TIME = 100000 //The ping time.
        private val NETWORK_CONNECTION_ERROR = 1 //The network connection error.
        private val TIMEOUT_ERROR = 2 //The timeout error.
        private val APP_VERSION_CHECK = 4 //앱 버전 check.

//        //onestore inapp
//        private val ONESTORE_LOGIN_REQUEST_CODE = 1001
//        private val ONESTORE_PURCHASE_REQUEST_CODE = 2001
//        private val IAP_API_VERSION = 5
//
//        //google inapp
//        private val REQ_PERMISSION_CODE = 1
//        private val BILLING_SUBSCRIBE_MONTH_PRODUCT_ID = "subscribe_month"
//        private val BILLING_SINGLE_MONTH_PRODUCT_ID = "single_month"
//        private val BILLING_LICENS_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsEH3Bt7tpFlqDW57EgiDX0FT19NRCoTEGG8SIj002k2nbc/zlhahr3LalchTfVsJ9ctB1Mls37qWYxWhxnWRk86HV25n1422FQzJL807kx8vwastA/ZcBtl7AjOj0POWEGKO2paIE2IJ9npsBZlVLvRaBiAe25sPIPgcCH9nsI037JUxzExbJJE4T1o1IQtVNOm9CucTG413qjtETfQr2kAGrZ3xRX1gdPmChvqvXfxJCogXDQY5M5Ki1qRCI+htim8AxtRi6uXwwrkg6Q7bGIv36wDF+6azc3wugtei+OiDPpxlh+ADOAnJNqSuMYoSbqob3Uaj8VxqFyGK9/MgoQIDAQAB"
    }

    private class WeakHandler(act: MainActivity) : Handler() {
        private val ref: WeakReference<MainActivity> = WeakReference(act)

        override fun handleMessage(msg: Message) {
            val act = ref.get()

            if (act != null) {
                when (msg.what) {
                    HANDLER_NETWORK_TIMER -> {
                        act.showErrorDlg(TIMEOUT_ERROR)
                    }
                    HANDLER_SPLASH -> if (act.checkPermission()) {
                        act.setMainView()
                    }
                    HANDLER_SPLASH_DELAY -> {
                        act.showMainView()
                    }
                }
            }
        }
    }

    private val networkListener = object : NetworkChangeListener {
        override fun onNetworkDisconnected() {
            DLog.e("bjj Listener onNetworkDisconnected")

            // 네트워크 전환 시 onNetworkDisconnected 들어왔을 경우 1초 딜레이 후 네트워크 상태 체크하여 네트워크 차단팝업 발생하도록 함
            (this@MainActivity as Activity).runOnUiThread {
                Handler().postDelayed({
                    if (getNetworkInfo(applicationContext) == NETWORK_TYPE_ETC) {
                        showErrorDlg(NETWORK_CONNECTION_ERROR)
                    }
                }, 1000L)
            }
        }

        override fun onNetworkconnected() {
            DLog.e("bjj Listener onNetworkconnected")
        }

        override fun onDataSaverChanged() {
            DLog.e("bjj Listener onDataSaverChanged")

//            (this@MainActivity as Activity).runOnUiThread {
//                showDataSaveDlg(R.string.popup_title_data_save, R.string.popup_msg_data_save)
//            }
        }
    }

    private var dataSaverListener = object : IDataSaverListener {
        override fun onDataSaverChanged() {
            DLog.e("bjj Listener onDataSaverChanged")

//            (this@MainActivity as Activity).runOnUiThread {
//                showDataSaveDlg(R.string.popup_title_data_save, R.string.popup_msg_data_save)
//            }
        }
    }

    // 현재 kakao만 적용됨
    private fun urlLoading(view: WebView?, uri: Uri?): Boolean {
        if (uri.toString().isNullOrEmpty()) {
            return false
        }

        val url = uri.toString()
        val scheme = uri!!.scheme

        DLog.e("bjj uri.toString() = $uri, ${uri!!.scheme}")

        when (scheme) {
            "https" -> return false
            "intent" -> {
                try {
                    // Intent 생성
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)

                    // 실행 가능한 앱이 있으면 앱 실행
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivityForResult(intent, KAKAO_LOGIN_REQUEST_CODE)

                        DLog.e("bjj urlLoading intent ACTIVITY: ${intent.getPackage()}")

                        return true
                    }

                    // Fallback URL이 있으면 현재 웹뷰에 로딩
                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                    if (fallbackUrl != null) {
                        view?.loadUrl(fallbackUrl)

                        DLog.e("bjj urlLoading intent FALLBACK: $fallbackUrl")

                        return true
                    }

                    DLog.e("bjj urlLoading intent Could not parse anythings")

                } catch (e: URISyntaxException) {
                    DLog.e("bjj urlLoading intent Invalid intent request", e)
                }

                return true
            }
            "tel" -> {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse(url))
                startActivityForResult(dialIntent, TEL_REQUEST_CODE)

                return true
            }
            else -> return false
        }
    }

    private fun isInstalledApp(context: Context, packageName: String?): Boolean {
        val appList = context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in appList) {
            if (appInfo.packageName == packageName) {
                return true
            }
        }

        return false
    }


    //Google BILLING
//    override fun onBillingInitialized() {
//        // * 구매 완료시 호출
//        // productId: 구매한 sku (ex) no_ads)
//        // details: 결제 관련 정보
////        val isBool = googleBp?.isPurchased(BILLING_SUBSCRIBE_MONTH_PRODUCT_ID) ?: false
////        SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PURCHASED_SINGLE_MONTH, isBool)
//
//        mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(true).toJsonString())
//
//        DLog.e("bjj BILLING onBillingInitialized")
//    }
//
//    override fun onPurchaseHistoryRestored() {
//        // * 구매 정보가 복원되었을때 호출
//        // bp.loadOwnedPurchasesFromGoogle() 하면 호출 가능
////        val isBool = googleBp?.isPurchased(BILLING_SUBSCRIBE_MONTH_PRODUCT_ID) ?: false
////        SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PURCHASED_SINGLE_MONTH, isBool)
//
//        DLog.e("bjj BILLING onPurchaseHistoryRestored")
//    }
//
//    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
//        // * 구매 오류시 호출
//        // errorCode == Constants.BILLING_RESPONSE_RESULT_USER_CANCELED 일때는
//        // 사용자가 단순히 구매 창을 닫은것임으로 이것 제외하고 핸들링하기.
//
////         val skuDetails: SkuDetails = googleBp?.getPurchaseListingDetails(productId) as SkuDetails
//
//
//        DLog.e("bjj BILLING onProductPurchased " + productId + " ^ " + details)
//
//        //TODO 아랫걸로 보낼지 activityresult로 보낼지 판단
//        //single_month purchased at Wed Aug 12 22:51:00 GMT+09:00 2020(GPA.3353-4786-9413-58565). Token: dgkbeeoiclkjpdoafoogehim.AO-J1OzUdS_d7A_lyCiKHUC825T4DDQu86PdwB0R0tRgrrv28mBL4YRbY1v_hoXsWW8gNt-34QRjgIPLyBvXeEoPf4FbDwytbUTjd0hSzOMw9pEIhU3NO6XQhC3s8F_L81UwzFOOnM4x, Signature: CITI8oTFUphcpTels23QpNxcWIZzdBULJK5VZwtIta9dL46mOlTTfMQeYM13Wk5NUdhbxQ3D1vPsYxEed+nEb30jkDqQyX7UMnFhIiTp6T8VI1PIsuQzhoIyWoCfh0Q3eP7W+yYjw8VluD2ebTo69T6x4LqO6y3lcQxlkMrKL9pRbG6m7NgqoBRrREBm8H9fPexplvmc//AEmQUmGIpoGoE8MRMoxEPkC0+3l+HhFk3qktZX1pzb4vufUtHjpCevygI2qQfil0Nt1gtccE3/6p8bUu5MDf1vNct+qvsQ5X1QkF02a9d+xuDmnIxXg+CFqu5GIBA3p/iI2Np2s1knrw==
//        mWebview?.sendEvent(tatalkServerScript.GET_REQUEST_BUY_PRODUCT_INFO, ReturnRequestBuyProductInfo(true, details.toString()).toJsonString())
//
//
////        if (productId.equals("single_month")) {
//        // TODO: 구매 해 주셔서 감사합니다! 메세지 보내기
////            val isBool = googleBp?.isPurchased(BILLING_SUBSCRIBE_MONTH_PRODUCT_ID) ?: false
////            SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PURCHASED_SINGLE_MONTH, isBool)
//
//        // * 광고 제거는 1번 구매하면 영구적으로 사용하는 것이므로 consume하지 않지만,
//        // 만약 게임 아이템 100개를 주는 것이라면 아래 메소드를 실행시켜 다음번에도 구매할 수 있도록 소비처리를 해줘야한다.
//        // bp.consumePurchase(Config.Sku);
////        }
//    }
//
//    override fun onBillingError(errorCode: Int, error: Throwable?) {
//        // * 처음에 초기화됬을때.
//        val isPurchased = googleBp?.isPurchased(BILLING_SINGLE_MONTH_PRODUCT_ID) ?: false
//
//        if (errorCode != Constants.BILLING_RESPONSE_RESULT_USER_CANCELED) {
//            mWebview?.sendEvent(tatalkServerScript.GET_REQUEST_BUY_PRODUCT_INFO, ReturnRequestBuyProductInfo(false, "").toJsonString())
//        }
//
//        DLog.e("bjj BILLING onBillingError " + errorCode + " ^ " + error?.message + " ^ " + isPurchased + " ^ " + googleBp)
//    }


    /**
     * GPS
     */
    private fun getGPSLoacation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "사용자의 위치 정보 권한을 허용하지 않았습니다", Toast.LENGTH_SHORT).show()
            return
        }

        if (mLocationManager == null) {
            mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        mLocationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, mLocationListener)
        mLocationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1f, mLocationListener)
    }


    private val mLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            var geoCoder = Geocoder(this@MainActivity, Locale.getDefault())
            val addresses = geoCoder.getFromLocation(location!!.latitude, location!!.longitude, 1) // Here 1 represent max location result to returned, by documents it recommended 1 to 5

            DLog.d("bjj getGPSLoacation 00 : $addresses, ${addresses?.size}")

            if (addresses == null) {
                return
            }

            if (addresses.size == 0) {
                return
            }

            val address = addresses[0].getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

            val city = addresses[0].locality
            val state = addresses[0].adminArea
            val country = addresses[0].countryName
            val postalCode = addresses[0].postalCode
            val knownName = addresses[0].featureName // Only if available else return NULL

            val latitudeStr: String = java.lang.String.valueOf(location!!.latitude)
            val longitudeStr: String = java.lang.String.valueOf(location!!.longitude)

            if (location.provider == LocationManager.GPS_PROVIDER) {
                DLog.d("bjj getGPSLoacation aa : $address, ${latitudeStr}, ${longitudeStr}")
            } else {
                DLog.d("bjj getGPSLoacation bb : $address, ${latitudeStr}, ${longitudeStr}")
            }

            val gpsData = getLocationInfo(latitudeStr, longitudeStr, address)

            if (gpsData != null) {
                mWebview?.sendEvent(IdevelServerScript.GET_GPS_INFO, gpsData!!.toJsonString())
                mLocationManager?.removeUpdates(this)
            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun doTakeAlbumAction(type: String, param: String) {
        mCameraReturnParam = param
        mCameraReturnType = type

//        val intent = Intent(Intent.ACTION_PICK)
//        intent.type = MediaStore.Images.Media.CONTENT_TYPE
//        intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        DLog.e("bjj doTakeAlbumAction :: " + Build.MANUFACTURER)

        if (Build.MANUFACTURER == "samsung") {
            val intent = Intent("com.sec.android.app.myfiles.PICK_DATA")
            intent.putExtra("CONTENT_TYPE", "*/*")
            intent.addCategory(Intent.CATEGORY_DEFAULT)

            startActivityForResult(intent, PICK_FROM_ALBUM)
        } else {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"

            startActivityForResult(intent, PICK_FROM_ALBUM)
        }
    }

    private fun doTakePhotoAction(type: String, param: String) {
        mCameraReturnParam = param
        mCameraReturnType = type

        val intent = Intent()
        intent.action = MediaStore.ACTION_IMAGE_CAPTURE

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_$timeStamp.jpg"

        createImageUri(imageFileName, "image/jpg")?.let { uri ->
            photoURI = uri
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(intent, PICK_FROM_CAMERA)
        }
    }

    private var photoURI: Uri? = null
    fun createImageUri(filename: String, mimeType: String): Uri? {
        var values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun sendGalleryImage(uri: Uri, index: Int = 0, totalCount: Int = 0) {
        DLog.e("bjj camera sendGalleryImage :: path " + getImageFile(uri)?.absolutePath)

        if (totalCount > 0) {
            uploadFile(getImageFile(uri)?.absolutePath, index, totalCount)
        } else {
            uploadFile(getImageFile(uri)?.absolutePath)
        }
    }

//    private fun sendCameraImage(bitmap: Bitmap) {
//        DLog.e("bjj camera sendCameraImage :: bitmap " + bitmap + " ^ " + getImageFile(getCaptureImageUri(bitmap)!!)?.absolutePath)
//
//        uploadFile(getImageFile(getCaptureImageUri(bitmap)!!)?.absolutePath)
//    }

    private fun sendCameraImage(uri: Uri) {
        DLog.e("bjj camera sendCameraImage :: path " + getImageFile(uri)?.absolutePath)
        uploadFile(getImageFile(uri)?.absolutePath)
    }

    private fun resizeBitmapImageFn(bmpSource: Bitmap, maxResolution: Int): Bitmap? {
        val iWidth = bmpSource.width //비트맵이미지의 넓이
        val iHeight = bmpSource.height //비트맵이미지의 높이
        var newWidth = iWidth
        var newHeight = iHeight
        var rate = 0.0f

        DLog.e("bjj resizeBitmapImageFnaa $iWidth ^ $iHeight")

        //이미지의 가로 세로 비율에 맞게 조절
        if (iWidth > iHeight) {
            if (maxResolution < iWidth) {
                rate = maxResolution / iWidth.toFloat()
                newHeight = (iHeight * rate).toInt()
                newWidth = maxResolution
            }
        } else {
            if (maxResolution < iHeight) {
                rate = maxResolution / iHeight.toFloat()
                newWidth = (iWidth * rate).toInt()
                newHeight = maxResolution
            }
        }

        DLog.e("bjj resizeBitmapImageFnbb $newWidth ^ $newHeight")

        return Bitmap.createScaledBitmap(bmpSource, newWidth, newHeight, true)
    }

    private fun getImageFile(uri: Uri): File? {
        var uri: Uri? = uri
        val projection = arrayOf<String>(MediaStore.Images.Media.DATA)

        if (uri == null) {
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        var mCursor: Cursor? = contentResolver.query(uri!!, projection, null, null, MediaStore.Images.Media.DATE_MODIFIED + " desc")

        if (mCursor == null || mCursor.getCount() < 1) {
            return null // no cursor or no record
        }

        val column_index: Int = mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        mCursor.moveToFirst()

        val path: String = mCursor.getString(column_index)

        if (mCursor != null) {
            mCursor.close()
            mCursor = null
        }

        return File(path)
    }

    private fun getCaptureImageUri(inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)

        val path: String = MediaStore.Images.Media.insertImage(contentResolver, inImage, packageName + "_capture_" + System.currentTimeMillis(), null)

        return Uri.parse(path)
    }

    private fun uploadFile(filePath: String?, index: Int = 0, totalCount: Int = 0) {
        val url = URL(getUploadUrl(this@MainActivity))

        var returnParamStr = mCameraReturnParam

        if (totalCount > 0) {
            val sb = StringBuilder()
            sb.append(mCameraReturnParam).append(index).append("/").append(totalCount)

            returnParamStr = sb.toString()
        }

        DLog.e("bjj uploadFile :: " + filePath + " ^ " + returnParamStr)

        mApiManager?.uploadFile(url.toString(), filePath ?: "", filePath ?: "", mCameraReturnType
                ?: "", returnParamStr ?: "", object : OnResultListener<Any> {
            override fun onResult(result: Any, flag: Int) {
                if (result == null) {
                    return
                }

                val uploadInfoData = result as UploadInfoData

                var sucessStr: String
                var keyStr: String

                sucessStr = if (uploadInfoData?.isUploadSucess.isNullOrEmpty()) {
                    "FAIL"
                } else {
                    uploadInfoData?.isUploadSucess!!
                }

                keyStr = if (uploadInfoData?.fileKey.isNullOrEmpty()) {
                    ""
                } else {
                    uploadInfoData?.fileKey!!
                }

                mWebview?.sendEvent(IdevelServerScript.GET_REQUEST_FILE_UPLOAD_INFO, ReturnRequestFileUploadInfo(sucessStr, mCameraReturnType
                        ?: "", mCameraReturnParam ?: "", keyStr).toJsonString())
            }

            override fun onFail(error: Any, flag: Int) {
                mWebview?.sendEvent(IdevelServerScript.GET_REQUEST_FILE_UPLOAD_INFO, ReturnRequestFileUploadInfo("FAIL", mCameraReturnType
                        ?: "", mCameraReturnParam ?: "", "").toJsonString())
            }
        })
    }


    private var recordDownloadTask: AsyncTask<Void, Void, Boolean>? = null

    @SuppressLint("StaticFieldLeak")
    private fun testRecordDownload(fileURL: String, fileName: String) {
        val baseUrl = "https://lgdc.wtest.biz"

        mApiManager?.startRecordDownload(baseUrl, fileURL, object : OnResultListener<Any> {
            override fun onResult(result: Any, flag: Int) {
                recordDownloadTask?.cancel(true)
                recordDownloadTask = object : AsyncTask<Void, Void, Boolean>() {
                    override fun doInBackground(vararg voids: Void): Boolean {
                        return saveRecordFile(result as ResponseBody, fileName)
                    }

                    override fun onPostExecute(result: Boolean?) {
                    }
                }

                recordDownloadTask?.execute()
            }

            override fun onFail(error: Any, flag: Int) {
            }
        })
    }

    @Synchronized
    private fun saveRecordFile(body: ResponseBody, fileName: String): Boolean {
        try {
            // 파일있으면 삭제부터 한다.
//            deleteFile(fileName, pkgName)

//      1
            val rootPath = Environment.getExternalStorageDirectory().absolutePath
            var savedRootFilePath = rootPath
//      2
//            var savedRootFilePath = if (rootPath.endsWith("/")) {
//                "${rootPath}Android${File.separator}data${File.separator}${packageName}"
//            } else {
//                "${rootPath}${File.separator}Android${File.separator}data${File.separator}${packageName}"
//            }
//      3
//            var savedRootFilePath = getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!.path


            savedRootFilePath = "$savedRootFilePath${File.separator}Download${File.separator}"
            val sohoDownloadFolder = File(savedRootFilePath, "${File.separator}dailyinspection")

            if (!sohoDownloadFolder.exists()) {
                sohoDownloadFolder.mkdirs()
            }

            val destinationFile = File(savedRootFilePath, "dailyinspection${File.separator}${fileName}")
            var inputStream: InputStream? = null
            var ost: OutputStream? = null

            DLog.e("bjj saveRecordFile :: INIT :: "
                    + savedRootFilePath
                    + " ^ " + sohoDownloadFolder.exists()
                    + " ^ " + sohoDownloadFolder.path
                    + " ^ " + sohoDownloadFolder.isDirectory
                    + " ^ " + sohoDownloadFolder.isFile
                    + " ^ " + destinationFile.path
                    + " ^ " + destinationFile.isDirectory
                    + " ^ " + destinationFile.isFile)
            try {
                try {
                    inputStream = body.byteStream()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                ost = FileOutputStream(destinationFile, true)

                val data = ByteArray(32 * 1024)
                var fileSizeDownloaded: Long = 0
                val fileSize = body.contentLength()

                while (true) {
                    val read = inputStream?.read(data) ?: break

                    if (read == -1) {
                        break
                    }

                    if (destinationFile.length() == fileSize) {
                        break
                    }

                    DLog.e("bjj saveRecordFile :: ING "
                            + destinationFile.path + " ^ "
                            + destinationFile.length() + " ^ "
                            + fileSizeDownloaded + " ^ "
                            + fileSize)

                    ost.write(data, 0, read)
                    fileSizeDownloaded += read.toLong()
                }

                ost.flush()

                DLog.e("bjj saveRecordFile :: DONE "
                        + destinationFile.path + " ^ "
                        + destinationFile.length())

                (this@MainActivity as Activity).runOnUiThread {
                    openFileManager(destinationFile.path, true)
                }

                return true
            } catch (e: IOException) {
                e.printStackTrace()

                (this@MainActivity as Activity).runOnUiThread {
                    openFileManager(destinationFile.path, false)
                }

                DLog.e("bjj saveRecordFile :: FAIL aa " + e)
                return false
            } finally {
                inputStream?.close()
                ost?.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()

            (this@MainActivity as Activity).runOnUiThread {
                openFileManager("", false)
            }

            DLog.e("bjj saveRecordFile :: FAIL bb " + e)
            return false
        }
    }

    private fun openFileManager(path: String, isSuccess: Boolean) {
        if (!path.isEmpty()) {
            Toast.makeText(this, "다운로드 완료.\n" + path + "에서 확인해주세요.", Toast.LENGTH_SHORT).show()
        }

        mWebview?.sendEvent(IdevelServerScript.SET_DOWNLOAD_FILE, DownloadFileStatusInfo(isSuccess).toJsonString())
    }


    /**
     * Read From NFC Tag
     */
    private fun showNFC(intent: Intent) { //테그데이터를 전달받았을때 태그정보를 화면에 보여줌.
//        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
//        val tag2 = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)

        if (isNFCenable) {
            val nfcStr: String = NfcUtils.resolveIntent(intent)

            if (nfcStr.isEmpty()) {
                mWebview?.sendEvent(IdevelServerScript.SET_NFC, NfcInfo("").toJsonString())
            } else {
                mWebview?.sendEvent(IdevelServerScript.SET_NFC, NfcInfo(nfcStr).toJsonString())
            }
        }
    }

    //battery
    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            (this@MainActivity as Activity).runOnUiThread {
                val level = intent.getIntExtra("level", 0)
                mWebview?.sendEvent(IdevelServerScript.SET_BATTERY, BatteryInfo(level).toJsonString())

                DLog.e("bjj getBatteryPercentage :: batteryReceiver " + level)
            }
        }
    }

    private fun initBeacon() {
        m_positioningEngine = PositioningEngine.getInstance()

        val config = LMConfig()
        config.projectID = "PRJ000001" // Project ID
        config.useAssets = true
        config.useBeaconList = true
        config.isBeaconFromLBS = false
        m_positioningEngine?.addCheckInListener(this@MainActivity)
        m_positioningEngine?.addInfoListener(this@MainActivity)
        m_positioningEngine?.setDebugMode(true)
        m_positioningEngine?.init(this@MainActivity, config)
    }

    //2021. 11. 해당 Mac의 체크아웃 정보를 표출
    override fun onCheckOut(p0: LMBeacon?) {
        if (p0 != null) {
//            DLog.e("bjj Beacone >> onCheckOut = ${p0.name} ^ ${p0.rssi}")

            DLog.e("bjj Beacone >> onCheckOut = " + p0.toString())

            mWebview?.sendEvent(IdevelServerScript.SET_BEACON_ON_CHECK_OUT, BeaconInfo(p0.toString()).toJsonString())
        }
    }

    //2021. 11. 해당 Mac의 체크인 정보를 표출
    override fun onCheckIn(p0: LMBeacon?) {
        if (p0 != null) {
//            DLog.e("bjj Beacone >> onCheckIn = ${p0.name} ^ ${p0.rssi}")

            DLog.e("bjj Beacone >> onCheckIn = " + p0.toString())

            mWebview?.sendEvent(IdevelServerScript.SET_BEACON_ON_CHECK_IN, BeaconInfo(p0.toString()).toJsonString())
        }
    }

    //2021. 11. 해당 Mac의 정보를 표출
    override fun onNearBy(p0: ArrayList<LMBeacon>?) {
        if (p0 != null) {
            if (p0.size > 0) {
                DLog.e("bjj Beacone >> onNearBy = get ${p0.size} ^ ${p0[0]}")

                mWebview?.sendEvent(IdevelServerScript.SET_BEACON_NEAR_BY, BeaconInfo(p0[0].toString()).toJsonString())
            }
        }
    }

    override fun onBluetoothStateChanged(p0: IPositioningInfoListener.BluetoothState?) {
        DLog.e("bjj Beacone >> onBluetoothStateChanged = ${p0?.name}")

        if (p0 != null) {
            mWebview?.sendEvent(IdevelServerScript.SET_BEACON_STATE_CHANGE, BeaconInfo(p0.toString()).toJsonString())
        }
    }

    override fun onInitResult(p0: LMEnum.InitState?, p1: String?) {
        DLog.e("bjj Beacone >> onInitResult = ${p0?.value()}, $p1, $m_positioningEngine")

        m_positioningEngine?.start()
        var alBeacon: ArrayList<LMBeacon> = ArrayList()

        for (i in 1..2000) {
            if (i == 1) {
                //2021. 11. DB 등록 MacAddress 을 For 문을 이용하여 나열 하시면 등록된 Mac 중 세기가 가장 센 장비가 체크인
                val beacon = LMBeacon()

                beacon.activeRange = 20.0F
                beacon.txPower = -65
                beacon.checkIn = "Y"
                beacon.name = "0007790DE515"
                beacon.macAddress = "0007790DE515"
                alBeacon.add(beacon)

                DLog.e("bjj Beacone >> onInitResult::addBeacon()")
            } else {
//                var beacon = LMBeacon();
//                beacon.activeRange = 20.0F
//                beacon.txPower = -65
//                beacon.checkIn = "Y"
//                beacon.name = "0007790CE${i}"
//                //beacon.macAddress = "0007790CE773"
//                alBeacon.add(beacon)
            }
        }

        DLog.e("bjj Beacone >> onInitResult::addBeacon() size = ${alBeacon.size}")

        m_positioningEngine?.addBeaconList(alBeacon)
    }

    override fun onBeaconList(p0: ArrayList<LMBeacon>?) {
        DLog.e("bjj Beacone >> onBeaconList = $p0")
    }

    //2021.11.19 가스센서 호출
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var mScanSettings: ScanSettings.Builder? = null
    private var scanFilters: List<ScanFilter>? = null
    private var mScanCallback: ScanCallback? = null
    private fun initGasBle() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        scanFilters = Vector<ScanFilter>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothLeScanner = mBluetoothAdapter!!.bluetoothLeScanner
            mScanSettings = ScanSettings.Builder()
            mScanSettings!!.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)

            val scanSettings: ScanSettings = mScanSettings!!.build()

            mScanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    super.onScanResult(callbackType, result)

                    try {
                        val mac = result.device.address.replace(":", "")

                        // 체크인된 건물안에 설치된 가스센서 mac 주소를 함수로 지정하여 입력하시면 됩니다.
                        if (mac.equals("0007790D026B")) {
                            DLog.e("bjj gas mac equal 26B")

                            if (GasBleData.isGalBleData(result.scanRecord!!.bytes)) {
                                // make gasBLE data
                                makeGasBleData(result.scanRecord!!.bytes)
                            }
                        } else {
                            DLog.e("bjj gas mac not equal 26B")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            mBluetoothLeScanner!!.startScan(scanFilters, scanSettings, mScanCallback)
        }
    }

    private fun makeGasBleData(scanRecord: ByteArray?) {
        // BLE Scan Call-Back Data parsing
        val data: GasBleData = GasBleData.parse(scanRecord)
        // get O2 value
        val strO2: String = if (data.isO2SensorEnabled()) data.getO2Live().toString() + " %" else "N/A"
        // get CO value
        val strCO: String = if (data.isCOSensorEnabled()) data.getCOLive().toString() + " ppm" else "N/A"
        // get H2S value
        val strH2S: String = if (data.isH2SSensorEnabled()) data.getH2SLive().toString() + " ppm" else "N/A"
        // get CH4 value
        val strCH4: String = if (data.isCH4SensorEnabled()) data.getCH4Live().toString() + "% LEL" else "N/A"
        // get BatteryState
        val strBatteryState: String = data.getBatteryState()
        // get Temperature
        val strTemperature: String = data.getTemperature().toString() + "°C"
        // get Humidity
        val strHumidity: String = data.getHumidity().toString() + "%"
        // get isEmergency
        val isEmergency: Boolean = data.isEmergency()


        var resultStrBuffer = StringBuffer()
        if (strO2.isNotEmpty()) {
            resultStrBuffer.append("O2:").append(strO2).append(", ")
        }
        if (strCO.isNotEmpty()) {
            resultStrBuffer.append("CO:").append(strCO).append(", ")
        }
        if (strH2S.isNotEmpty()) {
            resultStrBuffer.append("H2S:").append(strH2S).append(", ")
        }
        if (strCH4.isNotEmpty()) {
            resultStrBuffer.append("CH4:").append(strCH4).append(", ")
        }
        if (strBatteryState.isNotEmpty()) {
            resultStrBuffer.append("BatteryState:").append(strBatteryState).append(", ")
        }
        if (strTemperature.isNotEmpty()) {
            resultStrBuffer.append("Temperature:").append(strTemperature).append(", ")
        }
        if (strHumidity.isNotEmpty()) {
            resultStrBuffer.append("Humidity:").append(strHumidity).append(", ")
        }

        if (isEmergency) {
            resultStrBuffer.append("Emergency:").append("true")
        } else {
            resultStrBuffer.append("Emergency:").append("false")
        }


        mWebview?.sendEvent(IdevelServerScript.SET_BLE, BleInfo(resultStrBuffer.toString()).toJsonString())

        // Put Data Array
        // !@*#&!*@$^!$*!@(#!@$(!@#(!@&#(&!$ <-- Array 혹은 DB에 가공된 Data 수집해 놓고 사용
        DLog.e("bjj gas makeGasBleData :: "
                + "O2 : " + strO2
                + ", CO : " + strCO
                + ", H2S : " + strCH4
                + ", CH4 : " + strH2S
                + ", Battery : " + strBatteryState
                + ", Tempe : " + strTemperature
                + ", Humidity : " + strHumidity
                + ", Emergency : " + isEmergency
        )
    }
}