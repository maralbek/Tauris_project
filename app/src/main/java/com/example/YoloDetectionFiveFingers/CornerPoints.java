package com.example.YoloDetectionFiveFingers;

import org.opencv.core.Mat;
import org.opencv.core.Point;

//initialize class
public class CornerPoints {
    private static final String TAG = "OCVSample::Activity";
    public static int ID;
    static int size;
    Mat markerCorners;
    public Point[] cornerValues = new Point[4]; //this are the corner points of a single marker
    public static int[][][] markerfeatures = new int[4][4][3]; //here all of the 16 or 12 corners of 4 or 3 markers are saved

    //class constructor
    CornerPoints(Mat markerCorners, int ID, int size) {
        this.ID = ID;
        this.markerCorners = markerCorners;
        this.size = size;
    }

    //initialize method
    public void getMarkerCorners() {
        //one loop accesses 4 points of  marker. Note that each marker data is sent separately from the Main activity. This class deals with single marker.
        for (int i = 0; i < 4; i++) {
            //use the Mat of markercorners to access the corner values only.
            this.cornerValues[i] = new Point(markerCorners.get(0, i)[0], markerCorners.get(0, i)[1]);
            //this is not necessary and can be removed
            markerfeatures[ID][i][0] = 0;
            //use markercorners to access x and y coordinate of each of the 4 corners of the single marker. 8 points on total
            markerfeatures[ID][i][1] = (int) this.cornerValues[i].x;
            markerfeatures[ID][i][2] = (int) this.cornerValues[i].y;

        }

        markerCorners.release();
        System.gc();

    }


}