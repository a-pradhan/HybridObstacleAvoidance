package com.adityapradhan.hybridobstacleavoidance;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * Created by aditya on 9/28/16.
 * Interface implemented by Kalman Filter classes
 * Can be used to implement a strategy pattern for changing the type of Kalman Filter used for different
 * events at run time e.g stationary vs moving
 */
public interface KFilter {
    public void predict();

    public void correct(RealVector z);

    public RealMatrix getStateCovarianceMatrix();

    public RealVector getStateEstimationVector();

}
