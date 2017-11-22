package com.aajc.robug.ble;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aajc.robug.R;

/**
 * Created by Alexandro on 2017/11/19.
 */

public class DeviceRowView extends LinearLayout {

    public DeviceRowView(Context context, final String name, final String address, final iDeviceRowView iEvent) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.scanned_device_row_view, this, true);

        LinearLayout rootLayout = (LinearLayout) getChildAt(0);
        LinearLayout secondLayout = (LinearLayout) rootLayout.getChildAt(1);

        TextView nameTx = (TextView) secondLayout.getChildAt(0);
        TextView addressTx = (TextView) secondLayout.getChildAt(1);

        nameTx.setText(name);
        addressTx.setText(address + " (Mac Address)");

        if (iEvent != null)
            rootLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    iEvent.rowClicked(name, address);
                }
            });
    }

    public interface iDeviceRowView {
        public void rowClicked(String name, String address);
    }
}
