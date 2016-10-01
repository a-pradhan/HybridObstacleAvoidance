package com.adityapradhan.hybridobstacleavoidance;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/*
 * AccelerometerListerner observes the readings from the device's accelerometer sensor and uses the
 * MovementDetection object to record whether the changes in acceleration are significant or not
 *
 */
public class AccelerometerListener implements SensorEventListener {
    private MovementDetection movementDetection;

    public AccelerometerListener(MovementDetection movementDetection) {
        this.movementDetection = movementDetection;
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;


        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
            long currTime = System.currentTimeMillis();

            movementDetection.recAccelerometerChange(x,y,z,currTime);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
