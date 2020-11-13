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
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_PHONE_STATE;

import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice;
import uk.co.alt236.bluetoothlelib.device.beacon.BeaconType;
import uk.co.alt236.bluetoothlelib.device.beacon.BeaconUtils;
import uk.co.alt236.bluetoothlelib.device.beacon.ibeacon.IBeaconDevice;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //------------------creating variables and objects---------------------
    private ListView listViewBeacon, listViewWifi;
    TextView magnetometerSensorValueTv, directionCompassTv;
    private Button scanBtn, saveToDatabaseBtn;
    private BluetoothManager mBluetoothManager;
    BluetoothLeScanner mBluetoothLeScanner;
    private Context context;
    private Handler mHandler = new Handler();
    private ArrayList<Transmitter> beaconList, wifiList;
    private BluetoothAdapter mBlueToothAdapter;
    private Calendar calendar;
    private SimpleDateFormat simpleDateFormat;
    private WifiInfo wifiInfo;
    private WifiManager wifiManager;
    private BeaconAndWifiListAdapter adapterBle, adapterWifi;
    static public SensorManager mSensorManager;
    static List<Sensor> SensorList;
    private Sensor magneticFieldSensor;
    double magnetometerValue;

    //-----------------------------variables used to autoincrement in database--------------------
    long maxid = 0;
    long maxidWifi = 0;
    long maxidbeacon2 = 0;
    long maxidMagnetometer = 0;
    //--------------------------------------------------------------------------------------------
    //------------------a flags specifying if still collect data into database--------------------
    Boolean flagMagnetometer = false;
    Boolean flagWifi = false;
    Boolean flagBle = false;
    Boolean flagBle2 = false;
    //--------------------------------------------------------------------------------------------
    //-----------------------------max number of samples in database and iterators----------------
    int numberOfSamples = 200;
    int magnetometerDatabaseIterator = 0;
    int wifiDatabaseIterator = 0;
    int bleDatabaseIterator = 0;
    int ble2DatabaseIterator = 0;
    //---------------------------------------------------------------------------------------------
    //------------------------------variables needed to compass------------------------------------
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    int azimuth =0;
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    //---------------------------------------------------------------------------------------------

    DatabaseReference beaconReference;
    DatabaseReference beaconReference2;
    DatabaseReference magnetometerReference;
    DatabaseReference wifiReference;
//---------------------------------------------------------------------------

    List<ScanFilter> filters;
    ScanSettings scanSettings;


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
        saveToDatabaseBtn = findViewById(R.id.saveToDatabeseBtnId);
        //magnetometerSensorValueTv = findViewById(R.id.sensorTvId);
        directionCompassTv = findViewById(R.id.directionCompassId);
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
        Transmitter transmitterWifi = new Transmitter(wifiInfo.getMacAddress(), simpleDateFormat.format(calendar.getTime()), wifiInfo.getRssi(), "Wifi", wifiInfo.getSSID());
        wifiList.add(transmitterWifi);
        listViewWifi.setAdapter(adapterWifi);
        //---------------------------------------------------------

        //-----------------------Sensor----------------------------
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        SensorList = mSensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
        magneticFieldSensor = SensorList.get(0);
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
                flagBle = true;
                flagMagnetometer = true;
                flagWifi = true;
                flagBle2 = true;
            }
        });

        //-------------------------Database Settings------------------------------------------------
        beaconReference = FirebaseDatabase.getInstance().getReference().child("beacon1 Values");
        beaconReference2 = FirebaseDatabase.getInstance().getReference().child("beacon2 Values");
        wifiReference = FirebaseDatabase.getInstance().getReference().child("wifi Values");
        magnetometerReference = FirebaseDatabase.getInstance().getReference().child("Magnetometer Values");
        //------------------------------------------------------------------------------------------

        //--------------------Settings and filters for scanning bluetooth devices-------------------
        String[] peripheralAddresses = new String[]{"C6:40:D6:9C:59:7E", "E8:D4:18:0D:DB:37", "DB:A8:FF:3E:95:79",
                "C9:52:36:05:C4:12", "78:BD:BC:70:77:1F"};
// Build filters list
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
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setReportDelay(0L)
                .build();
    }
    //----------------------------------------------------------------------------------------------

    //-----------------------------------------Threads----------------------------------------------
    private Runnable BLEstopScan = new Runnable() {

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            //mBlueToothAdapter.stopLeScan(mLeScanCallback); old solution
            mBluetoothLeScanner.stopScan(scanCallback);
            mHandler.postDelayed(BLEstartScan, 100);
        }
    };

    private Runnable BLEstartScan = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            //start scanning Beacons or BLE devices
            //mBlueToothAdapter.startLeScan(mLeScanCallback); old solution
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
            mHandler.postDelayed(wifiScanner, 1000);
        }
    };
