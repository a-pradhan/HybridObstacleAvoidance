package com.adityapradhan.hybridobstacleavoidance;

import android.util.Log;

import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;

/**
 * StationaryKalmanFilter implements a Kalman Filter for a person who's velocity is 0 cm/s, using
 * values determined by sensor experiments to design the R matrix.
 *
 */
public class StationaryKalmanFilter implements KFilter, FilterSubject {
    private KalmanFilter filter;
    private final double dt = 0.05;
    private ArrayList<FilterObserver> observers = new ArrayList<FilterObserver>();
    private RealVector prediction;
    private RealVector measurement;
    private RealVector estimate;
    private int currentTimeStep = 0;

    // state transition matrix
    RealMatrix A = new Array2DRowRealMatrix(
            new double[][] {
                    { 1, 0, 0, dt }, // dispacement from left IR sensor
                    { 0, 1, 0, dt }, // displacement from ultrasound sensor
                    { 0, 0, 1, dt }, // displacement from right IR sensor
                    { 0, 0, 0, 1  }  // velocity

            });

    // no control input modelled
    RealMatrix B = null;

    // Measurement function matrix - used to convert state matrix into measurement form
    RealMatrix H = new Array2DRowRealMatrix(new double[][] {
            { 1, 0, 0, 0 },
            { 0, 1, 0, 0 },
            { 0, 0, 1, 0 }

    });

    // process noise covariance matrix
    RealMatrix Q = new Array2DRowRealMatrix(
            new double[][] {
                    { Math.pow(dt,3)/3, 0, 0, 0 },
                    { 0, Math.pow(dt,3)/3, 0, 0 },
                    { 0, 0, Math.pow(dt,3)/3, 0 },
                    { 0, 0, 0, 0               }
            });

    // sensor error covariance matrix
    RealMatrix R = new Array2DRowRealMatrix(new double[][] {
            {47.6 , 0, 0 },
            { 0, 120.8, 0 },
            { 0, 0, 97.7 }


    });

    // same as R matrix as initial sensor reading is used for initial state estimate with addition of error for velocity
    RealMatrix P0 = new Array2DRowRealMatrix(new double[][] {
            {47.6 , 0, 0,0 },
            { 0, 120.8, 0,0 },
            { 0, 0, 97.7,0},
            {0,0,0,0}
    });

    // initialize filter using first set of measurements from sensors
    public StationaryKalmanFilter(RealVector initialState) {
        // Initial state estimate
        RealVector X0 = initialState;

        ProcessModel pm = new DefaultProcessModel(A, B, Q, X0, P0);
        MeasurementModel mm = new DefaultMeasurementModel(H, R);
        filter = new KalmanFilter(pm, mm);
    }

    // When initializing the filter again with an intermediate error covariance matrix
    public StationaryKalmanFilter(RealVector initialState, RealMatrix errorCovariance) {
        // Initial state estimate
        RealVector X0 = initialState;

        // initial error covariance matrix
        RealMatrix P0 = errorCovariance;

        ProcessModel pm = new DefaultProcessModel(A, B, Q, X0, P0);
        MeasurementModel mm = new DefaultMeasurementModel(H, R);
        filter = new KalmanFilter(pm, mm);
    }

    @Override
    public void predict() {
        filter.predict();
    }

    @Override
    public void correct(RealVector z) {
        Log.i("Filter status", "Filter was corrected");
        prediction = this.getStateEstimationVector(); // stores state vector
        filter.correct(z);
        measurement = z;
        estimate = filter.getStateEstimationVector(); // gets updated state vector
        currentTimeStep += 50; // 50 ms increments
        notifyFilterObservers();
    }

    @Override
    public RealMatrix getStateCovarianceMatrix() {
        return filter.getErrorCovarianceMatrix();
    }

    @Override
    public RealVector getStateEstimationVector() {
        return filter.getStateEstimationVector();
    }

    @Override
    public void registerObserver(FilterObserver o) {
        observers.add(o);
    }

    @Override
    public void removeFilterObserver(FilterObserver o) {
        int i = observers.indexOf(o);
        if (i >= 0) {
            observers.remove(i);
        }
    }

    @Override
    public void notifyFilterObservers() {
        for(int i = 0; i < observers.size(); i++) {
            FilterObserver observer = observers.get(i);
            observer.onFilterUpdate(prediction, measurement, estimate, currentTimeStep);
        }

    }
}

