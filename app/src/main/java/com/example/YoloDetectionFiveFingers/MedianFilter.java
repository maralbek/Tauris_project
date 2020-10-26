package com.example.YoloDetectionFiveFingers;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class MedianFilter {
    private final static Queue<Double> window = new LinkedList<>();
    private final int period;
    private int sum;
    double[] array = new double[9];
    double median;
    private static final String TAG = "OCVSample::Activity";




    public MedianFilter(int period) {
        assert period > 0 : "Period must be a positive integer";
        this.period = period;
    }

    public double newNum(int num) {
        window.add((double) num);
        if (window.size() > 9) {
            window.remove();
            Object[] objectArray = window.toArray();
            //int length = objectArray.length;;
            for (int i = 0; i < 9; i++) {
                array[i] = (double) objectArray[i];
            }
            Arrays.sort(array);
            median = array[5];


        }
        //Log.i(TAG, "inside object cell number " + window);
        System.gc();

        return median;


    }

}