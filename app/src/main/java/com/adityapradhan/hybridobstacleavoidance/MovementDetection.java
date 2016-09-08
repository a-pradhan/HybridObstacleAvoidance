package com.adityapradhan.hybridobstacleavoidance;

import android.util.Log;

/**
 * Created by Aditya on 9/7/2016.
 */
public class MovementDetection {
    private boolean[] sigAccelerometerChanges;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 10;
    private int index;


    public MovementDetection(int size ) { // size corresponds to the number of last readings you want to look at
        sigAccelerometerChanges = new boolean[size];
        index = 0;
    }

    // returns boolean if last 5 values are true in movement array
    public void recAccelerometerChange(float x, float y, float z, long currTime) {
        if ((currTime - lastUpdate) > 100) {
            long diffTime = (currTime - lastUpdate);
            lastUpdate = currTime;

            float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 1000;

            if (speed > SHAKE_THRESHOLD) {
                Log.i("Moving State", Boolean.toString(isMoving()));
               // Log.i("walking", Float.toString(speed));
                storeAccelerometerChange(true);

            } else {
                Log.i("Moving State", Boolean.toString(isMoving()));
               // Log.i("stationary", Float.toString(speed));
                storeAccelerometerChange(false);
        }

            last_x = x;
            last_y = y;
            last_z = z;


        }


    }

    public void storeAccelerometerChange(boolean isChangeSignificant) {
        if(index < sigAccelerometerChanges.length) {
            sigAccelerometerChanges[index] = isChangeSignificant;
            index++;
        } else {
            shiftSigAccelerometerChangesArray(sigAccelerometerChanges);
            sigAccelerometerChanges[--index] = isChangeSignificant;
        }

    }

    public void shiftSigAccelerometerChangesArray(boolean[] sigAccelerometerChanges) {
        for(int i=0; i < sigAccelerometerChanges.length - 1; i++) {
            sigAccelerometerChanges[i] = sigAccelerometerChanges[i+1];
        }
    }

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
