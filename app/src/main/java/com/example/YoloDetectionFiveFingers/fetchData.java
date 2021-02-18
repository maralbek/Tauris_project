package com.example.YoloDetectionFiveFingers;


import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;
import static java.lang.System.out;

public class fetchData extends AsyncTask<String, String, String> {

    public static List<String> list = new ArrayList<String>();
    public static HashMap<String, String> dict = new HashMap<String, String>();
    String fetched_data;



    protected String doInBackground(String... params) {


        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();


            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            String line = "";

            while ((line = reader.readLine()) != null) {

                fetched_data = line;
                Log.d("Response: ", "> " + fetched_data);   //here u ll get whole response...... :-)

            }
            fetched_data = fetched_data.substring(1, fetched_data.length()-1);//remove curly brackets
            fetched_data = fetched_data.replaceAll("\"", "");
            String[] keyValuePairs = fetched_data.split(",");


            for (String s : keyValuePairs) {
                String key = s.split(":")[0];
                String value = s.split(":")[1];
                dict.put(key,value);

            }

            list = Arrays.asList(fetched_data.split(","));

            return null;


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
