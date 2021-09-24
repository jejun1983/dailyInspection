package com.idevel.dailyinspection.web;

import android.content.Context;

import com.idevel.dailyinspection.utils.DLog;

/**
 * The UrlData Class.
 *
 * @author : jjbae
 */
public class UrlData {
    /**
     * 시작 페이지
     */
    public static String NORMAL_SERVER_URL = "https://lgdc.wtest.biz/";

    /**
     * 파일 업로드 API url
     */
    public static String getUploadUrl(Context context) {
        String url = "";

//        if (BuildConfig.DEBUG) {
//            url = SharedPreferencesUtil.getString(context, SharedPreferencesUtil.Cmd.SETTING_URL);
//        } else {
//            // 상용
            url = "https://lgdc.wtest.biz/app/image.php";
//        }

        DLog.e("bjj UrlData :: getUploadUrl "+url);

        return url;
    }
}