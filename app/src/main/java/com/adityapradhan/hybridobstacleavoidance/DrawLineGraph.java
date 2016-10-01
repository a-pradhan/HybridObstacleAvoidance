package com.adityapradhan.hybridobstacleavoidance;

import android.graphics.Color;
import android.util.Log;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.commons.math3.linear.RealVector;

/**
 * Created by Aditya on 9/18/2016.
 * DrawGraph class is used to produce Graph of measurement, predicted state and filter estimate while the filter is running
 */
public class DrawLineGraph implements FilterObserver {
    private double prediction; // prediction based on model
    private double measurement;
    private double estimate; // prediction based on filter estimate updated with measurement
    private int timeStep;
    private String graphName;
    private int stateVectorIndex;


    private LineGraphSeries<DataPoint> predictionSeries = new LineGraphSeries<DataPoint>();
    private LineGraphSeries<DataPoint> measurementSeries = new LineGraphSeries<DataPoint>();
    private LineGraphSeries<DataPoint> estimateSeries = new LineGraphSeries<DataPoint>();
    private GraphView graphView;


    // initialize only with graph name and state vector index
    public DrawLineGraph(String graphName, int stateVectorIndex, GraphView graphView) {
        this.graphName = graphName;
        this.stateVectorIndex = stateVectorIndex;
        this.graphView = graphView;
        drawGraph();
    }



    @Override
    public void onFilterUpdate(RealVector prediction, RealVector measurement, RealVector estimate, int timeStep) {
        Log.i("Graph Status", "filter updated");
        this.prediction = prediction.getEntry(stateVectorIndex);
        this.measurement = measurement.getEntry(stateVectorIndex);
        this.estimate = estimate.getEntry(stateVectorIndex);
        this.timeStep = timeStep;
        addEntries();


    }

    // initial setup of graph and drawing
    public void drawGraph() {
        graphView.addSeries(predictionSeries);
        graphView.addSeries(measurementSeries);
        graphView.addSeries(estimateSeries);
        graphView.setTitle(graphName);

        Viewport viewport = graphView.getViewport();

        // set manual X bounds
        viewport.setXAxisBoundsManual(true);
        viewport.setMinX(0);
        viewport.setMaxX(700);
        viewport.setScrollable(true);

        predictionSeries.setTitle("prediction");
        predictionSeries.setColor(Color.BLACK);
        measurementSeries.setTitle("measurement");
        measurementSeries.setColor(Color.RED);
        estimateSeries.setTitle("estimate");
        estimateSeries.setColor(Color.GREEN);


        // display legend
        graphView.getLegendRenderer().setVisible(true);
    }

   // add new entries to existing series
   public void addEntries() {
       Log.i("Timestep value", Integer.toString(timeStep));
       predictionSeries.appendData(new DataPoint(timeStep,prediction), true, 1000);
       measurementSeries.appendData(new DataPoint(timeStep, measurement ), true, 1000);
       estimateSeries.appendData(new DataPoint(timeStep, estimate), true, 1000);
   }



}
