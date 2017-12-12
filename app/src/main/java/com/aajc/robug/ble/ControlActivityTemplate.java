package com.aajc.robug.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by Alexandro on 2017/11/23.
 */

public abstract class ControlActivityTemplate extends AppCompatActivity {

    public final static UUID HM_RX_TX =
            UUID.fromString(SampleGattAttributes.HM_RX_TX);
    private final static String TAG = ControlActivityTemplate.class.getName();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    protected boolean mConnected = false;
    private BluetoothLeService mBluetoothLeService;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(getMacAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                System.out.println("track - connected");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState(R.string.disconnected);
                System.out.println("track - disconnected");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                System.out.println("track - discovered");
                displayGattServices(mBluetoothLeService.getSupportedGattServices());

                deviceConnected();
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    /**
     * 進入activity 是否自動連接
     */

    public abstract boolean autoConnect();

    /**
     * 設要連接的mac address
     */
    public abstract String getMacAddress();

    /**
     * 連線到指定device完成
     */
    public abstract void deviceConnected();

    /**
     * 傳送 data 到 device
     */
    public void sendDataToBLE(String str) {
        Log.d(TAG, "Sending result=" + str);
        final byte[] tx = str.getBytes();
        if (mConnected && characteristicTX != null) {
            characteristicTX.setValue(tx);
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
        }
    }

    public void startScanService() {
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (autoConnect()) {
            startScanService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null && getMacAddress() != null) {
            final boolean result = mBluetoothLeService.connect(getMacAddress());
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(mGattUpdateReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mBluetoothLeService.disconnect();
            unbindService(mServiceConnection);
            mBluetoothLeService = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = "unknown_service";
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();


        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));

            // If the service exists for HM 10 Serial, say so.
            if (SampleGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") {
                System.out.println("has HM series");
            } else {
                //System.out.println("Not hm series");
            }
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            // get characteristic when UUID matches RX/TX UUID
            characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
            characteristicRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
        }

    }
}
