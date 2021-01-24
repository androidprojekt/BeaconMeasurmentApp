package com.example.test_beaconapp;

import java.util.ArrayList;

public class Transmitter {

    private String macAdress, name, lastUpdate, type;
    private int rssi, samplesIterator;
    private boolean savingSamples;
    private ArrayList<Integer> samplesTab;

    public Transmitter(String macAdress, String lastUpdate, int rssi, String type) {
        this.macAdress = macAdress;
        this.lastUpdate = lastUpdate;
        this.rssi = rssi;
        this.type = type;
        this.name = "default";
        this.savingSamples =true;
        this.samplesTab = new ArrayList<>();
        this.samplesIterator=0;
    }
    public Transmitter (String macAdress, String lastUpdate, int rssi, String type, String name)
    {
        this.macAdress = macAdress;
        this.lastUpdate = lastUpdate;
        this.rssi = rssi;
        this.type = type;
        this.name = name;
        this.savingSamples =true;
        this.samplesTab = new ArrayList<>();
        this.samplesIterator=0;
    }



    public void addToTheSamplesTab(int sample)
    {
        samplesTab.add(sample);
    }


    public ArrayList<Integer> getSamplesTab() {
        return samplesTab;
    }

    public void setSamplesTab(ArrayList<Integer> samplesTab) {
        this.samplesTab = samplesTab;
    }

    public void clearTheSamplesTab()
    {
        samplesTab.clear();
    }
    public boolean isSavingSamples() {
        return savingSamples;
    }

    public void setSavingSamples(boolean savingSamples) {
        this.savingSamples = savingSamples;
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

    public int getSamplesIterator() {
        return samplesIterator;
    }

    public void setSamplesIterator() {
        this.samplesIterator++;
    }

    public void clearSamplesIterator(){this.samplesIterator=0;}
}