//--------------------------------------------------------------------------------------------------

    //------------------------------Updating WiFi information-------------------------------------------
    private void checkWifi() {
        wifiInfo = wifiManager.getConnectionInfo();
        wifiList.get(0).setRssi(wifiInfo.getRssi());
        wifiList.get(0).setLastUpdate(simpleDateFormat.format(calendar.getTime()));

        if (flagWifi) {
            //a flag specifying if still collect data into database
            if (wifiDatabaseIterator == numberOfSamples) {
                flagWifi = false;
                Toast.makeText(getApplicationContext(), "STOP", Toast.LENGTH_SHORT).show();
            }
            wifiReference.child(String.valueOf(++maxidWifi)).setValue(wifiInfo.getRssi());
            wifiDatabaseIterator++;
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
                    /*
                    if (beaconList.size() == 1) {
                        //we limit the list to two items (because we have only 2 beacons)
                        if (beaconList.get(0).getMacAdress().contains(device.getAddress())) {
                            //overwriting the values of a given beacon
                            beaconList.get(0).setLastUpdate(simpleDateFormat.format(calendar.getTime()));
                            beaconList.get(0).setRssi(rssi);

                            if (flagBle) {
                                //a flag specifying if still collect data into database
                                if (bleDatabaseIterator == numberOfSamples) {
                                    flagBle = false;
                                    Toast.makeText(getApplicationContext(), "STOP", Toast.LENGTH_SHORT).show();
                                }
                                beaconReference.child(String.valueOf(++maxid)).setValue(rssi);
                                bleDatabaseIterator++;
                            }
                        }
                        listViewBeacon.setAdapter(adapterBle);

                    } else {
                        //filling the list with two beacons

                        Transmitter transmitter = new Transmitter(device.getAddress(), simpleDateFormat.format(calendar.getTime()), rssi, "Beacon");
                        beaconList.add(transmitter);

                        listViewBeacon.setAdapter(adapterBle);
                    }*/
                }
            });

        }
    };

    //onlescan
 /* // Old Solution for scanning bluetooth devices
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {


            final BluetoothLeDevice deviceLe = new BluetoothLeDevice(device, rssi, scanRecord, System.currentTimeMillis());  //found BLE device (beacon, phone or something else)
            context = getApplicationContext();

            if (BeaconUtils.getBeaconType(deviceLe) == BeaconType.IBEACON) {
                //we only consider beacons
                final IBeaconDevice iBeacon = new IBeaconDevice(deviceLe);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        calendar = Calendar.getInstance();
                        if (beaconList.size() == 2) {
                            //we limit the list to two items (because we have only 2 beacons)
                            if (beaconList.get(0).getMacAdress().contains(iBeacon.getAddress())) {
                                //overwriting the values of a given beacon
                                beaconList.get(0).setLastUpdate(simpleDateFormat.format(calendar.getTime()));
                                beaconList.get(0).setRssi(iBeacon.getRssi());

                                if(flagBle) {
                                    //a flag specifying if still collect data into database
                                    if(bleDatabaseIterator == numberOfSamples) {
                                        flagBle = false;
                                        Toast.makeText(getApplicationContext(), "STOP", Toast.LENGTH_SHORT).show();
                                    }
                                    beaconReference.child(String.valueOf(++maxid)).setValue(iBeacon.getRssi());
                                    bleDatabaseIterator++;
                                             }
                            } else {
                                //overwriting the value of a given beacon
                                beaconList.get(1).setLastUpdate(simpleDateFormat.format(calendar.getTime()));
                                beaconList.get(1).setRssi(iBeacon.getRssi());
                                if(flagBle2) {
                                    //a flag specifying if still collect data into database
                                    if(ble2DatabaseIterator == numberOfSamples) {
                                        flagBle2 = false;
                                        Toast.makeText(getApplicationContext(), "STOP", Toast.LENGTH_SHORT).show();
                                    }
                                    beaconReference2.child(String.valueOf(++maxidbeacon2)).setValue(iBeacon.getRssi());
                                    ble2DatabaseIterator++;
                                }
                            }
                            listViewBeacon.setAdapter(adapterBle);

                        } else {
                            //filling the list with two beacons

                            Transmitter transmitter = new Transmitter(iBeacon.getAddress(), simpleDateFormat.format(calendar.getTime()), iBeacon.getRssi(), "Beacon");
                            beaconList.add(transmitter);
                            Log.i("TEST_UUID", "uuid: " + iBeacon.getUUID());

                            if (beaconList.size() == 2 && beaconList.get(0).getMacAdress().contains(transmitter.getMacAdress())) {
                                //preventing the repetition of beacons on the list
                                beaconList.remove(1);
                            }
                            listViewBeacon.setAdapter(adapterBle);
                        }
                    }
                });
            }
        }
    };
*/

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        String update = simpleDateFormat.format(calendar.getTime());

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(sensorEvent.values, 0, accelerometerReading,
                    0, accelerometerReading.length);
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(sensorEvent.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
        }

        float x = Math.round(magnetometerReading[0]);
        float y = Math.round(magnetometerReading[1]);
        float z = Math.round(magnetometerReading[2]);
        magnetometerValue = Math.sqrt((x * x) + (y * y) + (z * z));
        //magnetometerSensorValueTv.setText("Magnetometer Value: " + magnetometerValue + " [uT]" + "\nlast update: " + update);

        if (flagMagnetometer) {
            //a flag specifying if still collect data into database
            if (magnetometerDatabaseIterator == numberOfSamples) {
                flagMagnetometer = false;
                Toast.makeText(getApplicationContext(), "STOP", Toast.LENGTH_SHORT).show();
            }
            magnetometerReference.child(String.valueOf(++maxidMagnetometer)).setValue(magnetometerValue);
            magnetometerDatabaseIterator++;
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

}
