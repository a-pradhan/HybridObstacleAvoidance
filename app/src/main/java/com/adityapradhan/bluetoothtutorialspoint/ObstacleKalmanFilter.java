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


import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;


/*
 * implementation of a basic Kalman Filter using the Apache common
 * KalmanFilter library
 */
public class ObstacleKalmanFilter {

    public static void main(String[] args) {
        final double dt = 0.2;

        // state transition matrix
        RealMatrix A = new Array2DRowRealMatrix(
                new double[][] {
                        { 1, 0, 0, dt },
                        { 0, 1, 0, dt },
                        { 0, 0, 1, dt },
                        { 0, 0, 0, 1  }

                });

        // no control input modelled
        RealMatrix B = null;

        // Measurement function matrix - used to convert state matrix into
        // measurement space
        // measurement matrix vector fill we in the form:
		/*
		 * [1.23] [1] Z = [1.41] X = [2] [1.67] [3] [4]
		 */
        RealMatrix H = new Array2DRowRealMatrix(new double[][] {
                { 1, 0, 0, 0 },
                { 0, 1, 0, 0 },
                { 0, 0, 1, 0 }

        });

        // process noise covariance matrix
        RealMatrix Q = new Array2DRowRealMatrix(
                new double[][] {
                        { 100, 0, 0, 0 },
                        { 0, 100, 0, 0 },
                        { 0, 0, 100, 0 },
                        { 0, 0, 0, 2.25 }
                });

        // sensor error covariance matrix
        RealMatrix R = new Array2DRowRealMatrix(new double[][] {
                { 20, 0, 0 },
                { 0, 9, 0 },
                { 0, 0, 20 }

        });

        // Initial state estimate
        RealVector X0 = new ArrayRealVector(new double[] { 1.5, 1.5, 1.5, 1 });

        // initial error covariance matrix
        RealMatrix P0 = new Array2DRowRealMatrix(
                new double[][] {
                        { 9, 0, 0, 0 },
                        { 0, 10, 0, 0 },
                        { 0, 0, 12, 0 },
                        { 0, 0, 0, 1.25 }

                });

        ProcessModel pm = new DefaultProcessModel(A, B, Q, X0, P0);
        MeasurementModel mm = new DefaultMeasurementModel(H, R);
        KalmanFilter filter = new KalmanFilter(pm, mm);

        System.out.println("Filter created successfully");

        // mock measurements for x
        double[] z = { 2.3, 2.9, 1.7 };

        // mock measurement vector velocity is constant
        RealVector Z = new ArrayRealVector(z);
        RealVector[] predictions = new RealVector[3];
        RealVector[] estimates = new RealVector[3];

        for (int i = 0; i < 1; i++) {
            System.out.println("Iteration: " + (i + 1));
            filter.predict();
            // store prediction and print
            predictions[i] = filter.getStateEstimationVector();
            System.out.println("predicted state: " + predictions[0]);
            System.out.println("error covariance matrix: " + filter.getErrorCovarianceMatrix());

            filter.correct(Z);
            // store filter estimate and print
            estimates[i] = filter.getStateEstimationVector();
            System.out.println("estimated stated: " + estimates[i]);

        }



    }

}
