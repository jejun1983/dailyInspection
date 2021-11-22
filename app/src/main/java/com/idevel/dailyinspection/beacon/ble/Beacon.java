package com.idevel.dailyinspection.beacon.ble;

public class Beacon {
    public String address;
    public int rssi;
    public String now;

    public String id;
    public String name;
    public String uuid;
    public String workTime;
    public long time;
    public long firstTime;
    public boolean inout = false;


    public Beacon() {    }

    public Beacon(String address)
    {
        this.address = address;
    }

    public Beacon(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Beacon(String id, String name,String workTime)
    {
        this.id = id;
        this.name = name;
        this.workTime = workTime;
    }

}

