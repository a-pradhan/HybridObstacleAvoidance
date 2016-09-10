package com.adityapradhan.hybridobstacleavoidance;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
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
import com.opencsv.CSVWriter;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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


    private SensorManager sensorManager;
    private Sensor accelerometer;
    private MovementDetection movementDetection = new MovementDetection(10);
    private HandlerThread mSensorThread;
    private Handler mSensorHandler;


    EventDetection eventDetection;
    private ObstacleKalmanFilter filter;


    //    private long lastUpdate = 0;
//    private float last_x, last_y, last_z;
//    private static final int SHAKE_THRESHOLD = 200;
    private StringBuilder recDataString = new StringBuilder();

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    String launchTimeStamp;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        Log.i("Info", "Start of Main Activity");
        final TextView receivedDataTextView = (TextView) findViewById(R.id.receivedDataTextView);
        final TextView filterEstimateTextView = (TextView) findViewById(R.id.filterEstimateTextView);
        final ReadingParser readingParser = new ReadingParser();
        eventDetection = new EventDetection();
        launchTimeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());

        // setup accelerometer
        Log.i("Info", "setup sensor");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = null;
        if (accelerometer == null) {
            // Use the accelerometer.
            if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            } else {
                // Sorry, there are no accelerometers on your device.
                // You can't play this game.
            }
        }
        Log.i("Info", "register sensor listener");

        // http://stackoverflow.com/questions/3286815/sensoreventlistener-in-separate-thread
        mSensorThread = new HandlerThread("Sensor thread", Thread.MAX_PRIORITY);
        mSensorThread.start();
        mSensorHandler = new Handler(mSensorThread.getLooper()); //Blocks until looper is prepared, which is fairly quick
        sensorManager.registerListener(new AccelerometerListener(movementDetection), accelerometer, SensorManager.SENSOR_DELAY_NORMAL, mSensorHandler);


        // initialize handler object to receive messages sent by bluetooth module
        //TODO create new Handler class for this method
        bluetoothIn = new Handler() {


            public void handleMessage(Message msg) {
                if (msg.what == handlerState) {
                    // String obtained by reading from byte buffer
                    String readMessage = (String) msg.obj;
                    String readingString = readingParser.parseReadings(readMessage);


                    if (readingString != null) {
                        // full set of readings received -> initialize/run appropriate filter
                        // Log.i("parsed reading" , readingString);
                        //Log.i("count", Integer.toString(counter++));
                        receivedDataTextView.setText(readingString);
                        // separate readings and arrange in a measurement vector
                        String[] splitStringReadings = readingString.split(",");
                        ArrayList<Double> splitDoubleReadings = new ArrayList<Double>(splitStringReadings.length + 1); // extra to hold velocity, which is not measured

                        for (int i = 0; i < splitStringReadings.length; i++) {
                            splitDoubleReadings.add(Double.parseDouble(splitStringReadings[i]));
                        }

                        RealVector measurements = new ArrayRealVector(splitDoubleReadings.toArray(new Double[splitDoubleReadings.size()]));


                        if (obstacleDetected(measurements)) {
                            // instantiate correct filter
                            filter = initFilter(filter, splitDoubleReadings);

//                            if (filter == null) {
//                                // initialize filter with first set of readings making up part of the state with assumed 0 velocity
//                                splitDoubleReadings.add(0d);
//                                Double[] stateArray = splitDoubleReadings.toArray(new Double[splitDoubleReadings.size()]);
//
//                                RealVector initialState = new ArrayRealVector(stateArray);
//
//                                filter = new ObstacleKalmanFilter(initialState);
//                                Log.i("initialState used", initialState.toString());
//                                Log.i("initial covariance", filter.getStateCovarianceMatrix().toString());
//                                eventDetection = new EventDetection();
//                                eventDetection.addStateEstimate(initialState);
//                            } else {
//                                // check if moving or not
//                                RealVector[] previousEstimates = eventDetection.getStateEstimates();
//                                boolean[] distanceChangedArray = eventDetection.getChangedDistanceIndex(previousEstimates);
//
//                                boolean isMoving = isMoving(distanceChangedArray);
//                                filter = initMovingFilter(isMoving, filter); // instantiate new filter using if
//
//                                counter++;
//
//                                // run filter
//                                filter.predict();
//                                //Log.i("filter prediction", filter.getStateEstimationVector().toString());
//                                filter.correct(measurements);
//                                filterEstimateTextView.setText(filter.getStateEstimationVector().toString());
//                                eventDetection.addStateEstimate(filter.getStateEstimationVector());
//                                if (counter % 5 == 0) {
//                                    Log.i("Filter Estimate", filter.getStateEstimationVector().toString());
//                                }
//
//                                //Log.i("state estimate", filter.getStateEstimationVector().toString());
//                                //Log.i("state covariance", filter.getStateCovarianceMatrix().toString());
//
//
//                            }
                            filter.predict();
                            filter.correct(measurements);
                            RealVector stateEstimateVector = filter.getStateEstimationVector();
                            String stateEstimate = "";

                            for(int i = 0; i < stateEstimateVector.getDimension(); i++) {
                                if(i != stateEstimateVector.getDimension() - 1) {
                                    stateEstimate += stateEstimateVector.getEntry(i) + ",";
                                } else {
                                    // do not add "," for last entry
                                    stateEstimate += stateEstimateVector.getEntry(i) + "\n";
                                }

                            }
                            // eventDetection.addStateEstimate(filter.getStateEstimationVector());
                            filterEstimateTextView.setText(stateEstimate);
                           // Log.i("state estimate", stateEstimate);

                            //Log.i("state covariance", filter.getStateCovarianceMatrix().toString());
                            // log readings to CSv file
                            String logReadings = readingString + "," + stateEstimate;
                            Log.i("log reading", logReadings);
                            saveToCSV(launchTimeStamp, logReadings);

                            // reset filter if estimates are negative (applies for moving case)
                            if (areEstimatesNegative(stateEstimateVector)) {
                                Log.i("Rest Filter", "STATE ESTIMATE IS NEGATIVE");
                                filter = null;
                            }


                        } else {
                            Log.i("Reset Filter", "NO OBSTACLE DETECTED");
                            filter = null;
                        }


                    }


                    // check if slowed/accelerating/user turned and reinitialize Filter object with latest estimate and current covariance matrix;


                }


            }

        };


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

