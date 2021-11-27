package com.idevel.dailyinspection.beacon

import java.util.*


class BeaconInterfaceData {
    var beaconInfo = ArrayList<beaconInfoItem>()

    inner class beaconInfoItem {
        var macAddress: String? = null
    }
}
