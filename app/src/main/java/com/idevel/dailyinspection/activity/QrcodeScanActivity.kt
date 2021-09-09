package com.idevel.dailyinspection.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.idevel.dailyinspection.BuildConfig
import com.idevel.dailyinspection.R
import com.idevel.dailyinspection.utils.DLog
import java.util.*


class QrcodeScanActivity : AppCompatActivity() {
    companion object {
        const val IS_MANUAL = "IS_MANUAL"
        const val WEB_URL = "WEB_URL"
        const val CALL_BACK = "CALL_BACK"
    }

    private var myWebview: WebView? = null
    private var isRegistSucess = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_qrcode_scan)

        myWebview = findViewById<View>(R.id.my_webview) as WebView

        val isManual = intent.getBooleanExtra(IS_MANUAL, false)
        if (isManual) {
            val url = intent.getStringExtra(WEB_URL)

            DLog.e("bjj QrcodeScanActivity onCreate :: " + isManual + " ^ " + url)

            url?.let {
                showRegistWebView(it)
            }

            return
        }

        showScanner()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        DLog.i("bjj QrcodeScanActivity onActivityResult :: " + result)

        if (result.contents == null) {
            Toast.makeText(this@QrcodeScanActivity, "QR코드 인증이 취소되었습니다.", Toast.LENGTH_SHORT).show()

            isRegistSucess = false

            setResult(RESULT_CANCELED)
            finish()
        } else {
            DLog.i("bjj QrcodeScanActivity onActivityResult aa ::" + result.contents)

            Toast.makeText(this, "페이지를 가져오는데 성공하였습니다.", Toast.LENGTH_SHORT).show()

            isRegistSucess = true

            val i = Intent()
            i.putExtra(CALL_BACK, result.contents)

            setResult(RESULT_OK, i)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        myWebview?.let {
            it.stopLoading()
            it.clearCache(true)
            it.clearHistory()
            it.destroy()
        }
    }

    override fun onBackPressed() {
        DLog.i("bjj QrcodeScanActivity onBackPressed :: " + isRegistSucess)

        if (isRegistSucess) {
            setResult(RESULT_OK)
        } else {
            setResult(RESULT_CANCELED)
        }

        super.onBackPressed()
    }

    private fun showRegistWebView(url: String) {
        (this@QrcodeScanActivity as Activity).runOnUiThread {
            if (myWebview?.visibility == View.GONE) {
                myWebview?.visibility = View.VISIBLE
            }

            val myWebviewSettings = myWebview?.settings
            if (BuildConfig.DEBUG) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
                }
            }

            myWebviewSettings!!.javaScriptEnabled = true
            myWebviewSettings!!.setSupportZoom(false)
            myWebviewSettings!!.builtInZoomControls = false
            myWebviewSettings!!.useWideViewPort = true
            myWebviewSettings!!.domStorageEnabled = true
            myWebviewSettings!!.cacheMode = WebSettings.LOAD_DEFAULT
            myWebviewSettings!!.textZoom = 100

            myWebview?.loadUrl(url)
            isRegistSucess = false

            myWebview?.webViewClient = WebViewClient()
            myWebview?.webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                    return jsAlert(message, result!!, false)
                }

                override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
                    return jsAlert(message, result, true)
                }
            }
        }
    }

    private fun showScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setBeepEnabled(false)
        integrator.setOrientationLocked(false)
        integrator.captureActivity = QrcodeCaptureActivity::class.java
        integrator.setPrompt("QR코드를 사각형 안에 비춰주세요.")

        integrator.initiateScan()
    }

    fun jsAlert(message: String?, result: JsResult, hasNegativeBtn: Boolean): Boolean {
        if (isFinishing) {
            return false
        }

        DLog.i("bjj QrcodeScanActivity jsAlert :: " + message)

        if (message!!.contains("완료")) {
            isRegistSucess = true
        }

        val builder = AlertDialog.Builder(this@QrcodeScanActivity)
        builder.setTitle("알림")
        builder.setMessage(message)
        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
            result.confirm()

            DLog.i("bjj QrcodeScanActivity jsAlert :: " + message)


            if (isRegistSucess) {
                setResult(RESULT_OK)
                finish()
            }
        }
        builder.setOnCancelListener { result.cancel() }

        if (hasNegativeBtn) {
            builder.setNegativeButton(android.R.string.cancel) { dialog, which -> result.cancel() }
        }

        builder.create()
        builder.show()

        return true
    }
}