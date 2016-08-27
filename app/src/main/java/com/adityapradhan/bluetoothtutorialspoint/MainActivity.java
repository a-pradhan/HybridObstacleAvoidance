package com.adityapradhan.bluetoothtutorialspoint;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.NonSymmetricMatrixException;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import ucl.LightHouse.LightHouseAPI;

public class MainActivity extends AppCompatActivity {

    Button turnBtOnButton, turnBtOffButton, listDevicesButton, getVisibleDevicesButton, connectButton;
    ListView listView;

    private BluetoothAdapter BA;
    private Set<BluetoothDevice> pairedDevices;
    BluetoothDevice mDevice;
    ConnectThread connectThread;
    Handler bluetoothIn;
    final int handlerState = 0;
    private ObstacleKalmanFilter filter;
    private EventDetection eventDetection;
    int counter; // to keep track of no. of iterations

    private StringBuilder recDataString = new StringBuilder();
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("Info", "Start of Main Activity");
        final TextView receivedDataTextView = (TextView) findViewById(R.id.receivedDataTextView);
        final TextView filterEstimateTextView = (TextView) findViewById(R.id.filterEstimateTextView);
        final ReadingParser readingParser = new ReadingParser();

        // initialize handler object to receive messages sent by bluetooth module
        //TODO create method for string processing
        bluetoothIn = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == handlerState) {
                    // String obtained by reading from byte buffer
                    String readMessage = (String) msg.obj;
                    String readingString = readingParser.parseReadings(readMessage);



                    if (readingString != null) { // full set of readings received -> initialize/run appropriate filter
                       // Log.i("parsed reading" , readingString);
                        //Log.i("count", Integer.toString(counter++));
                        receivedDataTextView.setText(readingString);
                        // separate readings and arrange in a measurement vector
                        String[] splitStringReadings = readingString.split(",");
                        ArrayList<Double> splitDoubleReadings = new ArrayList<Double>(splitStringReadings.length + 1);

                        for(int i=0;i < splitStringReadings.length; i++) {
                            splitDoubleReadings.add(Double.parseDouble(splitStringReadings[i]));
                        }

                        if(filter == null) {
                            // initialize filter with first set of readings making up part of the state with assumed 0 velocity
                            splitDoubleReadings.add(0d);
                            Double[] stateArray = splitDoubleReadings.toArray(new Double[splitDoubleReadings.size()]);

                            RealVector initialState = new ArrayRealVector(stateArray);

                            filter = new ObstacleKalmanFilter(initialState);
                            Log.i("initialState used", initialState.toString());
                            Log.i("initial covariance", filter.getStateCovarianceMatrix().toString());
                            eventDetection = new EventDetection();
                            eventDetection.addStateEstimate(initialState);
                        } else {
                            // check if moving or not
                            RealVector[] previousEstimates = eventDetection.getStateEstimates();
                            boolean[] distanceChangedArray = eventDetection.getChangedDistanceIndex(previousEstimates);
                            boolean isMoved = false;

                            for(boolean changedDistance : distanceChangedArray) {
                                if(changedDistance == true){
                                    isMoved = true;
                                    break;
                                }
                            }


                            if(isMoved == true) {

                                // if moving reinstantiate filter -  set initial velocity to 1 m/s and use current state estimate and covariance matrix
                                Log.i("Movement", "movement detected reinitializing filter");
                                RealVector initialState = filter.getStateEstimationVector();
                                RealMatrix initialCovarianceMatrix = filter.getStateCovarianceMatrix();
                                initialState.setEntry(3, 1.0); // set velocity to desired value
                                filter = new ObstacleKalmanFilter(initialState, initialCovarianceMatrix);

                            }
                                counter++;
                                // perform update
                                RealVector measurements = new ArrayRealVector(splitDoubleReadings.toArray(new Double[splitDoubleReadings.size()]));
                                filter.predict();
                                //Log.i("filter prediction", filter.getStateEstimationVector().toString());

                                filter.correct(measurements);
                                filterEstimateTextView.setText(filter.getStateEstimationVector().toString());
                                eventDetection.addStateEstimate(filter.getStateEstimationVector());
                                if(counter % 5 == 0) {
                                    Log.i("Filter Estimate", splitStringReadings.toString());
                                }

                                //Log.i("state estimate", filter.getStateEstimationVector().toString());
                                //Log.i("state covariance", filter.getStateCovarianceMatrix().toString());


                        }













                        }











                        // check if slowed/accelerating/user turned and reinitialize Filter object with latest estimate and current covariance matrix;



                    }



 //                   int endOfLineIndex = recDataString.indexOf("\n");
