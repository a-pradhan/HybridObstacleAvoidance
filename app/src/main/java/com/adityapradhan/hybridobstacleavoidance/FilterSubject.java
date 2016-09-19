package com.adityapradhan.hybridobstacleavoidance;

public interface FilterSubject {
    public void registerObserver(FilterObserver o);
    public void removeFilterObserver(FilterObserver o);
    public void notifyFilterObservers();
}