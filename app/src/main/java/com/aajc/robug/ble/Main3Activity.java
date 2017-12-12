package com.aajc.robug.ble;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aajc.robug.R;
import com.aajc.robug.preference.PrefRW;

import java.util.ArrayList;

public class Main3Activity extends AppCompatActivity {

    private static final int SCAN_TIME = 5000;
    private final int REQUEST_ENABLE_BT = 100;
    private final int REQUEST_LOCATION = 101;
    private final int PERMISSION_REQUEST_INTENT = 102;
    private LinearLayout scannedDeviceLayout, newDeviceLayout;
    private boolean mScanning = false;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mBluetoothDevices = new ArrayList<BluetoothDevice>();

    private Handler mHandler; //該Handler用來搜尋Devices10秒後，自動停止搜尋
    private String TAG = "main3";
    /**
     * 建立一個BLAdapter的Callback，當使用startLeScan或stopLeScan時，每搜尋到一次設備都會跳到此callback
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() { //使用runOnUiThread方法，其功能等同於WorkThread透過Handler將資訊傳到MainThread(UiThread)中，
                //詳細可進到runOnUiThread中觀察
                @Override
                public void run() {
                    if (!mBluetoothDevices.contains(device)) { //利用contains判斷是否有搜尋到重複的device
                        mBluetoothDevices.add(device);         //如沒重複則添加到bluetoothdevices中
//                        deviceName.add(device.getName() + " rssi:" + rssi + "\r\n" + device.getAddress()); //將device的Name、rssi、address裝到此ArrayList<Strin>中
//                        ((BaseAdapter) listAdapter).notifyDataSetChanged();//使用notifyDataSetChanger()更新listAdapter的內容
                        setViews();
                    }
                }
            });
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        scannedDeviceLayout = findViewById(R.id.scannedDeviceLayout);
        newDeviceLayout = findViewById(R.id.newDeviceLayout);

        getSupportActionBar().setTitle(R.string.scanning_activity_title);

        if (!getPackageManager().hasSystemFeature(getPackageManager().FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getBaseContext(), "ble not support", Toast.LENGTH_SHORT).show();
            finish();
        }

        permissionProcess();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.main_menu, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_alarm:
                ScanFunction(true);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setViews() {
        scannedDeviceLayout.removeAllViews();
        newDeviceLayout.removeAllViews();
        for (BluetoothDevice device : mBluetoothDevices) {
            // 已讀取過
            if (!PrefRW.read(this, device.getAddress()).equals("")) {
                scannedDeviceLayout.addView(new DeviceRowView(this, device.getName(), device.getAddress(), new DeviceRowView.iDeviceRowView() {
                    @Override
                    public void rowClicked(String name, String address) {
                        Intent in = new Intent(Main3Activity.this, RobugControlActivity4.class);
                        in.putExtra("name", name);
                        in.putExtra("address", address);

                        // save id to pref
                        PrefRW.write(Main3Activity.this, address, "exist");

                        startActivity(in);
                    }
                }));
            } else {
                // 還沒讀取過
                newDeviceLayout.addView(new DeviceRowView(this, device.getName(), device.getAddress(), new DeviceRowView.iDeviceRowView() {
                    @Override
                    public void rowClicked(String name, String address) {
                        Intent in = new Intent(Main3Activity.this, RobugControlActivity4.class);
                        in.putExtra("name", name);
                        in.putExtra("address", address);

                        // save id to pref
                        PrefRW.write(Main3Activity.this, address, "exist");

                        startActivity(in);
                    }
                }));
            }
        }
        if (scannedDeviceLayout.getChildCount() == 0)
            scannedDeviceLayout.addView(createNoneDeviceView());
        if (newDeviceLayout.getChildCount() == 0)
            newDeviceLayout.addView(createNoneDeviceView());
    }

    private void permissionProcess() {
        mBluetoothManager = (BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // permission check
        if (!permissionCheck())
            return;
        // 初始Beacon連線
        if (!bleInit())
            return;

        bleInitProcess();
    }

    private void bleInitProcess() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(getBaseContext(), "no bluetooth", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mHandler = new Handler();

        ScanFunction(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
    }

    private void ScanFunction(boolean enable) {
        if (enable && !mScanning) {
            mBluetoothDevices.clear();
            scannedDeviceLayout.removeAllViews();
            newDeviceLayout.removeAllViews();
            scannedDeviceLayout.addView(createNoneDeviceView());
            newDeviceLayout.addView(createNoneDeviceView());

            mHandler.postDelayed(new Runnable() { //啟動一個Handler，並使用postDelayed在10秒後自動執行此Runnable()
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);//停止搜尋
                    mScanning = false; //搜尋旗標設為false
                    Log.d(TAG, "ScanFunction():Stop Scan");
                }
            }, SCAN_TIME); //SCAN_TIME為幾秒後要執行此Runnable，此範例中為10秒
            mScanning = true; //搜尋旗標設為true
            mBluetoothAdapter.startLeScan(mLeScanCallback);//開始搜尋BLE設備
            Log.d(TAG, "Start Scan");
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause():Stop Scan");
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    private TextView createNoneDeviceView() {
        TextView info = new TextView(this);
        info.setText(getString(R.string.ble_device_not_fount));
        info.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        int l = getResources().getDimensionPixelOffset(R.dimen.size_l);
        info.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.sp_s));
        info.setPadding(l, l, l, l);
        info.setGravity(Gravity.CENTER);
        return info;
    }

    /**
     * 5.0 檢查藍芽權限
     */
    @TargetApi(Build.VERSION_CODES.M)
    public boolean permissionCheck() {
        String permissions[] = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (!hasPermissions(permissions)) {
            this.requestPermissions(permissions,
                    PERMISSION_REQUEST_INTENT); //Any number
            return false;
        }
        return true;
    }

    public boolean hasPermissions(String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * bluetooth le 初始
     */
    public boolean bleInit() {
        System.out.println("bleInit");
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "device not support ble", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bm.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }

        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (service != null && !service.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert);
            else
                builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.permission_locationRequestTitle)).setMessage(getString(R.string.permission_locationRequestContent));
            builder.setNegativeButton(getString(R.string.permission_locationCancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.setPositiveButton(getString(R.string.permission_locationEnable), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, REQUEST_LOCATION);
                }
            });
            builder.show();

            return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("on activity result");
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK)
                    permissionProcess();
                break;
            case REQUEST_LOCATION:
                permissionProcess();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        boolean allPermissionGranted = true;
        for (int i = 0; i < grantResults.length; i++)
            if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                allPermissionGranted = false;

        if (requestCode == PERMISSION_REQUEST_INTENT) {
            if (allPermissionGranted)
                permissionProcess();
            else {
                finish();
            }
        }
    }

}
