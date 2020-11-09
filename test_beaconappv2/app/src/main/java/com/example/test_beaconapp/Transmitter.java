package com.example.test_beaconapp;

public class Transmitter {

    String macAdress;
    String lastUpdate;
    String type;
    String name;
    int rssi;



    public Transmitter(String macAdress, String lastUpdate, int rssi, String type) {
        this.macAdress = macAdress;
        this.lastUpdate = lastUpdate;
        this.rssi = rssi;
        this.type = type;
        this.name = "default";
    }


    public Transmitter (String macAdress, String lastUpdate, int rssi, String type, String name)
    {
        this.macAdress = macAdress;
        this.lastUpdate = lastUpdate;
        this.rssi = rssi;
        this.type = type;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMacAdress() {
        return macAdress;
    }

    public void setMacAdress(String macAdress) {
        this.macAdress = macAdress;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String name) {
        this.lastUpdate = name;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
