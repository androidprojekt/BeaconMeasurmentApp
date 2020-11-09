package com.example.test_beaconapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class BeaconAndWifiListAdapter extends ArrayAdapter<Transmitter> {
    private Context mContext;
            int mResource;
            private  static final String TAG = "BeaconListAdapter";

            public BeaconAndWifiListAdapter(Context context, int resource,  List<Transmitter> objects) {
        super(context, resource, objects);
        this.mContext = context;
        mResource = resource;
    }


    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String macAdress = getItem(position).getMacAdress();
        String lastUpdate = getItem(position).getLastUpdate();
        String type = getItem(position).getType();
        String name = getItem(position).getName();
        int rssi = getItem(position).getRssi();

        Transmitter transmitter = new Transmitter(macAdress,lastUpdate,rssi,type);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource,parent,false);

        TextView tvMac = convertView.findViewById(R.id.macTvId);
        TextView tvUpdate = convertView.findViewById(R.id.lastUpdateTvId);
        TextView tvRssi = convertView.findViewById(R.id.rssiTvId);
        TextView tvType = convertView.findViewById(R.id.typeTvId);

        if(type=="Wifi")
        {
            tvMac.setText("Wifi Name: " + name);
        }
        else
        {
            tvMac.setText("Mac Adress: " + macAdress);
        }


        tvUpdate.setText("Last Update: " + lastUpdate);
        tvRssi.setText("RSSI: " + String.valueOf(rssi));
        tvType.setText("Type: " + type);
        return convertView;
    }
}

