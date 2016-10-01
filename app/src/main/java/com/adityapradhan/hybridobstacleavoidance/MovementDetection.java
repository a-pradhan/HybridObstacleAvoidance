package com.adityapradhan.hybridobstacleavoidance;

import android.util.Log;

/**
 * Created by Aditya on 9/7/2016.
 * The MovementDetection class is used to determine whether the user is moving or not by examining
 * the changes in accelerometer readings along the x,y,z axis over time (number of readings examined, x, can be set during instantiation).
 * If significant changes in acceleration, determined by the SHAKE_THRESHOLD value are detected over the course of x readings
 * the isMoving method returns true
 */
public class MovementDetection {
    private boolean[] sigAccelerometerChanges; // holds x boolean values corresponding to significant changes in acceleration
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 10;
    private int index;


    public MovementDetection(int size ) { // size corresponds to the number of readings to be examined
        sigAccelerometerChanges = new boolean[size];
        index = 0;
    }

    /* uses a weighted value proportional to change in acceleration in the x,y and z axis over the period of time since the last
     * accelerometer reading and compares with threshold value to determine whether the change is significant or not. Results are
     * stored in the sigAccelerometerChanges array
     */
    public void recAccelerometerChange(float x, float y, float z, long currTime) {
        if ((currTime - lastUpdate) > 100) {
            long diffTime = (currTime - lastUpdate);
            lastUpdate = currTime;

            float weightedChange = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 1000;

            if (weightedChange > SHAKE_THRESHOLD) {
                //Log.i("Moving State", Boolean.toString(isMoving()));
                storeAccelerometerChange(true);

            } else {
                //Log.i("Moving State", Boolean.toString(isMoving()));
                storeAccelerometerChange(false);
        }

            last_x = x;
            last_y = y;
            last_z = z;


        }


    }

    // appends type of acceleration in sigAccelerometerChanges array
    public void storeAccelerometerChange(boolean isChangeSignificant) {
        if(index < sigAccelerometerChanges.length) {
            sigAccelerometerChanges[index] = isChangeSignificant;
            index++;
        } else {
            // make room for new element
            shiftSigAccelerometerChangesArray(sigAccelerometerChanges);
            sigAccelerometerChanges[--index] = isChangeSignificant;
        }

    }

    // deletes first entry and shifts array elements 1 index to the left
    public void shiftSigAccelerometerChangesArray(boolean[] sigAccelerometerChanges) {
        for(int i=0; i < sigAccelerometerChanges.length - 1; i++) {
            sigAccelerometerChanges[i] = sigAccelerometerChanges[i+1];
        }
    }

    // returns true if last x values are true in movement array
   public boolean isMoving() {
       int count = 0;
       for(boolean sigAccelerationChange : sigAccelerometerChanges) {
           if(sigAccelerationChange == true) {
               count++;
           }

       }
       if(count > sigAccelerometerChanges.length / 2) {
           return true;
       } else {
           return false;
       }


   }

}
