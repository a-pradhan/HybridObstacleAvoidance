package com.adityapradhan.bluetoothtutorialspoint;

import android.util.Log;

import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * Created by aditya on 8/15/16.
 */
public class ObstacleKalmanFilter {
    final double dt = 1;

    // state transition matrix
    RealMatrix A = new Array2DRowRealMatrix(new double[][] {
            {1,dt},
            {0,1}
    });

    // no control input modelled
    RealMatrix B = null;

    // Measurement function matrix - used to convert state matrix into measurement space
    RealMatrix H = new Array2DRowRealMatrix(new double[][] {
            {1, 0}
    });

    // process noise covariance matrix
    RealMatrix Q = new Array2DRowRealMatrix(new double[][] {
            {100, 0},
            {0  , 2.25}
    });

    // sensor error covariance matrix
    RealMatrix R =  new Array2DRowRealMatrix(new double[] {9});

    // Initial state estimates
    RealVector X0 = new ArrayRealVector(new double[] {10, 1});

    // initial error covariance matrix
    RealMatrix P0 = new Array2DRowRealMatrix(new double[][] {
            {9, 0 },
            {0, 1.25 }
    });

    ProcessModel pm = new DefaultProcessModel(A,B,Q,X0,P0 );
    MeasurementModel mm = new DefaultMeasurementModel(H, R);
    KalmanFilter filter = new KalmanFilter(pm, mm);




    // mock measurements for x
    double[] z = {11.3,12.9,14.2};

    // mock measurement vector velocity is constant
    RealVector Z;
    double[] predictions = new double[3];
    double[] estimates = new double[3];


// Should go in an activity thread;

//    for (int i = 0; i < z.length; i++) {
//        System.out.println("Iteration: " + (i+1));
//        filter.predict();
//        // store prediction and print
//        predictions[i] = filter.getStateEstimation()[0];
//        double[] prediction = filter.getStateEstimation();
//        System.out.println("predicted distance: " + prediction[0] + ", predicted velocity: " + prediction[1]);
//        System.out.println(filter.getErrorCovariance());
//        Z = new ArrayRealVector(new double[] {z[i]});
//
//        filter.correct(Z);
//        // store filter estimate and print
//        double[] stateEstimate = filter.getStateEstimation();
//        estimates[i] = filter.getStateEstimation()[0];
//        System.out.println("distance: " + stateEstimate[0] + ", velocity: " + stateEstimate[1]);
//
//
//    }
}
