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
import android.os.SystemClock;
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
import com.jjoe64.graphview.GraphView;
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
import java.util.Set;
import java.util.UUID;




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
    private GraphView leftIRGraph, ultrasoundGraph, rightIRGraph;

    private KFilter filter;


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
        launchTimeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());

        // initialize Graph Views
        leftIRGraph = (GraphView) findViewById(R.id.graphLeftIR);
        ultrasoundGraph = (GraphView) findViewById(R.id.graphUltrasound);
        rightIRGraph = (GraphView) findViewById(R.id.graphRightIR);



        // setup accelerometer sensing
        Log.i("Info", "setup sensor");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = null;
        if (accelerometer == null) {
            // Use the accelerometer.
            if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            } else {
                // Sorry, there are no accelerometers on your device.
            }
        }
        Log.i("Info", "register sensor listener");

        // http://stackoverflow.com/questions/3286815/sensoreventlistener-in-separate-thread
        mSensorThread = new HandlerThread("Sensor thread", Thread.MAX_PRIORITY);
        mSensorThread.start();
        mSensorHandler = new Handler(mSensorThread.getLooper()); //Blocks until looper is prepared
        sensorManager.registerListener(new AccelerometerListener(movementDetection), accelerometer, SensorManager.SENSOR_DELAY_NORMAL, mSensorHandler);


        // initialize handler object to receive readings sent by bluetooth module and perform filtering
        bluetoothIn = new Handler() {


            public void handleMessage(Message msg) {
                //long startTime = System.currentTimeMillis(); // message comes in
                if (msg.what == handlerState) {
                    // String obtained from connectedThread
                    String readMessage = (String) msg.obj;
                    String readingString = readingParser.parseReadings(readMessage);



                    if (readingString != null) { // full set of readings received

                        receivedDataTextView.setText(readingString);

                        // separate readings, convert to double  and arrange in a measurement vector
                        String[] splitStringReadings = readingString.split(",");
                        ArrayList<Double> splitDoubleReadings = new ArrayList<Double>(splitStringReadings.length + 1); // extra to hold velocity, which is not measured

                        for (int i = 0; i < splitStringReadings.length; i++) {
                            splitDoubleReadings.add(Double.parseDouble(splitStringReadings[i]));
                        }

                        RealVector measurements = new ArrayRealVector(splitDoubleReadings.toArray(new Double[splitDoubleReadings.size()]));


                        if (obstacleDetected(measurements)) {
                            // at least 1 of the readings is in the sensors rated range

                            filter = initFilter(filter, splitDoubleReadings);
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

                            filterEstimateTextView.setText(stateEstimateVector.toString());
                            String logReadings = readingString + "," + stateEstimate;
                            saveToCSV(launchTimeStamp, logReadings);
                            Log.i("covariance", filter.getStateCovarianceMatrix().toString());

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
    } // end of onCreate() method



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
    } // end of onStart method

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
    } // end of onStop method


    // thread to establish connection with bluetooth module
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
                // Unable to connect. Close the socket
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

    // Thread to receive stream of bytes, corresponding to sensor readings from bluetooth module
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
            byte[] buffer = new byte[512]; // buffer to store stream of bytes received from bluetooth
            int bytes; // number of bytes in the buffer

            // Keep looping to listen for input received via Bluetooth
            while (true) {
                try {
                    // read in bytes from Input Stream into buffer and store number of bytes retrieved
                    bytes = mmInStream.read(buffer);

                    // print out number of bytes in buffer
                    // Log.i("Buffer size", Integer.toString(bytes));


                   // create String using bytes from the byte buffer
                    String readMessage = new String(buffer, 0, bytes);

                    // send String obtained to the UI Activity handler bluetoothIn
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
      }

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

    // turn off device Bluetooth
    public void closeBluetoothConnection(View view) {
        if (connectThread != null) {
            connectThread.cancel();
            Log.i("Bluetooth", "bluetooth connection closed successfully");
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

    public KFilter initStationaryFilter(ArrayList<Double> splitDoubleReadings) {
        // initial sensor readings used to provide initial state for model
        splitDoubleReadings.add(0d); // velocity set to 0
        Double[] stateArray = splitDoubleReadings.toArray(new Double[splitDoubleReadings.size()]);
        RealVector initialState = new ArrayRealVector(stateArray); // state vector

        filter = new StationaryKalmanFilter(initialState);
        Log.i("initialState used", initialState.toString());
        Log.i("initial covariance", filter.getStateCovarianceMatrix().toString());

        return filter;

    }

    // returns adjusted filter if user is moving
    public KFilter initMovingFilter(boolean isMoved, KFilter currFilter) {
        if (isMoved == true) {
            // if moving reinstantiate filter -  set initial velocity to 1 m/s and use current state estimate and covariance matrix
            Log.i("Movement detected", "Initializing moving filter");
            RealVector initialState = currFilter.getStateEstimationVector();
            RealMatrix initialCovarianceMatrix = currFilter.getStateCovarianceMatrix();
            initialState.setEntry(3, -50); // set velocity to desired value - obtain dynamically in future
            filter = new StationaryKalmanFilter(initialState, initialCovarianceMatrix);
        }
        return filter;
    }

    public KFilter initFilter(KFilter currFilter, ArrayList<Double> splitDoubleReadings) {
        if (currFilter == null) {
            // initialize filter for first time
            filter = initStationaryFilter(splitDoubleReadings);
            FilterSubject filterObservable = (FilterSubject) filter;

            // initialization of graphs
            FilterObserver drawLeftIRGraph = new DrawLineGraph("Left IR Readings",0, leftIRGraph);
            FilterObserver drawUltrasoundGraph = new DrawLineGraph("Ultrasound Readings", 1, ultrasoundGraph);
            FilterObserver drawRightIRGraph = new DrawLineGraph("Right IR Readings", 2, rightIRGraph);

            // register observers
            filterObservable.registerObserver(drawLeftIRGraph);
            filterObservable.registerObserver(drawUltrasoundGraph);
            filterObservable.registerObserver(drawRightIRGraph);

        } else {

            // initialize filter for person on the move
            boolean isMoving = movementDetection.isMoving();
            filter = initMovingFilter(isMoving, filter); // instantiate new filter if moving otherwise use existing

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

} // end of class
