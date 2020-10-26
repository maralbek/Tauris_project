package com.example.YoloDetectionFiveFingers;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class GetWarpedFrame {
    static Mat mRgba;
    int x1;
    int y1;
    int x2;
    int y2;
    int x3;
    int y3;
    int x4;
    int y4;
    public static double maxWidth;
    public static double maxHeight;
    public static int newHeight = 600;
    public static int newWidth = 900;

    GetWarpedFrame(Mat mRgba, int[][] corner_tr, int[][] corner_br, int[][] corner_bl, int[][] corner_tl) {
    //GetWarpedFrame(Mat mRgba, int[][] corner_tl, int[][] corner_tr, int[][] corner_br, int[][] corner_bl) {

        // corners order changed because landscape orientation is used. Aruco marker 3 is top left now
        this.x1 = corner_tl[0][0];
        this.y1 = corner_tl[1][0];
        this.x2 = corner_tr[0][0];
        this.y2 = corner_tr[1][0];
        this.x3 = corner_br[0][0];
        this.y3 = corner_br[1][0];
        this.x4 = corner_bl[0][0];
        this.y4 = corner_bl[1][0];
        this.mRgba = mRgba;
    }

    public Mat getTransform() {
        //find maximum width of the new warped frame
        double widthA = Math.sqrt((Math.pow((x3 - x4), 2)) + (Math.pow((y3 - y4), 2)));
        double widthB = Math.sqrt((Math.pow((x2 - x1), 2)) + (Math.pow((y2 - y1), 2)));
        maxWidth = Math.max(widthA, widthB);

        //find maximum height
        double heightA = Math.sqrt((Math.pow((x2 - x3), 2)) + (Math.pow((y2 - y3), 2)));
        double heightB = Math.sqrt((Math.pow((x1 - x4), 2)) + (Math.pow((y1 - y4), 2)));
        maxHeight = Math.max(heightA, heightB);

        //create a matrix of the destination warped frame
        List<Point> dstPoints = new ArrayList<>();
        dstPoints.add(new Point(0, 0));
        dstPoints.add(new Point(maxWidth - 1, 0));
        dstPoints.add(new Point(maxWidth - 1, maxHeight - 1));
        dstPoints.add(new Point(0, maxHeight-1));
        Mat dstMat = Converters.vector_Point2f_to_Mat(dstPoints);

        //create a matrix of the source warped frame
        List<Point> srcPoints = new ArrayList<>();
        srcPoints.add(new Point(x1, y1));
        srcPoints.add(new Point(x2, y2));
        srcPoints.add(new Point(x3, y3));
        srcPoints.add(new Point(x4, y4));

        Mat srcMat = Converters.vector_Point2f_to_Mat(srcPoints);

        //get the transformation matrix
        Mat perspectiveTransformation = Imgproc.getPerspectiveTransform(srcMat, dstMat);

        srcMat.release();
        dstMat.release();
        System.gc();

        //return the transformation matrix
        return perspectiveTransformation;
    }
}
