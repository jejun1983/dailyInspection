package com.idevel.dailyinspection.utils.ble;

public class GasBleData {
    public static final byte BATTERY_STATUS_CONNECTED = 39;
    public static final byte BATTERY_STATUS_LOW = 33;
    public static final byte BATTERY_STATUS_NORMAL = 32;
    public static final byte CH4_ETYPE_MASK = 9;
    public static final byte CH4_WARNING_BUMPERTEST = 78;
    public static final byte CH4_WARNING_FIRST = 73;
    public static final byte CH4_WARNING_NORMAL = 72;
    public static final byte CH4_WARNING_PREHEAT = 77;
    public static final byte CH4_WARNING_SECOND = 74;
    public static final byte CO_ETYPE_MASK = 7;
    public static final byte CO_WARNING_BUMPERTEST = 62;
    public static final byte CO_WARNING_FIRST = 57;
    public static final byte CO_WARNING_NORMAL = 56;
    public static final byte CO_WARNING_PREHEAT = 61;
    public static final byte CO_WARNING_SECOND = 58;
    public static final byte CO_WARNING_STEL = 59;
    public static final byte CO_WARNING_TWA = 60;
    public static final byte EMERGENCY_EMERGENCY_1 = 81;
    public static final byte EMERGENCY_EMERGENCY_2 = 82;
    public static final byte EMERGENCY_NORMAL = 80;
    public static final byte EMERGENCY_TYPE = 80;
    public static final byte H2S_ETYPE_MASK = 8;
    public static final byte H2S_WARNING_BUMPERTEST = 70;
    public static final byte H2S_WARNING_FIRST = 65;
    public static final byte H2S_WARNING_NORMAL = 64;
    public static final byte H2S_WARNING_PREHEAT = 69;
    public static final byte H2S_WARNING_SECOND = 66;
    public static final byte H2S_WARNING_STEL = 67;
    public static final byte H2S_WARNING_TWA = 68;
    public static final byte HUMIDTY_WARNING_ALERT = 25;
    public static final byte HUMIDTY_WARNING_NORMAL = 24;
    public static final int INT_TX_POWER_TYPE_LONG = 2;
    public static final int INT_TX_POWER_TYPE_NORMAL = 0;
    public static final int INT_TX_POWER_TYPE_SHORT = 1;
    public static final byte MSG_SEQ_NO_1 = 9;
    public static final byte MSG_SEQ_NO_2 = 10;
    public static final byte MSG_SEQ_ONLY = 8;
    public static final byte O2_ETYPE_MASK = 6;
    public static final byte O2_WARNING_BUMPERTEST = 54;
    public static final byte O2_WARNING_FIRST = 49;
    public static final byte O2_WARNING_NORMAL = 48;
    public static final byte O2_WARNING_PREHEAT = 53;
    public static final byte O2_WARNING_SECOND = 50;
    public static final byte O2_WARNING_THIRD = 55;
    public static final byte SENSOR_DISABLED = 0;
    public static final byte TEMPERATURE_WARNING_ALERT = 17;
    public static final byte TEMPERATURE_WARNING_NORMAL = 16;
    public static final byte TX_POWER_TYPE_LONG = 10;
    public static final byte TX_POWER_TYPE_NORMAL = 8;
    public static final byte TX_POWER_TYPE_SHORT = 9;
    public static final byte VIBRATION_STATUS_PHASE_1 = 40;
    public static final byte VIBRATION_STATUS_PHASE_2 = 41;
    public static final byte VIBRATION_STATUS_PHASE_3 = 42;
    private byte m_bApperance;
    private byte m_bBattery;
    private byte m_bBatteryType;
    private byte m_bCarbonType = 0;
    private byte m_bEmergency;
    private byte m_bHumidityType;
    private byte m_bHydrogenType = 0;
    private byte m_bMethaneType = 0;
    private byte m_bOxygenType = 0;
    private byte m_bTemeratureType;
    private byte m_bTxpower;
    private byte m_bTxpowerType;
    private byte m_bVibration;
    private String m_sCarbon = "0";
    private String m_sHumidity;
    private String m_sHydrogen = "0";
    private String m_sMethane = "0";
    private String m_sOxygen = "0";
    private String m_sTemperature;