//
//
//                    if(endOfLineIndex > 0) {
//                        String dataInPrint = recDataString.substring(0, endOfLineIndex);
//                        //Log.i("Bluetooth", "Data Received via Bluetooth");
//
//                        //int dataLength = dataInPrint.length();
//
//                        if(recDataString.charAt(0) == '#') {
//                            String readings = recDataString.substring(1, endOfLineIndex);
//                            // TODO create textview to display the count
//                            receivedDataTextView.setText(readings);
//                            Log.i("Readings", readings);
//                        }
//                        recDataString.delete(0, recDataString.length());
//                        dataInPrint = "";
//
//                    } else {
//                        Log.i("Reading Parse Error", "end of Line index is not bigger than 0, could be null");
//                    }

//                    int hashIndex = recDataString.indexOf("#");
//                    int newLineIndex = -1;
//                    String partialReading = "";
//
//                    // beginning of a set of reading is found
//                    while (hashIndex != -1) {
//
//                        newLineIndex = recDataString.indexOf("\n", newLineIndex + 1);
//                        if (newLineIndex != -1) {
//
//                            if (newLineIndex > hashIndex) {
//
//                                String parsedReading = recDataString.substring(hashIndex + 1, newLineIndex);
//                                // check if there is another hash in the String indicating another set of readings
//                                hashIndex = recDataString.indexOf("#", hashIndex + 1);
//
//                                receivedDataTextView.setText(parsedReading);
//                                Log.i("Readings", parsedReading);
//                                // separate readings and send to server
//                                // TODO refactor this code
//                                String[] separatedReadings = parsedReading.split(",");
//                                String IRLeft = separatedReadings[0];
//                                String US = separatedReadings[1];
//                                String IRRight = separatedReadings[2];
//                                uploadData(IRLeft,US,IRRight);
//
//
//
//
//                            } else {
//                                // partial reading from previous set combined with remainder of reading set in
//                                // subsequent iteration of loop
//                                String endOfReading = recDataString.substring(0, newLineIndex);
//                                String parsedReading = partialReading + endOfReading;
//                                newLineIndex = newLineIndex + 1;
//                                receivedDataTextView.setText(parsedReading);
//                                Log.i("Readings", parsedReading);
//                                String[] separatedReadings = parsedReading.split(",");
//                                String IRLeft = separatedReadings[0];
//                                String US = separatedReadings[1];
//                                String IRRight = separatedReadings[2];
//                                uploadData(IRLeft,US,IRRight);
//                            }
//
//                        } else {
//                            // partial reading
//                            partialReading = recDataString.substring(hashIndex + 1, recDataString.length());
//                            hashIndex = -1;
//
//                        }
//                    }
                }

        };


        //TODO delete redundant handler after identification
