package com.example.test_beaconapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.sql.Struct;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_PHONE_STATE;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //------------------creating variables and objects---------------------
    private ListView listViewBeacon, listViewWifi;
    private TextView  directionCompassTv, xCordinateTv, yCordinateTv;
    private Button scanBtn, saveToDatabaseBtn, resetBtn;
    private BluetoothManager mBluetoothManager;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothAdapter mBlueToothAdapter;
    private Context context;
    private Handler mHandler = new Handler();
    private ArrayList<Transmitter> beaconList, wifiList;
    private Calendar calendar;
    private SimpleDateFormat simpleDateFormat;
    private WifiInfo wifiInfo;
    private WifiManager wifiManager;
    private BeaconAndWifiListAdapter adapterBle, adapterWifi;
    static public SensorManager mSensorManager;
    private Boolean startSaveToDatabaseFlag = false;
    private int numberOfSamples = 20; //max number of samples in database and iterators
    private DatabaseReference mDatabaseReference;
    private List<ScanFilter> filters;
    private ScanSettings scanSettings;
    RadioGroup radiogroup;
    RadioButton radioButton;
    String directionInDatabase = "UP";
    int xCordinate=0;
    int yCordinate=0;

    //------------------------------variables needed to compass------------------------------------
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    int azimuth = 0;
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    //---------------------------------------------------------------------------------------------

//---------------------------------------------------------------------------


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        calendar = Calendar.getInstance();
        simpleDateFormat = new SimpleDateFormat("hh:mm:ss a");
        context = getApplicationContext();
        //--------------------Views---------------------
        listViewBeacon = findViewById(R.id.listView);
        listViewWifi = findViewById(R.id.listViewWifi);
        scanBtn = findViewById(R.id.buttonId);
        resetBtn = findViewById(R.id.resetBtnId);
        xCordinateTv = findViewById(R.id.xCordinateId);
        yCordinateTv=findViewById(R.id.yCordinateId);
        saveToDatabaseBtn = findViewById(R.id.saveToDatabeseBtnId);
        directionCompassTv = findViewById(R.id.directionCompassId);
        radiogroup = findViewById(R.id.radioGroupId);
        //-----------------------------------------------

        //--------------------Bluetooth and Wifi-------------------
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothLeScanner = mBlueToothAdapter.getBluetoothLeScanner(); // new solution for scanning
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        beaconList = new ArrayList<>();
        wifiList = new ArrayList<>();
        adapterBle = new BeaconAndWifiListAdapter(context, R.layout.adapter_view_layout, beaconList);
        adapterWifi = new BeaconAndWifiListAdapter(context, R.layout.adapter_view_layout, wifiList);
        wifiInfo = wifiManager.getConnectionInfo(); //actual connected AP
        Transmitter transmitterWifi = new Transmitter(wifiInfo.getMacAddress(), simpleDateFormat.format(calendar.getTime()),
                wifiInfo.getRssi(), "Wifi", wifiInfo.getSSID());
        wifiList.add(transmitterWifi);
        listViewWifi.setAdapter(adapterWifi);
        //---------------------------------------------------------

        //-----------------------Sensor----------------------------
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //----------------------------------------------------------

        //-------------------------PERMISSIONS-------------------------------
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        READ_PHONE_STATE,
                        ACCESS_FINE_LOCATION},
                1);
        //--------------------------------------------------------------------

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiScanner.run();
                BLEstartScan.run();
            }
        });

        saveToDatabaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSaveToDatabaseFlag = true;
                String xValueStr =String.valueOf(xCordinateTv.getText());
                xCordinate=Integer.parseInt(xValueStr);

                String yValueStr =String.valueOf(yCordinateTv.getText());
                yCordinate=Integer.parseInt(yValueStr);
            }
        });

        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            resettingFunction();
            }
        });

        //-------------------------Database Settings------------------------------------------------
        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child(directionInDatabase);
        //------------------------------------------------------------------------------------------

        //--------------------Settings and filters for scanning bluetooth devices-------------------
        String[] peripheralAddresses = new String[]{"C6:40:D6:9C:59:7E", "E8:D4:18:0D:DB:37", "DB:A8:FF:3E:95:79",
                 "D6:2E:C2:40:FD:03","F7:8B:72:B7:42:C4", "C1:90:8E:4B:16:E5"};
        //Beacon F7:8B:72:B7:42:C4 jest chyba uszkodzony
        filters = null;
        if (peripheralAddresses != null) {
            filters = new ArrayList<>();
            for (String address : peripheralAddresses) {
                ScanFilter filter = new ScanFilter.Builder()
                        .setDeviceAddress(address)
                        .build();
                filters.add(filter);
            }
        }
        scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setReportDelay(0)
                .build();

    }
    //----------------------------------------------------------------------------------------------

    //-----------------------------------------Threads----------------------------------------------
    private Runnable BLEstopScan = new Runnable() {

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            mBluetoothLeScanner.stopScan(scanCallback);
            mHandler.postDelayed(BLEstartScan, 1);
        }
    };

    private Runnable BLEstartScan = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            mBluetoothLeScanner.startScan(filters, scanSettings, scanCallback);
            mHandler.postDelayed(BLEstopScan, 6000);
        }
    };

    private Runnable wifiScanner = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            checkWifi();
            //This line will continuously call this Runnable with 1000 milliseconds gap
            mHandler.postDelayed(wifiScanner, 500);
        }
    };
