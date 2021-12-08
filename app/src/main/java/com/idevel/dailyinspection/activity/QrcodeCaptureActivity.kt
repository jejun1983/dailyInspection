package com.idevel.dailyinspection.activity

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.idevel.dailyinspection.R
import com.idevel.dailyinspection.utils.DLog
import com.idevel.dailyinspection.utils.MessageEvent
import com.idevel.dailyinspection.utils.RxBus
import com.idevel.dailyinspection.utils.SharedPreferencesUtil
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ViewfinderView
import java.lang.reflect.Field
import kotlin.math.roundToInt


class QrcodeCaptureActivity : CaptureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        disableLaser()
        super.onCreate(savedInstanceState)

        val dm = resources.displayMetrics
        val layoutSize = (50 * dm.density).roundToInt()

        val params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, layoutSize)

        val titleText = TextView(this)
        titleText.layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        titleText.setTextColor(Color.parseColor("#ffffff"))
        titleText.setBackgroundColor(Color.parseColor("#929292"))
        titleText.text = resources.getString(R.string.app_name) + " QR 코드 등록"
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        titleText.gravity = Gravity.CENTER

        addContentView(titleText, params)

        val titleBtnSize = (30 * dm.density).roundToInt()
        val titleBtnParam = RelativeLayout.LayoutParams(titleBtnSize, titleBtnSize)
        titleBtnParam.topMargin = (10 * dm.density).roundToInt()
        titleBtnParam.leftMargin = (5 * dm.density).roundToInt()


        //TODO flash on/off
        val titleCheckBox = CheckBox(this)
//        titleCheckBox.setBackgroundResource(R.drawable.con_qr_toggle)
        titleCheckBox.bringToFront()
        addContentView(titleCheckBox, titleBtnParam)

        titleCheckBox.isChecked = SharedPreferencesUtil.getBoolean(this@QrcodeCaptureActivity, SharedPreferencesUtil.Cmd.QR_FLASH)
        titleCheckBox.setOnCheckedChangeListener { view, isChecked ->
            DLog.e("bjj QrcodeScanActivity aa " + isChecked + " ^ " + view.isShown)

            if(view.isShown){
                if (isChecked) {
                    RxBus.publish(MessageEvent(MessageEvent.MessageType.MT_FLASH_ON))
                } else {
                    RxBus.publish(MessageEvent(MessageEvent.MessageType.MT_FLASH_OFF))
                }

                finish()
            }
        }
    }

    private fun disableLaser() {
        val barcodeView = initializeContent()
        val viewFinder: ViewfinderView = barcodeView.viewFinder
        var scannerAlphaField: Field? = null

        try {
            scannerAlphaField = viewFinder.javaClass.getDeclaredField("SCANNER_ALPHA")
            scannerAlphaField.isAccessible = true
            scannerAlphaField.set(viewFinder, IntArray(1))
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }
}