//        Handler mHandler = new Handler() {
//            @Override
//            public void handleMessage(Message msg) {
//                byte[] writeBuf = (byte[]) msg.obj;
//                int begin = (int) msg.arg1;
//                int end = (int) msg.arg2;
//
//                switch (msg.what) {
//                    case 1:
//                        String writeMessage = new String(writeBuf);
//                        writeMessage = writeMessage.substring(begin, end);
//                        break;
//                }
//            }
//        };

        // initialize button objects
        turnBtOnButton = (Button) findViewById(R.id.btnTurnBluetoothOn);
        turnBtOffButton = (Button) findViewById(R.id.btnTurnBluetoothOff);
        listDevicesButton = (Button) findViewById(R.id.btnListPairedDevices);
        getVisibleDevicesButton = (Button) findViewById(R.id.btnGetVisibleDevices);
        connectButton = (Button) findViewById(R.id.btnConnect);


        // initialize bluetooth adapter object
        BA = BluetoothAdapter.getDefaultAdapter();
        listView = (ListView) findViewById(R.id.listView);

        // turn on bluetooth
        if (!BA.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }


        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                pairedDevices = BA.getBondedDevices();

                // obtain the HC-06 device if it is present
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        if (device.getName().equals("HC-06"))
                            mDevice = device;
                        break;
                    }
                }

                // TODO make the connectThread variable a local variable, passed into the method rather than having
                // it available throughout the class
                connectThread = new ConnectThread(mDevice);
                connectThread.start();
            }
        });

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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
        // view visible devices
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
    }

    public void closeBluetoothConnection(View view) {
        if (connectThread != null) {
            connectThread.cancel();
            Log.i("Bluetooth", "bluetooth connection closed successfully");
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.adityapradhan.bluetoothtutorialspoint/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.adityapradhan.bluetoothtutorialspoint/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    // interface with bluetooth module
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID;
        private ConnectedThread mConnectedThread;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
            // get connected bluetooth devices UUID
            MY_UUID = mmDevice.getUuids()[0].getUuid();
            //"00001101-0000-1000-8000-00805f9b34fb"


            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                Log.i("bluetooth status", "attempting to create RFCOMM socket");
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.i("bluetooth status", "RFCOMM socket created");
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            BA.cancelDiscovery();

            Log.i("bluetooth status", "start of connectThread");


            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            try {
                Log.d("bluetooth status", "attempting connection");
                mmSocket.connect();
                Log.d("device connected", "bluetooth connection successful");
            } catch (IOException connectException) {
                // Unable to connect. Close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }

                return;
            }

            //manageConnectedSocket(mmSocket);
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();

        }

        // Will cancel an in-progress connection, and close the socket
        public void cancel() {
            try {
                mmSocket.close();

            } catch (IOException closeException) {
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;


        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;


        }

        public void run() {
//            byte[] buffer = new byte[1024];  // buffer store for the stream
//            int begin = 0;
//            int bytes;  // bytes returned from read()

            byte[] buffer = new byte[512];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    // read in bytes from Input Stream into buffer and store number of bytes retrieved
                    bytes = mmInStream.read(buffer);
                    // print out number of bytes in buffer
                    int bufferSize = 0;
                    for (byte x : buffer) {
                        if (x > 0) {
                            bufferSize++;
                        }
                    }

                    //Log.i("Buffer size", Integer.toString(bufferSize));
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }


            // Keep listening to the InputStream until an exception occurs
//            while (true) {
//                try {
//                    // Read from the InputStream
//                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
//                    for (int i = begin; i < bytes; i++) {
//                        if (buffer[i] == "#".getBytes()[0]) {
//                            // Send the obtained bytes to the UI activity
//                            mHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
//                            begin = i + 1;
//                            if (i == bytes - 1) {
//                                bytes = 0;
//                                begin = 0;
//                            }
//                        }
//                    }
//                } catch (IOException e) {
//                    break;
//                }
//            }

//            while (true) {
//
//                try {
//                    // Read from Input Stream
//                    bytes = mmInStream.read(buffer);
//                    // Send the obtained bytes to the UI activity
//                    mHandler.obtainMessage(9999,bytes, -1, buffer).sendToTarget();
//                } catch (IOException e) {
//                    break;
//                }
//            }
//
//        }

        /* Call this from the main activity to send data to the remote device */
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }





    }
    public void uploadData(String IRLeft, String US, String IRRight) {


        WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr.isWifiEnabled()) {
            // WiFi adapter is ON
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            if (wifiInfo.getNetworkId() == -1) {
                // Not connected to an access-Point
                Log.i("Readings Upload", "Wifi not connected. Readings were not sent");

            } else {
                // Connected to an Access Point
                LightHouseAPI lighthouse = new LightHouseAPI();
                HashMap<String, String> readings = new HashMap<String, String>();
                // ID
                String deviceID = Settings.Secure.ANDROID_ID;
                readings.put("ID", deviceID);


                // Timestamp
                long timestamp = System.currentTimeMillis();
                readings.put("Timestamp", Long.toString(timestamp));

                // sensor_type

                // Distance
                readings.put("IRLeft", IRLeft);
                readings.put("Distance", IRLeft);
                readings.put("Ultrasound", US);
                readings.put("IRRight", IRRight);


                // Send readings to UDP port on server
                boolean response = lighthouse.sendSensorDataSync(readings);
                Log.i("Data Sync Response", Boolean.toString(response));
            }

        } else {
            // WiFi adapter is OFF
            Log.i("Data Upload", "Wifi is not turned on. Readings were not sent");
        }




    }

    // assuming a constant velocity of the user.
    // assume velocity is 1 metre a second
    public boolean isMoving(double[] distances) {
        for(int i=0; i< distances.length - 1; i++) {
            if(distances[i+1] < distances[i] ) {
                return false;
            }
        }

        return true;

    }

}
