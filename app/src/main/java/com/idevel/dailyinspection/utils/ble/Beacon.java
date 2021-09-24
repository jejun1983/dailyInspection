package com.idevel.dailyinspection.utils.ble;

public class Beacon {
    public String address;
    public int rssi;
    public String now;

    public String id;
    public String name;
    public String placename;
    public String placecode;
    public String uuid;
    public String workTime;
    public long time;
    public long firstTime;
    public boolean inout = false;
    public String IWERK;
    public String FCL_ALIAS;

    public Beacon() {    }

    public Beacon(String address)
    {
        this.address = address;
    }

    public Beacon(String IWERK, String id, String name, String placecode, String placename) {
        this.IWERK = IWERK;
        this.id = id;
        this.name = name;
        this.placecode = placecode;
        this.placename = placename;
    }

    public Beacon(String IWERK, String id, String name, String placecode, String placename, String workTime, String FCL_ALIAS)
    {
        this.IWERK = IWERK;
        this.id = id;
        this.name = name;
        this.placecode = placecode;
        this.placename = placename;
        this.workTime = workTime;
        this.FCL_ALIAS = FCL_ALIAS;
    }

}

