package com.idevel.dailyinspection.fcm

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService

import com.idevel.dailyinspection.utils.DLog
import com.idevel.dailyinspection.utils.SharedPreferencesUtil

class FcmInstanceIDService : FirebaseInstanceIdService() {

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    @Override
    override fun onTokenRefresh() {
        // Get updated InstanceID token.
        val refreshedToken = FirebaseInstanceId.getInstance().token
        DLog.d("Refreshed token: $refreshedToken")

        SharedPreferencesUtil.setString(this, SharedPreferencesUtil.Cmd.PUSH_REG_ID, refreshedToken)
    }
    // [END refresh_token]

}
