package cz.marstaj.metrocatcher.model;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by mastajner on 14/03/14.
 */
public class DataModel {

    private static double[] layerWeights = null;
    private static double[][] initialWeights = null;
    private static double[] biases1 = null;
    private static Double bias2 = null;
    private static Context context;

    public static void initWithContext(Context context) {
        DataModel.context = context;
    }

    public static double[] getLayerWeights() {
        if (layerWeights == null) {
            try {
                layerWeights = readLW(context);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return layerWeights;
    }

    public static double[][] getInitialWeights() {
        if (initialWeights == null) {
            try {
                initialWeights = readIW(context);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return initialWeights;
    }

    public static double[] getBiases1() {
        if (biases1 == null) {
            try {
                biases1 = readB1(context);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return biases1;
    }

    public static Double getBias2() {
        if (bias2 == null) {
            try {
                bias2 = readB2(context);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return bias2;
    }

    public static boolean loadAllData() {
        try {
            initialWeights = readIW(context);
            layerWeights = readLW(context);
            biases1 = readB1(context);
            bias2 = readB2(context);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    private static double[][] readIW(Context context) throws IOException {
        AssetManager am = context.getAssets();
        InputStream is = am.open("netIW.txt");
        InputStreamReader inputStreamReader = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(inputStreamReader);
        ArrayList<String> list = new ArrayList<String>();
        String line;
        while ((line = br.readLine()) != null) {
            list.add(line);
        }
        br.close();
        inputStreamReader.close();
        is.close();
        double[][] result = new double[list.size()][list.get(0).split(",").length];
        for (int i = 0; i < list.size(); i++) {
            String[] strArray = list.get(i).split(",");
            double[] res = new double[strArray.length];
            for (int j = 0; j < strArray.length; j++) {
                res[j] = Float.valueOf(strArray[j]);
            }
            result[i] = res;
        }

        return result;
    }

    private static double[] readLW(Context context) throws IOException {
        AssetManager am = context.getAssets();
        InputStream is = am.open("netLW.txt");
        InputStreamReader inputStreamReader = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(inputStreamReader);
        ArrayList<String> list = new ArrayList<String>();
        String line;
        while ((line = br.readLine()) != null) {
            list.add(line);
        }
        br.close();
        inputStreamReader.close();
        is.close();
        String[] strArray = list.get(0).split(",");
        double[] result = new double[strArray.length];
        for (int i = 0; i < strArray.length; i++) {
            result[i] = Double.valueOf(strArray[i]);
        }
        return result;
    }

    private static double readB2(Context context) throws IOException {
        AssetManager am = context.getAssets();
        InputStream is = am.open("netB2.txt");
        InputStreamReader inputStreamReader = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(inputStreamReader);
        ArrayList<Double> list = new ArrayList<Double>();
        String line;
        while ((line = br.readLine()) != null) {
            list.add(Double.valueOf(line));
        }
        br.close();
        inputStreamReader.close();
        is.close();
        return list.get(0);
    }

    private static double[] readB1(Context context) throws IOException {
        AssetManager am = context.getAssets();
        InputStream is = am.open("netB1.txt");
        InputStreamReader inputStreamReader = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(inputStreamReader);
        ArrayList<Double> list = new ArrayList<Double>();
        String line;
        while ((line = br.readLine()) != null) {
            list.add(Double.valueOf(line));
        }
        br.close();
        inputStreamReader.close();
        is.close();
        double[] result = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }
}
