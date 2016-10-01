package com.adityapradhan.hybridobstacleavoidance;

import org.apache.commons.math3.linear.RealVector;

/*
 * interface for classes that make use of notifications from KalmanFilter classes that implement the
 * FilterSubject interface
 */
public interface FilterObserver {
    public void onFilterUpdate(RealVector prediction, RealVector measurement, RealVector estimate, int timeStep);
}