//--------------------------------------------------------------------------------------------------

    //------------------------------Updating WiFi information-------------------------------------------
    private void checkWifi() {
        wifiInfo = wifiManager.getConnectionInfo();
        wifiList.get(0).setRssi(wifiInfo.getRssi());
        wifiList.get(0).setLastUpdate(simpleDateFormat.format(calendar.getTime()));

        if (startSaveToDatabaseFlag) {
            if (wifiList.get(0).isSavingSamples()) {
                //a flag specifying if still collect data into database
                if (wifiList.get(0).getSamplesIterator() == numberOfSamples) {
                    wifiList.get(0).setSavingSamples(false);
                    String str = "av of: "+ wifiList.get(0).getMacAdress()+": ";
                    double average = averageOfList(wifiList.get(0).getSamplesTab());
                    String temp = xCordinate +"," + yCordinate;
                    mDatabaseReference.child(temp).child("WIFI").setValue(average);
                    Log.d("AVERAGE" ,str+average);
                    Toast.makeText(getApplicationContext(), "Wifi samples upload success!", Toast.LENGTH_SHORT).show();
                }
                else {
                    wifiList.get(0).addToTheSamplesTab(wifiList.get(0).getRssi());
                    wifiList.get(0).setSamplesIterator();
                }
            }
        }
        listViewWifi.setAdapter(adapterWifi);
    }
//--------------------------------------------------------------------------------------------------


    @RequiresApi(api = Build.VERSION_CODES.N)
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);

            final BluetoothDevice device = result.getDevice();
            final int rssi = result.getRssi();

            //Toast.makeText(getApplicationContext(), "Number of results" +uuidList.size(), Toast.LENGTH_SHORT).show();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    calendar = Calendar.getInstance();
                    if (beaconList.size() != 0) {
                        boolean newBeacon = true;
                        for (Transmitter transmitter : beaconList) {
                            if (transmitter.macAdress.contains(result.getDevice().getAddress())) {
                                newBeacon = false;
                                transmitter.setRssi(rssi);
                                transmitter.setLastUpdate(simpleDateFormat.format(calendar.getTime()));

                                if (startSaveToDatabaseFlag) {
                                    if (transmitter.isSavingSamples()) {
                                        if (transmitter.getSamplesIterator() == numberOfSamples) {
                                            transmitter.setSavingSamples(false);
                                            String str = "av of: "+ transmitter.getMacAdress()+": ";
                                            double average = averageOfList(transmitter.getSamplesTab());
                                            Log.d("AVERAGE" ,str+average);
                                            String temp = xCordinate +"," + yCordinate;
                                            mDatabaseReference.child(temp).child(result.getDevice().getAddress()).setValue(average);
                                            Toast.makeText(getApplicationContext(), "Beacons samples upload success!", Toast.LENGTH_SHORT).show();
                                        }
                                        else {
                                            transmitter.addToTheSamplesTab(rssi);
                                            transmitter.setSamplesIterator();
                                        }
                                    }

                                }
                            }
                        }
                        if (newBeacon == true) {
                            Transmitter transmitter = new Transmitter(device.getAddress(), simpleDateFormat.format(calendar.getTime()), rssi, "Beacon");
                            beaconList.add(transmitter);
                            //Toast.makeText(getApplicationContext(), "DRUGI ", Toast.LENGTH_SHORT).show();
                        }
                        listViewBeacon.setAdapter(adapterBle);
                    } else {
                        Transmitter transmitter = new Transmitter(device.getAddress(), simpleDateFormat.format(calendar.getTime()), rssi, "Beacon");
                        beaconList.add(transmitter);
                        listViewBeacon.setAdapter(adapterBle);
                    }
                }
            });
        }
    };



    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        //String update = simpleDateFormat.format(calendar.getTime());
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(sensorEvent.values, 0, accelerometerReading,
                    0, accelerometerReading.length);
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(sensorEvent.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
        }

        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        // "rotationMatrix" now has up-to-date information.
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
        // "orientationAngles" now has up-to-date information.
        azimuth = (int) Math.toDegrees(orientationAngles[0]);
        azimuth = (azimuth + 360) % 360;
        directionCompassTv.setText("Direction value: " + azimuth + "°");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            mSensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        Sensor magneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            mSensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(BLEstartScan);
        mHandler.removeCallbacks(BLEstopScan);
        mHandler.removeCallbacks(wifiScanner);
        mSensorManager.unregisterListener(this);
    }

    public double averageOfList(ArrayList<Integer> list)
    {
        double average=0.0;
        int sum=0;
        for(int element:list)
        {
            sum+=element;
        }
        average = (double)sum/(list.size());
        return average;
    }

    public void resettingFunction()
    {
        startSaveToDatabaseFlag=false;
        for(Transmitter tx: beaconList)
        {
            tx.clearTheSamplesTab();
            tx.clearSamplesIterator();
            tx.setSavingSamples(true);
        }
        wifiList.get(0).clearSamplesIterator();
        wifiList.get(0).clearTheSamplesTab();
        wifiList.get(0).setSavingSamples(true);
    }

    public void checkRadioButton(View view) {
        int radioId = radiogroup.getCheckedRadioButtonId();
        radioButton= findViewById(radioId);
        directionInDatabase= String.valueOf(radioButton.getText());
        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child(directionInDatabase);
    }
}
