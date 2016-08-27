package com.adityapradhan.bluetoothtutorialspoint;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.junit.Test;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Created by Aditya on 8/26/2016.
 */
public class EventDetectionUnitTest {
//    @Test
//    public void userIsMovingTowardsObstacleTest() {
//        RealVector[] stateEstimates = new RealVector[] {
//                new ArrayRealVector(new double[] {80.0,65,50.8,0}),
//                new ArrayRealVector(new double[] {79.0,65,50.8,0}),
//                new ArrayRealVector(new double[] {78.0,65,50.8,0}),
//                new ArrayRealVector(new double[] {77.0,65,50.8,0}),
//                new ArrayRealVector(new double[] {76.0,65,50.8,0})
//
//        };
//
//        boolean userIsMoving = EventDetection.isMoving(stateEstimates);
//        assertEquals(true, userIsMoving);
//
//    }



    // tests for determining which IR sensors are the ones that are going to be configu
    @Test
    public void leftIRDistanceReduced() {
        RealVector[] stateEstimates = new RealVector[]{
                new ArrayRealVector(new double[]{80.0, 65, 50.8, 0}),
                new ArrayRealVector(new double[]{79.0, 66, 50.8, 0}),
                new ArrayRealVector(new double[]{78.0, 67, 50.8, 0}),
                new ArrayRealVector(new double[]{77.0, 68, 50.8, 0}),
                new ArrayRealVector(new double[]{76.0, 69, 50.8, 0})
        };

        boolean[] changedDistanceArray = EventDetection.getChangedDistanceIndex(stateEstimates);
        assertTrue(Arrays.equals(new boolean[] {true, false, false, false}, changedDistanceArray));

    }

    @Test
    public void USDistanceReduced() {

        RealVector[] stateEstimates = new RealVector[]{
                new ArrayRealVector(new double[]{80.0, 65.1, 50.8, 0}),
                new ArrayRealVector(new double[]{81.0, 65.045, 50.8, 0}),
                new ArrayRealVector(new double[]{83  , 63.0, 50.8, 0}),
                new ArrayRealVector(new double[]{79  , 50.0, 50.8, 0}),
                new ArrayRealVector(new double[]{83  , 49.8, 50.8, 0})
        };

        boolean[] changedDistanceArray = EventDetection.getChangedDistanceIndex(stateEstimates);
        assertTrue(Arrays.equals(new boolean[] {false, true, false, false}, changedDistanceArray));

    }

    @Test
    public void rightIRDistanceReduced() {
        RealVector[] stateEstimates = new RealVector[]{
                new ArrayRealVector(new double[]{80.0, 65, 51.8, 0}),
                new ArrayRealVector(new double[]{79.0, 66, 50.9, 0}),
                new ArrayRealVector(new double[]{78.0, 67, 50.8, 0}),
                new ArrayRealVector(new double[]{81.0, 58, 50.7, 0}),
                new ArrayRealVector(new double[]{76.0, 69, 50.6, 0})
        };

        boolean[] changedDistanceArray = EventDetection.getChangedDistanceIndex(stateEstimates);
        assertTrue(Arrays.equals(new boolean[] {false, false, true, false}, changedDistanceArray));

    }

    @Test
    public void addStateEstimateToFullArrayTest() {
        EventDetection eventDetection = new EventDetection();
        // add 5 estimates to empty array
        eventDetection.addStateEstimate(new ArrayRealVector(new double[] {2.0,1.0,1.0,0.0}));
        eventDetection.addStateEstimate(new ArrayRealVector(new double[] {1.0,1.0,1.0,0.0}));
        eventDetection.addStateEstimate(new ArrayRealVector(new double[] {1.0,1.0,1.0,0.0}));
        eventDetection.addStateEstimate(new ArrayRealVector(new double[] {1.0,1.0,1.0,0.0}));
        eventDetection.addStateEstimate(new ArrayRealVector(new double[] {1.0,1.0,1.0,0.0}));

        // add new estimate to stateEstimates array which is now full
        eventDetection.addStateEstimate(new ArrayRealVector(new double[] {0.1,0.1,0.1,0.1}));

        RealVector[] estimates = eventDetection.getStateEstimates();
        RealVector firstEstimate = estimates[0];
        double[] newFirstEstimate = new double[] {1.0,1.0,1.0,0.0};

        RealVector lastEstimate = estimates[estimates.length-1];
        double[] expectedResult = new double[] {0.1,0.1,0.1,0.1};

        // convert to double array and compare to expected outcomes
        assertTrue(Arrays.equals(firstEstimate.toArray(), newFirstEstimate)); // check previous first estimate has been replaced by second estimate
        assertTrue(Arrays.equals(lastEstimate.toArray(), expectedResult)); // check new state estimate has been added to end of  stateEstimates array


    }




}
