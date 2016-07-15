package com.adityapradhan.bluetoothtutorialspoint;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    Button turnBtOnButton, turnBtOffButton, listDevicesButton, getVisibleDevicesButton;
    ListView listView;
    private BluetoothAdapter BA;
    private Set<BluetoothDevice> pairedDevices;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize button objects
        turnBtOnButton = (Button) findViewById(R.id.btnTurnBluetoothOn);
        turnBtOffButton = (Button) findViewById(R.id.btnTurnBluetoothOff);
        listDevicesButton = (Button) findViewById(R.id.btnListPairedDevices);
        getVisibleDevicesButton = (Button) findViewById(R.id.btnGetVisibleDevices);

        // initialize bluetooth adapter object
        BA = BluetoothAdapter.getDefaultAdapter();

        listView = (ListView) findViewById(R.id.listView);

    }

    // button onClick methods

    public void on(View v) {
        // turn on bluetooth

        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Bluetooth Turned On", Toast.LENGTH_LONG).show();
        }
    }

    public void off(View v) {
        // turn off bluetooth
        BA.disable();
        Toast.makeText(getApplicationContext(), "Bluetooth Turned Off", Toast.LENGTH_LONG).show();
    }

    public void list(View v) {
        // view paired devices
        pairedDevices = BA.getBondedDevices();
        ArrayList list = new ArrayList();

        for (BluetoothDevice device : pairedDevices) {
            list.add(device.getName() + "\n" + device.getAddress());
        }
        Toast.makeText(getApplicationContext(), "Showing Paired Devices", Toast.LENGTH_SHORT).show();

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);


    }

    public void visible(View v) {
        // view visible devicess
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
    }


}