    public static boolean isGalBleData(byte[] scanRecord) {
        if (scanRecord[8] == 65) {
            return false;
        }
        return true;
    }

    private GasBleData() {
    }

    public static GasBleData parse(byte[] scanRecord) {
        boolean existEType;
        byte[] eTypeGasMask = {6, 7, 8, 9};
        GasBleData data = new GasBleData();
        int index = 7 + 1;
        data.m_bApperance = scanRecord[7];
        int index2 = index + 1;
        int index3 = index2 + 1;
        data.m_bEmergency = scanRecord[index2];
        int index4 = index3 + 1;
        data.m_bTxpowerType = scanRecord[index3];
        int index5 = index4 + 1;
        data.m_bTxpower = scanRecord[index4];
        int index6 = index5 + 1;
        data.m_bTemeratureType = scanRecord[index5];
        data.m_sTemperature = BleUtils.convertByteToInt(scanRecord[index6]) + "." + BleUtils.convertByteToInt(scanRecord[14]);
        int index7 = index6 + 1 + 1;
        int index8 = index7 + 1;
        data.m_bHumidityType = scanRecord[index7];
        int index9 = index8 + 1;
        data.m_sHumidity = BleUtils.convertByteToInt(scanRecord[index8]) + "";
        int index10 = index9 + 1;
        data.m_bBatteryType = scanRecord[index9];
        int index11 = index10 + 1;
        data.m_bBattery = scanRecord[index10];
        int index12 = index11 + 1;
        data.m_bVibration = scanRecord[index11];
        int index13 = index12;


        byte eType = (byte) (scanRecord[index13] >> 3);


        for( int i = 0; i < 4; i++) {
            if (eType == eTypeGasMask[i]) {
                switch (eType) {
                    case 6:
                        int index14 = index13 + 1;
                        data.m_bOxygenType = scanRecord[index13];
                        data.m_sOxygen = BleUtils.convertByteToInt(scanRecord[index14]) + "." + BleUtils.convertByteToInt(scanRecord[index14 + 1]);
                        index13 = index14 + 1 + 1;
                        break;
                    case 7:
                        int index15 = index13 + 1;
                        data.m_bCarbonType = scanRecord[index13];
                        data.m_sCarbon = Integer.parseInt(BleUtils.toHexString(new byte[]{scanRecord[index15], scanRecord[index15 + 1]}), 16) + "";
                        index13 = index15 + 1 + 1;
                        break;
                    case 8:
                        int index16 = index13 + 1;
                        data.m_bHydrogenType = scanRecord[index13];
                        data.m_sHydrogen = Integer.parseInt(BleUtils.toHexString(new byte[]{scanRecord[index16], scanRecord[index16 + 1]}), 16) + "";
                        index13 = index16;
                        break;
                    case 9:
                        int index17 = index13 + 1;
                        data.m_bMethaneType = scanRecord[index13];
                        index13 = index17 + 1;
                        data.m_sMethane = BleUtils.convertByteToInt(scanRecord[index17]) + "";
                        break;
                }
            }
        }


        return data;
    }

    public boolean isEmergency() {
        byte b = this.m_bEmergency;
        if (81 == b || 82 == b) {
            return true;
        }
        return false;
    }

    public byte getByteEmergency() {
        return this.m_bEmergency;
    }

    public String getTxPowerType() {
        byte b = this.m_bTxpowerType;
        if (8 == b) {
            return "TX_POWER_NORMAL";
        }
        if (9 == b) {
            return "TX_POWER_SHORT";
        }
        return "TX_POWER_LONG";
    }

    public int getTxPower() {
        return BleUtils.convertByteToInt(this.m_bTxpower);
    }

