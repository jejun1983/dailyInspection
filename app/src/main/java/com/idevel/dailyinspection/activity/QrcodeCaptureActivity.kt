package com.idevel.dailyinspection.activity

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.*
import com.idevel.dailyinspection.R
import com.idevel.dailyinspection.utils.DLog
import com.idevel.dailyinspection.utils.MessageEvent
import com.idevel.dailyinspection.utils.RxBus
import com.idevel.dailyinspection.utils.SharedPreferencesUtil
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ViewfinderView
import java.lang.reflect.Field
import kotlin.math.roundToInt
import android.widget.RelativeLayout


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
        titleText.setBackgroundColor(Color.parseColor("#17244C"))
        titleText.text = resources.getString(R.string.app_name) + " QR 코드 점검"
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        titleText.gravity = Gravity.CENTER

        addContentView(titleText, params)


        //checkBox add s
        val lButtonParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        lButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        lButtonParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
        lButtonParams.bottomMargin = (100 * dm.density).roundToInt()

        val titleCheckBox = CheckBox(this)
        titleCheckBox.setButtonDrawable(R.drawable.con_qr_toggle)
        titleCheckBox.layoutParams = lButtonParams

        val lContainerLayout = RelativeLayout(this)
        lContainerLayout.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        lContainerLayout.addView(titleCheckBox)

        addContentView(lContainerLayout, lContainerLayout.layoutParams)
        //checkBox add e


        titleCheckBox.isChecked = SharedPreferencesUtil.getBoolean(this@QrcodeCaptureActivity, SharedPreferencesUtil.Cmd.QR_FLASH)
        titleCheckBox.setOnCheckedChangeListener { view, isChecked ->
            DLog.e("bjj QrcodeScanActivity aa " + isChecked + " ^ " + view.isShown)

            if (view.isShown) {
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