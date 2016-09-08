package com.adityapradhan.hybridobstacleavoidance;

import org.apache.commons.math3.linear.RealVector;

/**
 * Created by Aditya on 9/8/2016.
 */


public class ObstacleDetected {
    private RealVector[] stateEstimates;
    private int index;

    public ObstacleDetected() {
        stateEstimates = new RealVector[5];
        index = 0;
    }

    public boolean isObstacleDetected() {

        return false;
    }
}
