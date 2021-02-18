package com.example.YoloDetectionFiveFingers;
import android.content.res.AssetManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.os.StrictMode;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.dnn.Dnn;
import org.opencv.utils.Converters;
import org.opencv.android.FpsMeter;


import org.opencv.objdetect.QRCodeDetector;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.lang.String;
import static java.lang.System.out;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";
    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    public TextToSpeech tts1;
    String newResult; //This variable needed to compare old and new text which is to be converted to speech
    boolean fetched = false;
    boolean QRdetected = false;
    boolean datafetched = false;
    boolean tell_description = false;
    int step = 40;
    int counter = 0;
    int vibro1 = 0;
    int vibro2 = 0;

    String final_planet = "false";

    FpsMeter fpsMeter = new FpsMeter();

    Net tinyYolo;
    public static String tinyYoloCfg;
    public static String tinyYoloWeights;
    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new
                    StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                switch(status){
                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };

        tts1=new TextToSpeech(getApplicationContext(),
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if(status != TextToSpeech.ERROR){
                            tts1.setLanguage(Locale.getDefault());
                        }
                    }
                });

    }
    public void convertTextToSpeech(String text) {
        if (null == text || "".equals(text)) {
            Log.d(TAG, "Nothing to say");
        }
        tts1.speak(text, TextToSpeech.QUEUE_FLUSH, null, TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED);
    }



    //initialize YoloDetector method. Input frame and transformation matrix are the parameters.
    public Mat yoloDetector(Mat frame, Mat perspectiveTransformation){
        //convert RGBA to RGB
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
        Mat imageBlob = Dnn.blobFromImage(frame, 0.00392, new Size(416,416),new Scalar(0, 0, 0),/*swapRB*/false, /*crop*/false);
        tinyYolo.setInput(imageBlob);
        java.util.List<Mat> result = new java.util.ArrayList<Mat>(2);
        List<String> outBlobNames = new java.util.ArrayList<>();
        outBlobNames.add(0, "yolo_16");
        outBlobNames.add(1, "yolo_23");
        tinyYolo.forward(result,outBlobNames);
        float confThreshold = 0.2f;
        List<Integer> clsIds = new ArrayList<>();
        List<Float> confs = new ArrayList<>();
        List<Rect> rects = new ArrayList<>();
        boolean middledetected = false;
        boolean ringdetected = false;
        boolean pinkydetected = false;
        boolean indexdetected = false;
        List<String> cocoNames = Arrays.asList("Thumb", "Index", "Middle", "Ring", "Pinky");
        int intConf = 0;
        Rect box = new Rect(0,0,0,0);

        for (int i = 0; i < result.size(); ++i)
        {
            Mat level = result.get(i);
            for (int j = 0; j < level.rows(); ++j)
            {
                Mat row = level.row(j);
                Mat scores = row.colRange(5, level.cols());
                Core.MinMaxLocResult mm = Core.minMaxLoc(scores);
                float confidence = (float)mm.maxVal;
                Point classIdPoint = mm.maxLoc;
                if (confidence > confThreshold)
                {
                    int centerX = (int)(row.get(0,0)[0] * frame.cols());
                    int centerY = (int)(row.get(0,1)[0] * frame.rows());
                    int width   = (int)(row.get(0,2)[0] * frame.cols());
                    int height  = (int)(row.get(0,3)[0] * frame.rows());
                    int left    = centerX - width  / 2;
                    int top     = centerY - height / 2;
                    clsIds.add((int)classIdPoint.x);
                    confs.add((float)confidence);
                    rects.add(new Rect(left, top, width, height));
                }
            }
        }

        int ArrayLength = confs.size();
        if (ArrayLength >= 1) {
            // Apply non-maximum suppression procedure.
            float nmsThresh = 0.2f;
            MatOfFloat confidences = new MatOfFloat(Converters.vector_float_to_Mat(confs));
            Rect[] boxesArray = rects.toArray(new Rect[0]);
            MatOfRect boxes = new MatOfRect(boxesArray);
            MatOfInt indices = new MatOfInt();
            Dnn.NMSBoxes(boxes, confidences, confThreshold, nmsThresh, indices);
            // Draw result boxes:
            int[] ind = indices.toArray();
            for (int i = 0; i < ind.length; ++i) {
                int idx = ind[i];
                //idGuy is the id of the finger. 0 is thumb, 1 is index and so on.
                int idGuy = clsIds.get(idx);
                float conf = confs.get(idx);

                //if detected finger is index, we need to store its confidence value and surrounding box coordinates. We will use later to detect the center point
                if(idGuy == 1) {
                    indexdetected = true;
                    intConf = (int) (conf * 100);
                    box = boxesArray[idx];
                }

                //if detected finger is middle, we need to change the boolean to true
                else if(idGuy == 2) {
                    middledetected = true;
                }

                //if detected finger is ring, we need to change the boolean to true
                else if(idGuy == 3) {
                    ringdetected = true;
                }

                //if detected finger is pinky, we need to change the boolean to true
                else if(idGuy == 4) {
                    pinkydetected = true;
                }
            }

            //we proceed only if all fingers are detected. Thumb is not that crucial in our case. Also we return the location of index finger only
            //***change this part 28/08/2020
            if(indexdetected == true && middledetected == true && ringdetected == true && pinkydetected == true) {
                //step is the dimensions of the square cell in pixels
                int step = 15;
                //here we calculate the center of the fingertip from the surrounding box. It is reverse process of center_x and center_y found before
                //box top left angle added by the half of the width/height
                double centerX = box.x + box.width/2;
                double centerY = box.y + box.height/2;
                //here we find the desired points coordinates in the warped image. dst = H * src. Where H is the transformation matrix taken from the function parameter
                double new_centerX = (perspectiveTransformation.get(0,0)[0]*centerX + perspectiveTransformation.get(0,1)[0]*centerY + perspectiveTransformation.get(0,2)[0])/(perspectiveTransformation.get(2,0)[0]*centerX + perspectiveTransformation.get(2,1)[0]*centerY + perspectiveTransformation.get(2,2)[0]);
                double new_centerY = (perspectiveTransformation.get(1,0)[0]*centerX + perspectiveTransformation.get(1,1)[0]*centerY + perspectiveTransformation.get(1,2)[0])/(perspectiveTransformation.get(2,0)[0]*centerX + perspectiveTransformation.get(2,1)[0]*centerY + perspectiveTransformation.get(2,2)[0]);
                //normalize the width and height to 900 and 600 respectively
                new_centerX = new_centerX/GetWarpedFrame.maxWidth*GetWarpedFrame.newWidth;
                new_centerY = new_centerY/GetWarpedFrame.maxHeight*GetWarpedFrame.newHeight;
                //proceed only if the finger position is within the area enclosed by markers
                if (new_centerX >= 0 && new_centerX <= 900 && new_centerY >= 0 && new_centerY <= 600  ) {
                    //find the corresponding cell numbers. Image is 60 (900/step) x 40 (600/step) = 2400 cell sized.
                    int cell_x = (int) Math.ceil(new_centerX / step);
                    int cell_y = (int) Math.ceil(new_centerY / step);
                    // N is the number of cells in one row
                    int N = GetWarpedFrame.newWidth / step;
                    //int N = GetWarpedFrame.newHeight / step;

                    //cell_number is the unique value of each of the  900*600/(15*15) = 2400 cells
                    int cell_number = N * Math.abs(cell_y - 1) + cell_x;
                    //int cell_number = N * Math.abs(cell_y - 1) + cell_x;


                    //access cell element from the excel file using the cell number and convert it  to String

                    //proceed only if the cell value is within the array size
                    if (cell_number >= 0 && cell_number <= 2400) {
                        System.out.println("cell number is " + cell_number);
                        // catch exception if json file was not downloaded
                        try {
                            final_planet = fetchData.list.get(cell_number);
                        } catch(IndexOutOfBoundsException e) {
                            //prevent from multiple TTS
                            if (datafetched == false) {
                                convertTextToSpeech("Please check your internet connection");
                                datafetched = true;
                            }
                            final_planet = "false";
                        }
                        //Only TTS text when the object location is not empty and is changed so one text is not repeated many times
                        if (!final_planet.equals(newResult) && !"false".equals(final_planet)) {
                            convertTextToSpeech(final_planet);
                            newResult = final_planet; //this is used to compare old and new text
                        }
                        //draw the box around the index frame
                        Imgproc.putText(frame, cocoNames.get(1) + " " + intConf + "%", box.tl(), Imgproc.FONT_HERSHEY_COMPLEX, 2, new Scalar(0, 0, 0), 2);
                        Imgproc.rectangle(frame, box.tl(), box.br(), new Scalar(255, 0, 0), 2);
                    }
                }
            }
        }
        //return the frame with bounding box
        return frame;
    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();

        if(QRdetected == false){

            QRCodeDetector qrDecoder = new QRCodeDetector();
            String QRtext = qrDecoder.detectAndDecode(frame);
            //new fetchData().execute("https://api.jsonbin.io/b/5ea97ac94c87c3359a63bd78");//Uk map
            //https://api.jsonbin.io/b/5ea9bb884c87c3359a63db73
            if (counter % step ==0){
                convertTextToSpeech("Please scan the QR code first");
            }
            counter+=1;


            if(QRtext.contains("http")) {
                Log.d("QRcode text: ", QRtext);
                new fetchData().execute(QRtext);//shapes
                QRdetected = true;
                convertTextToSpeech("QR code scanned successfully");


            }


        }

        if(QRdetected == true) {

            Mat ids = new Mat();// needed for Aruco
            List<Mat> corners = new ArrayList<>(); // needed for Aruco
            Dictionary dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_250); // needed for Aruco
            Mat perspectiveTransformation = new Mat();
            //Start detecting Aruco markers
            Aruco.detectMarkers(inputFrame.gray(), dictionary, corners, ids);
            // Initiate vibration
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            // Initiate descriptions when at least one marker is visible
            // it will initiated only once
            if (ids.size(0) >= 2 && tell_description == false) {
                if(fetchData.dict.get("title") != null && !fetchData.dict.get("title").trim().isEmpty()) {
                    String description = fetchData.dict.get("title");
                    convertTextToSpeech(description);
                    tell_description = true;
                }
            }

            int sum = 0;
            // vibrate when more than one and less than three markers are visible
            if (ids.size(0) == 1) {
                //initiate vibro counter
                vibro1 = vibro1+1;
                //System.out.println("Vibro 1 : "+ vibro1);

                //if markers are not visible on 50 consecutive frames vibrate
                if (vibro1 == 50) {
                    // take care of API versions deprecation
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.EFFECT_DOUBLE_CLICK));
                        vibro1 = 0;
                    } else {
                        //deprecated in API 26
                        //pattern: 0-start without delay, 50- duration, 50-pause, 50-duration (double vibration)
                        long[] pattern = {0, 50, 50, 50};
                        // -1 = no repeat
                        v.vibrate(pattern, -1);
                        // start over the counter
                        vibro1 = 0;
                    }
                }
            }

            if (ids.size(0) == 2) {

                //initiate vibro2 counter
                //Notify the user that only two markers are visible and it is advised to recalibrate by making at least 3 markers visible
                vibro2 = vibro2+1;
                //System.out.println("Vibro 2 : "+ vibro2);

                //if markers are not visible on 10 consecutive frames vibrate
                if (vibro2 == 10) {
                    // take care of API versions deprecation
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.EFFECT_DOUBLE_CLICK));
                        vibro2 = 0;
                    } else {
                        //deprecated in API 26
                        //pattern: 0-start without delay, 50- duration (single vibration)
                        long[] pattern = {0, 50};
                        // -1 = no repeat
                        v.vibrate(pattern, -1);
                        // start over the counter
                        vibro2 = 0;
                    }
                }

                fpsMeter.measure();
                int size = 2;
                //iterate over each Aruco marker
                for (int i = 0; i < 2; i++) {
                    int ID = (int) ids.get(i, 0)[0];
                    if (ID < 4) { //sometimes wrong ID numbers are detected. So restricted them to <4
                        Mat markerCorners = corners.get(i);
                        //call the CornerPoints class
                        CornerPoints pointvalues = new CornerPoints(markerCorners, ID, size);
                        //call the method within that class
                        pointvalues.getMarkerCorners();
                    }
                }
                //it looks like Aruco uses old detected coordinate values if new ones are not detected
                //get 2D arrays of 4 corner points. Only one corner of each marker is detected to get a new frame enclosed by inner corners
                int corner_tr[][] = {{CornerPoints.markerfeatures[0][2][1]}, {CornerPoints.markerfeatures[0][2][2]}}; //bottom left corner of TL marker
                int corner_br[][] = {{CornerPoints.markerfeatures[1][3][1]}, {CornerPoints.markerfeatures[1][3][2]}}; //BR corner of TR marker
                int corner_bl[][] = {{CornerPoints.markerfeatures[2][0][1]}, {CornerPoints.markerfeatures[2][0][2]}}; //Tl corner of BR marker
                int corner_tl[][] = {{CornerPoints.markerfeatures[3][1][1]}, {CornerPoints.markerfeatures[3][1][2]}}; //TR corner of BL marker
                GetWarpedFrame finalframe = new GetWarpedFrame(frame, corner_tr, corner_br, corner_bl, corner_tl);
                perspectiveTransformation = finalframe.getTransform();
                frame = yoloDetector(frame, perspectiveTransformation);
            }


            //if only 3 markers are detected
            if (ids.size(0) == 3) {
                vibro1 = 0;
                vibro2 = 0;
                fpsMeter.measure();

                int size = 3;
                for (int i = 0; i < 3; i++) {
                    int ID = (int) ids.get(i, 0)[0];
                    sum = ID + sum;
                }
                // If missing ID is 0 (1+2+3=6). 0 is not considered because detected markers size is 3
                if (sum == 6) {
                    for (int i = 0; i < 3; i++) {
                        int ID = (int) ids.get(i, 0)[0];
                        Mat markerCorners = corners.get(i);
                        CornerPoints pointvalues = new CornerPoints(markerCorners, ID, size);
                        pointvalues.getMarkerCorners();
                    }
                    int corner_br[][] = {{CornerPoints.markerfeatures[1][3][1]}, {CornerPoints.markerfeatures[1][3][2]}}; //BR corner of TR marker
                    int corner_bl[][] = {{CornerPoints.markerfeatures[2][0][1]}, {CornerPoints.markerfeatures[2][0][2]}}; //Tl corner of BR marker
                    int corner_tl[][] = {{CornerPoints.markerfeatures[3][1][1]}, {CornerPoints.markerfeatures[3][1][2]}}; //TR corner of BL marker
                    //missing corner is calculated from the remaining 3 corners. Add x and y values of diagonal corner points and subtract the corner points of the remaining marker
                    int corner_tr[][] = {{corner_tl[0][0] + corner_br[0][0] - corner_bl[0][0]}, {corner_tl[1][0] + corner_br[1][0] - corner_bl[1][0]}};
                    //use the calculated corner points to find a warped frame
                    GetWarpedFrame finalframe = new GetWarpedFrame(frame, corner_tr, corner_br, corner_bl, corner_tl);
                    //access the getwarped class to take the perspectiveTransformation matrix
                    perspectiveTransformation = finalframe.getTransform();
                    //call Yolodetector method. Note that Yolo is detecting finger coordinates on the input frame and then these coordinates are mapped to the warped frame
                    frame = yoloDetector(frame, perspectiveTransformation);
                }

                // If missing ID is 1 (0+2+3=5)
                else if (sum == 5) {
                    for (int i = 0; i < 3; i++) {
                        int ID = (int) ids.get(i, 0)[0];
                        Mat markerCorners = corners.get(i);
                        CornerPoints pointvalues = new CornerPoints(markerCorners, ID, size);
                        pointvalues.getMarkerCorners();
                    }
                    int corner_tr[][] = {{CornerPoints.markerfeatures[0][2][1]}, {CornerPoints.markerfeatures[0][2][2]}}; //bottom left corner of TL marker
                    int corner_bl[][] = {{CornerPoints.markerfeatures[2][0][1]}, {CornerPoints.markerfeatures[2][0][2]}}; //Tl corner of BR marker
                    int corner_tl[][] = {{CornerPoints.markerfeatures[3][1][1]}, {CornerPoints.markerfeatures[3][1][2]}}; //TR corner of BL marker
                    int corner_br[][] = {{corner_tr[0][0] + corner_bl[0][0] - corner_tl[0][0]}, {corner_tr[1][0] + corner_bl[1][0] - corner_tl[1][0]}};
                    GetWarpedFrame finalframe = new GetWarpedFrame(frame, corner_tr, corner_br, corner_bl, corner_tl);
                    perspectiveTransformation = finalframe.getTransform();
                    frame = yoloDetector(frame, perspectiveTransformation);
                }

                // If missing ID is 2 (0+1+3=4)
                else if (sum == 4) {
                    for (int i = 0; i < 3; i++) {
                        int ID = (int) ids.get(i, 0)[0];
                        Mat markerCorners = corners.get(i);
                        CornerPoints pointvalues = new CornerPoints(markerCorners, ID, size);
                        pointvalues.getMarkerCorners();
                    }
                    int corner_tr[][] = {{CornerPoints.markerfeatures[0][2][1]}, {CornerPoints.markerfeatures[0][2][2]}}; //bottom left corner of TL marker
                    int corner_br[][] = {{CornerPoints.markerfeatures[1][3][1]}, {CornerPoints.markerfeatures[1][3][2]}}; //BR corner of TR marker
                    int corner_tl[][] = {{CornerPoints.markerfeatures[3][1][1]}, {CornerPoints.markerfeatures[3][1][2]}}; //TR corner of BL marker
                    int corner_bl[][] = {{corner_br[0][0] + corner_tl[0][0] - corner_tr[0][0]}, {corner_br[1][0] + corner_tl[1][0] - corner_tr[1][0]}};
                    GetWarpedFrame finalframe = new GetWarpedFrame(frame, corner_tr, corner_br, corner_bl, corner_tl);
                    perspectiveTransformation = finalframe.getTransform();
                    frame = yoloDetector(frame, perspectiveTransformation);
                }

                // If missing ID is 3 (0+1+2=3)
                else if (sum == 3) {
                    for (int i = 0; i < 3; i++) {
                        int ID = (int) ids.get(i, 0)[0];
                        Mat markerCorners = corners.get(i);
                        CornerPoints pointvalues = new CornerPoints(markerCorners, ID, size);
                        pointvalues.getMarkerCorners();
                    }
                    int corner_tr[][] = {{CornerPoints.markerfeatures[0][2][1]}, {CornerPoints.markerfeatures[0][2][2]}}; //bottom left corner of TL marker
                    int corner_br[][] = {{CornerPoints.markerfeatures[1][3][1]}, {CornerPoints.markerfeatures[1][3][2]}}; //BR corner of TR marker
                    int corner_bl[][] = {{CornerPoints.markerfeatures[2][0][1]}, {CornerPoints.markerfeatures[2][0][2]}}; //Tl corner of BR marker
                    int corner_tl[][] = {{corner_tr[0][0] + corner_bl[0][0] - corner_br[0][0]}, {corner_tr[1][0] + corner_bl[1][0] - corner_br[1][0]}};
                    GetWarpedFrame finalframe = new GetWarpedFrame(frame, corner_tr, corner_br, corner_bl, corner_tl);
                    perspectiveTransformation = finalframe.getTransform();
                    frame = yoloDetector(frame, perspectiveTransformation);
                }
            }

            //if all of the markers detected
            if (ids.size(0) > 3) {
                vibro1 = 0;
                vibro2 = 0;
                fpsMeter.measure();
                int size = 4;
                //iterate over each Aruco marker
                for (int i = 0; i < 4; i++) {
                    int ID = (int) ids.get(i, 0)[0];
                    if (ID < 4) { //sometimes wrong ID numbers are detected. So restricted them to <4
                        Mat markerCorners = corners.get(i);
                        //call the CornerPoints class
                        CornerPoints pointvalues = new CornerPoints(markerCorners, ID, size);
                        //call the method within that class
                        pointvalues.getMarkerCorners();
                    }
                }
                //get 2D arrays of 4 corner points. Only one corner of each marker is detected to get a new frame enclosed by inner corners
                int corner_tr[][] = {{CornerPoints.markerfeatures[0][2][1]}, {CornerPoints.markerfeatures[0][2][2]}}; //bottom left corner of TL marker
                int corner_br[][] = {{CornerPoints.markerfeatures[1][3][1]}, {CornerPoints.markerfeatures[1][3][2]}}; //BR corner of TR marker
                int corner_bl[][] = {{CornerPoints.markerfeatures[2][0][1]}, {CornerPoints.markerfeatures[2][0][2]}}; //Tl corner of BR marker
                int corner_tl[][] = {{CornerPoints.markerfeatures[3][1][1]}, {CornerPoints.markerfeatures[3][1][2]}}; //TR corner of BL marker
                GetWarpedFrame finalframe = new GetWarpedFrame(frame, corner_tr, corner_br, corner_bl, corner_tl);
                perspectiveTransformation = finalframe.getTransform();
                frame = yoloDetector(frame, perspectiveTransformation);
            }


            perspectiveTransformation.release();

            System.gc();
        }
        return frame;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        //initialize appropriate cfg and weights files for detection
        tinyYoloCfg = getPath("yolov3-tiny_custom.cfg", this);
        tinyYoloWeights = getPath("yolov3-tiny_custom_last.weights", this);
        tinyYolo = Dnn.readNetFromDarknet(tinyYoloCfg, tinyYoloWeights);


    }
    @Override
    public void onCameraViewStopped() {
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"There's a problem, yo!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }
}





