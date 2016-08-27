package com.adityapradhan.bluetoothtutorialspoint;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 * Created by Aditya on 8/26/2016. EventDetection currently helps to identify whether the user is stationary or moving
 */
public class EventDetection {
    private RealVector[] stateEstimates;
    private int index;

    public EventDetection() {
        stateEstimates = new RealVector[5];
        index = 0;
    }

    // adds a state estimate to the end of the stateEstimates array. If it is full it deletes the first element and shifts the remaining elements to the front
    public void addStateEstimate(RealVector stateEstimate) {
        if(index < stateEstimates.length) {
            stateEstimates[index] = stateEstimate;
            index++;
        } else {
            shiftSateEstimatesArray(stateEstimates);
            stateEstimates[--index] = stateEstimate;
        }
    }

    private void shiftSateEstimatesArray(RealVector[] stateEstimates) {
        for(int i=0; i < stateEstimates.length - 1; i++) {
            stateEstimates[i] = stateEstimates[i+1];
        }
    }





    // returns a boolean array indicating the vector elements where the velocity changed
    public static boolean[] getChangedDistanceIndex(RealVector[] previousStates) {
           boolean[] distanceChangedArray = new boolean[previousStates.length - 1];

            if(previousStates[4] == null ) {
                return distanceChangedArray;
            } else {
                for (int stateColIndex = 0; stateColIndex < previousStates[0].getDimension(); stateColIndex++) {
                    for (int stateRowIndex = 0; stateRowIndex < previousStates.length - 1; stateRowIndex++) {
                        if (previousStates[stateRowIndex].getEntry(stateColIndex) <= previousStates[stateRowIndex + 1]
                                .getEntry(stateColIndex)) {
                            break;
                        }
                        // if the last two states have been compared
                        if (stateRowIndex + 1 == previousStates.length - 1) {
                            distanceChangedArray[stateColIndex] = true;

                        }
                    }
                }
            }

            return distanceChangedArray;

        }

    // TODO implement method
    static boolean obstacleDetected() {
        return false;
    }

    // TODO implement method
    static boolean userTurned() {
        return false;
    }


    // Getter and Setters

    public RealVector[] getStateEstimates() {
        return stateEstimates;
    }

    public int getIndex() {
        return index;
    }




}
