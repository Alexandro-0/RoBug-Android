package com.aajc.robug.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.aajc.robug.R;
import com.aajc.robug.ble.ControlActivityTemplate;

/**
 * Created by Alexandro on 2017/12/12.
 */

public class RobugControlActivity extends ControlActivityTemplate {

    public String deviceName, deviceAddress;
    public RelativeLayout controlLayout;
    public LinearLayout loadingLayout;

    /**
     * 進入activity 是否自動連接
     */
    @Override
    public boolean autoConnect() {
        return true;
    }

    /**
     * 設要連接的mac address
     */
    @Override
    public String getMacAddress() {
        return deviceAddress;
    }

    /**
     * 連線到指定device完成
     */
    @Override
    public void deviceConnected() {
        // 如果在 ACTION_GATT_CONNECTED 設，characteristic 會有null的問題
        loadingLayout.setVisibility(View.GONE);
        controlLayout.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deviceAddress = getIntent().getStringExtra("address");
        deviceName = getIntent().getStringExtra("name");

        if (deviceAddress == null || deviceName == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_robug_control);

        controlLayout = findViewById(R.id.controlLayout);
        loadingLayout = findViewById(R.id.loadingLayout);

        getSupportActionBar().setTitle(deviceName);
    }

    public void onStopMoving(View view) {
        // 1-左  2-後左    3-右轉    4-左轉
        sendDataToBLE("0");
    }

    public void onMoveForward(View view) {
        // 1-左  2-後左    3-右轉    4-左轉
        sendDataToBLE("1");
    }

    public void onLeft(View view) {
        // 1-左  2-後左    3-右轉    4-左轉
        sendDataToBLE("4");
    }

    public void onRight(View view) {
        // 1-左  2-後左    3-右轉    4-左轉
        sendDataToBLE("3");
    }

    public void onBackward(View view) {
        // 1-左  2-後左    3-右轉    4-左轉
        sendDataToBLE("2");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.control_menu, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_close:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