//    @Override
//    public void onSensorChanged(SensorEvent sensorEvent) {
//        Sensor mySensor = sensorEvent.sensor;
//
//
//        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            float x = sensorEvent.values[0];
//            float y = sensorEvent.values[1];
//            float z = sensorEvent.values[2];
//            long currTime = System.currentTimeMillis();
//
//            movementDetection.recAccelerometerChange(x,y,z,currTime);
//
//        }
//
//    }
//
//    @Override
//    public void onAccuracyChanged(Sensor sensor, int i) {
//
//    }

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
            // TODO handle null pointer exception when attempting to obtain UUID while BT is off
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
            // Cancel discovery if its on because it will slow down the connection
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
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;


        }

        public void run() {
              // Resource: https://wingoodharry.wordpress.com/2014/04/15/android-sendreceive-data-with-arduino-using-bluetooth-part-2/
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
//                    int bufferSize = 0;
//                    for (byte x : buffer) {
//                        if (x > 0) {
//                            bufferSize++;
//                        }
//                    }
                    //Log.i("Buffer size", Integer.toString(bufferSize));
                    // TODO handle NullPointerException
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


    // assess whether user is moving by checking changedDistancesArray
    public boolean isMoving(boolean[] distanceChangedArray) {
        for (boolean changedDistance : distanceChangedArray) {
            if (changedDistance == true) {
                return true;
            }
        }

        return false;

    }

    public ObstacleKalmanFilter initStationaryFilter(ArrayList<Double> splitDoubleReadings) {
        // initial sensor readings used to provide initial state for model
        splitDoubleReadings.add(0d); // velocity set to 0
        Double[] stateArray = splitDoubleReadings.toArray(new Double[splitDoubleReadings.size()]);
        RealVector initialState = new ArrayRealVector(stateArray); // state vector

        filter = new ObstacleKalmanFilter(initialState);
        Log.i("initialState used", initialState.toString());
        Log.i("initial covariance", filter.getStateCovarianceMatrix().toString());

        eventDetection.addStateEstimate(initialState);

        return filter;

    }

    // returns adjusted filter if user is moving
    public ObstacleKalmanFilter initMovingFilter(boolean isMoved, ObstacleKalmanFilter filter) {
        if (isMoved == true) {
            // if moving reinstantiate filter -  set initial velocity to 1 m/s and use current state estimate and covariance matrix
            Log.i("Movement detected", "Initializing moving filter");
            RealVector initialState = filter.getStateEstimationVector();
            RealMatrix initialCovarianceMatrix = filter.getStateCovarianceMatrix();
            initialState.setEntry(3, -50); // set velocity to desired value - obtain dynamically in future
            filter = new ObstacleKalmanFilter(initialState, initialCovarianceMatrix);
        }
        return filter;
    }

    public ObstacleKalmanFilter initFilter(ObstacleKalmanFilter filter, ArrayList<Double> splitDoubleReadings) {
        if (filter == null) {
            // initialize filter for first time
            filter = initStationaryFilter(splitDoubleReadings);

        } else {

            // initialize filter for person on the move
            RealVector[] previousEstimates = eventDetection.getStateEstimates();
            boolean[] distanceChangedArray = eventDetection.getChangedDistanceIndex(previousEstimates);
            //boolean isMoving = isMoving(distanceChangedArray);
            boolean isMoving = movementDetection.isMoving();
            filter = initMovingFilter(isMoving, filter); // instantiate new filter if moving other use existing

        }

        return filter;
    }

    public boolean obstacleDetected(RealVector measurementVector) {
        if (measurementVector.getEntry(0) < 160 || measurementVector.getEntry(1) < 140 || measurementVector.getEntry(2) < 160) {
            return true;
        }

        return false;

    }

    // returns an array corrseponding to 3 sensors identifying which sensors have detected an obstacle and corresponds to each side
    public boolean[] whichSide(RealVector estimateVector) {
        boolean[] obstaclePresent = new boolean[3];


        return obstaclePresent;
    }

    // returns true if any of the filter estimates are negative, signalling that the filter should be reset
    // TODO use loop to avoid hard coding
    public boolean areEstimatesNegative(RealVector estimateVector) {
        if (estimateVector.getEntry(0) < 20 || estimateVector.getEntry(1) < 20 || estimateVector.getEntry(2) < 20) {
            return true;
        }

        return false;
    }

    // saves sensor/filter data to a csv log file
    public void saveToCSV() {
//        String baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
//        String FILENAME = "sensor_data.csv";
//        String filepath = baseDir + File.separator + FILENAME;
//        String entry = "1,2,3\n";
//        File f = new

//        try {
//            // write to end of file if it exists
//            FileOutputStream out = openFileOutput(filepath, Context.MODE_APPEND);
//            out.write(entry.getBytes());
//            out.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
//        File file = new File(dir, "sensor_readings.csv");
//
//        try {
//            FileWriter fileWriter = new FileWriter(file);
//            fileWriter.append("1,2,3");
//            fileWriter.flush();
//            fileWriter.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        String fileName = "example.csv";
//        String dirName = "MyDirectory";
//        String contentToWrite = "Your Content Goes Here";
//        File myDir = new File("sdcard", dirName);
//
///*if directory doesn't exist, create it*/
//        if(!myDir.exists())
//            myDir.mkdirs();
//
//
//        File myFile = new File(myDir, fileName);
//
///*Write to file*/
//        try {
//            FileWriter fileWriter = new FileWriter(myFile);
//            fileWriter.append(contentToWrite);
//            fileWriter.flush();
//            fileWriter.close();
//        }
//        catch(IOException e){
//            e.printStackTrace();
//        }


//        String data = "1,2,3";
//        File baseDir = new File("/sdcard/Documents");
//        String fileName = "test";
//
//        File file = new File(baseDir, fileName + ".csv");
//        Log.i("Directory to Save", file.getAbsolutePath());
//
//        try {
//            if (!file.exists()) {
//                file.createNewFile();
//            }
//
//            FileWriter fw = new FileWriter(file.getAbsolutePath());
//            BufferedWriter bw = new BufferedWriter(fw);
//            bw.write(data);
//            bw.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } {
//
//        }




    }

public void saveToCSV(String timeStamp, String reading) {
    // Resource: http://blog.cindypotvin.com/saving-data-to-a-file-in-your-android-application/
    try
    {
        // Creates a trace file in the primary external storage space of the
        // current application.
        // If the file does not exists, it is created.
        File csvFile = new File(((Context)this).getExternalFilesDir(null), "readings_" + timeStamp + ".csv");
        if (!csvFile.exists())
            csvFile.createNewFile();
        // Adds a line to the trace file
        BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile, true /*append*/));
        writer.write(reading);
        writer.close();
        // Refresh the data so it can seen when the device is plugged in a
        // computer. You may have to unplug and replug the device to see the
        // latest changes. This is not necessary if the user should not modify
        // the files.
        MediaScannerConnection.scanFile((Context)(this),
                new String[] { csvFile.toString() },
                null,
                null);

    }
    catch (IOException e)
    {
        Log.e("file test", "Unable to write to the TraceFile.txt file.");
    }
}

    public static boolean isSdReadable()
    {

        boolean mExternalStorageAvailable = false;
        try
        {
            String state = Environment.getExternalStorageState();

            if (Environment.MEDIA_MOUNTED.equals(state))
            {
                // We can read and write the media
                mExternalStorageAvailable = true;
                Log.i("isSdReadable", "External storage card is readable.");
            }
            else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
            {
                // We can only read the media
                Log.i("isSdReadable", "External storage card is readable.");
                mExternalStorageAvailable = true;
            }
            else
            {
                // Something else is wrong. It may be one of many other
                // states, but all we need to know is we can neither read nor
                // write
                mExternalStorageAvailable = false;
                Log.i("isSdReadable", "Cannot read or write to storage");
            }
        } catch (Exception ex)
        {

        }
        return mExternalStorageAvailable;
    }
} // end of class
