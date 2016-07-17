package com.adityapradhan.bluetoothtutorialspoint;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button turnBtOnButton, turnBtOffButton, listDevicesButton, getVisibleDevicesButton;
    ListView listView;
    private BluetoothAdapter BA;
    private Set<BluetoothDevice> pairedDevices;
    BluetoothDevice mDevice;
    Handler mHandler;


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

        // turn on bluetooth
        if (!BA.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        // TODO remove test feature below
        pairedDevices = BA.getBondedDevices();


        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mDevice = device;
            }



        }
        // handler for reading / writing data from input/output streams
        mHandler = new Handler();

        ConnectThread connectThread = new ConnectThread(mDevice);
        connectThread.start();


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

    // interface with bluetooth module
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
            // get connected bluetooth devices UUID
            MY_UUID = mmDevice.getUuids()[0].getUuid();

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                Log.d("bluetooth status", "attempting to create RFCOMM socket");
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.d("bluetooth status", "RFCOMM socket created");
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            BA.cancelDiscovery();


            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            try {
                Log.d("bluetooth status", "attempting connection");
                mmSocket.connect();
                Log.d("device connected", "bluetooth connection successful");
            } catch (IOException connectException) {
                // Unable to connect. Close the socketand get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }

                return;
            }

            //manageConnectedSocket(mmSocket);

        }

        // Will cancel an in-progress connection, and close the socket
        public void cancel() {
            try {
                mmSocket.close();

            } catch (IOException closeException) {
            }
        }
    }
//
//    private class ConnectedThread extends Thread {
//        private final BluetoothSocket mmSocket;
//        private final InputStream mmInStream;
//        private final OutputStream mmOutStream;
//
//        public ConnectedThread(BluetoothSocket socket) {
//            mmSocket = socket;
//            InputStream tmpIn = null;
//            OutputStream tmpOut = null;
//
//            // Get the input and output streams, using temp objects because
//            // member streams are final
//            try {
//                tmpIn = socket.getInputStream();
//                tmpOut = socket.getOutputStream();
//            } catch (IOException e) {
//            }
//
//            mmInStream = tmpIn;
//            mmOutStream = tmpOut;
//        }
//
//        public void run() {
//            byte[] buffer = new byte[1024];  // buffer store for the stream
//            int bytes; // bytes returned from read()
//
//            // Keep listening to the InputStream until an exception occurs
//            while (true) {
//                try {
//                    // Read from the InputStream
//                    bytes = mmInStream.read(buffer);
//                    // Send the obtained bytes to the UI activity
//                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
//                            .sendToTarget();
//                } catch (IOException e) {
//                    break;
//                }
//            }
//
//        }
//
//        /* Call this from the main activity to send data to the remote device */
//        public void write(byte[] bytes) {
//            try {
//                mmOutStream.write(bytes);
//            } catch (IOException e) {
//            }
//        }
//
//        /* Call this from the main activity to shutdown the connection */
//        public void cancel() {
//            try {
//                mmSocket.close();
//            } catch (IOException e) {
//            }
//        }
//
//    }

}