    public byte getByteTemperatureState() {
        return this.m_bTemeratureType;
    }

    public String getTemperature() {
        return this.m_sTemperature;
    }

    public byte getByteHumidityState() {
        return this.m_bHumidityType;
    }

    public String getHumidity() {
        return this.m_sHumidity;
    }

    public String getBatteryState() {
        byte b = this.m_bBatteryType;
        if (b == 32) {
            return "Normal";
        }
        if (b == 33) {
            return "Low";
        }
        if (b == 39) {
            return "상시전원";
        }
        return "N/A";
    }

    public byte getByteBatteryState() {
        return this.m_bBatteryType;
    }

    public int getBattery() {
        return BleUtils.convertByteToInt(this.m_bBattery);
    }

    public String getVibrationState() {
        byte b = this.m_bVibration;
        if (b == 40) {
            return "Phase 1";
        }
        if (b == 41) {
            return "Phase 2";
        }
        if (b == 42) {
            return "Phase 3";
        }
        return "N/A";
    }

    public boolean isO2SensorEnabled() {
        if (this.m_bOxygenType == 0) {
            return false;
        }
        return true;
    }

    public String getO2State() {
        byte b = this.m_bOxygenType;
        if (b == 48) {
            return "일반";
        }
        if (b == 49) {
            return "1차 경보";
        }
        if (b == 50) {
            return "2차 경보";
        }
        if (b == 53) {
            return "센서 예열";
        }
        if (b == 54) {
            return "범버 테스트";
        }
        if (b == 55) {
            return "3차 경보";
        }
        return "N/A";
    }

    public byte getByteO2State() {
        return this.m_bOxygenType;
    }

    public String getO2Live() {
        return this.m_sOxygen;
    }

    public boolean isCOSensorEnabled() {
        if (this.m_bCarbonType == 0) {
            return false;
        }
        return true;
    }

    public String getCOState() {
        byte b = this.m_bCarbonType;
        if (b == 56) {
            return "일반";
        }
        if (b == 57) {
            return "1차 경보";
        }
        if (b == 58) {
            return "2차 경보";
        }
        if (b == 59) {
            return "STEL 경보";
        }
        if (b == 60) {
            return "TWA 경보";
        }
        if (b == 61) {
            return "센서 예열";
        }
        if (b == 62) {
            return "범버 테스트";
        }
        return "N/A";
    }

    public byte getByteCOState() {
        return this.m_bCarbonType;
    }

    public String getCOLive() {
        return this.m_sCarbon;
    }

    public boolean isCH4SensorEnabled() {
        if (this.m_bMethaneType == 0) {
            return false;
        }
        return true;
    }

    public String getCH4State() {
        byte b = this.m_bMethaneType;
        if (b == 72) {
            return "일반";
        }
        if (b == 73) {
            return "1차 경보";
        }
        if (b == 74) {
            return "2차 경보";
        }
        if (b == 77) {
            return "센서 예열";
        }
        if (b == 78) {
            return "범버 테스트";
        }
        return "N/A";
    }

    public byte getByteCH4State() {
        return this.m_bMethaneType;
    }

    public String getCH4Live() {
        return this.m_sMethane;
    }

    public boolean isH2SSensorEnabled() {
        if (this.m_bHydrogenType == 0) {
            return false;
        }
        return true;
    }

    public String getH2SState() {
        byte b = this.m_bHydrogenType;
        if (b == 64) {
            return "일반";
        }
        if (b == 65) {
            return "1차 경보";
        }
        if (b == 66) {
            return "2차 경보";
        }
        if (b == 67) {
            return "STEL 경보";
        }
        if (b == 68) {
            return "TWA 경보";
        }
        if (b == 69) {
            return "센서 예열";
        }
        if (b == 70) {
            return "범버 테스트";
        }
        return "N/A";
    }

    public byte getByteH2SState() {
        return this.m_bHydrogenType;
    }

    public String getH2SLive() {
        return this.m_sHydrogen;
    }
}

