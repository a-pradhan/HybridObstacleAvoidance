package com.adityapradhan.hybridobstacleavoidance;

/*
 * Interface for KalmanFilter classes used to employ Observer pattern.
 * Provide updates for FilterObserver objects
 */
public interface FilterSubject {
    public void registerObserver(FilterObserver o);
    public void removeFilterObserver(FilterObserver o);
    public void notifyFilterObservers();
}