package com.adityapradhan.hybridobstacleavoidance;

import org.apache.commons.math3.linear.RealVector;

public interface FilterObserver {
    public void onFilterUpdate(RealVector prediction, RealVector measurement, RealVector estimate, int timeStep);
}