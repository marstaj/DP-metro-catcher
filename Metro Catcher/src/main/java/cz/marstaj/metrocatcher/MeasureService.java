package cz.marstaj.metrocatcher;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import cz.marstaj.metrocatcher.alg.FFT;
import cz.marstaj.metrocatcher.alg.MergeSort;
import cz.marstaj.metrocatcher.alg.MovementNeuralNetwork;
import cz.marstaj.metrocatcher.model.ClassType;
import cz.marstaj.metrocatcher.model.DataModel;
import cz.marstaj.metrocatcher.util.LocalBinder;

/**
 * Created by mastajner on 12/12/13.
 */
public class MeasureService extends Service {

    public static final String UPDATE = "cz.marstaj.metrocatcher";
    public static final String UPDATE_MESSAGE = "UPDATE_MESSAGE";

    /**
     * Service TAG
     */
    private static final String TAG = MeasureService.class.getSimpleName();

    /**
     * Flag whether the service is running or not
     */
    public static boolean isRunning = false;

    /**
     * Time interval for switching windows
     */
    private final int INTERVAL = 2100; // 2100 ms should be enough for 256 samples window = 8 ms per value

    /**
     * Window size
     */
    private final int WINDOW_SIZE = 256;

    // Calibrating values for different sections
    private final double step1 = 100;
    private final double step2 = 50;
    private final double step3 = 20;
    private final double step4 = 10;

    // Speed values for different movements
    private final double floorSpeedPerWindow = 3.4; // 3.4m per 2s
    private final double escalatorSpeedPerWindow = 1.5; // 1.5 per 2s
    private final double escalatorWalkSpeedPerWindow = 2.7; //2.7m per 2s

    /**
     * Accelerometer object
     */
    private ACCSensor accSensor;

    /**
     * Timer for switching data lists
     */
    private Handler timer;

    /**
     * Time of last accelerometer value
     */
    private long lastTime;

    // Saved accelerometer data
    private ArrayList<Double> accData1;
    private ArrayList<Long> accTime1;
    private ArrayList<Double> accData2;
    private ArrayList<Long> accTime2;

    /**
     * Movement neural network
     */
    private MovementNeuralNetwork net;
    /**
     * Actual terrain type
     */
    private TerrainType activeTerrain = TerrainType.UNKNOWN;
    /**
     * Switching data list 1
     */
    Runnable switchLists1 = new Runnable() {
        @Override
        public void run() {
            ArrayList<Double> tmpData = accData1;
            ArrayList<Long> tmpTime = accTime1;

            resetDataLists1();
            timer.postDelayed(switchLists1, INTERVAL);

            // Analyze window
            double netResult = postProcessAccData(tmpData, tmpTime);
            ClassType classType = getActionClassFromNetResult(netResult);
            onNewClassType(classType);
        }
    };
    // Station model variables
    private int classCount;
    private int classCountError;
    /**
     * Switching data list 2
     */
    Runnable switchLists2 = new Runnable() {
        @Override
        public void run() {
            ArrayList<Double> tmpData = accData2;
            ArrayList<Long> tmpTime = accTime2;

            resetDataLists2();
            timer.postDelayed(switchLists2, INTERVAL);

            // Analyze window
            double netResult = postProcessAccData(tmpData, tmpTime);
            ClassType classType = getActionClassFromNetResult(netResult);
            onNewClassType(classType);
        }
    };
    private double meters;
    private boolean wasStep1 = false;
    private boolean wasStep2 = false;
    private boolean wasStep3 = false;

    /**
     * Linear interpolation
     *
     * @param x
     * @param y
     * @param xi
     * @return
     * @throws IllegalArgumentException
     */
    public static final double[] interpolateLinear(Long[] x, Double[] y, long[] xi) throws IllegalArgumentException {

        if (x.length != y.length) {
            throw new IllegalArgumentException("X and Y must be the same length");
        }
        if (x.length == 1) {
            throw new IllegalArgumentException("X must contain more than one value");
        }
        double[] dx = new double[x.length - 1];
        double[] dy = new double[x.length - 1];
        double[] slope = new double[x.length - 1];
        double[] intercept = new double[x.length - 1];

        // Calculate the line equation (i.e. slope and intercept) between each point
        for (int i = 0; i < x.length - 1; i++) {
            dx[i] = x[i + 1] - x[i];
            if (dx[i] == 0) {
                throw new IllegalArgumentException("X must be montotonic. A duplicate " + "x-value was found");
            }
            if (dx[i] < 0) {
                throw new IllegalArgumentException("X must be sorted");
            }
            dy[i] = y[i + 1] - y[i];
            slope[i] = dy[i] / dx[i];
            intercept[i] = y[i] - x[i] * slope[i];
        }

        // Perform the interpolation here
        double[] yi = new double[xi.length];
        for (int i = 0; i < xi.length; i++) {
            if ((xi[i] > x[x.length - 1]) || (xi[i] < x[0])) {
                yi[i] = Double.NaN;
            } else {
                int loc = Arrays.binarySearch(x, xi[i]);
                if (loc < -1) {
                    loc = -loc - 2;
                    yi[i] = slope[loc] * xi[i] + intercept[loc];
                } else {
                    yi[i] = y[loc];
                }
            }
        }

        return yi;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");

        // Make sure service will not get killed
        Notification notification = new Notification(R.drawable.ic_launcher, getString(R.string.measure_notification_text), System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // ???? needed?
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, TAG, "is running", pendingIntent);
        startForeground(MyApp.MEASURE_NOTIFICATION, notification);

        // Init
        timer = new Handler();
        resetDataLists1();
        resetDataLists2();
        activeTerrain = TerrainType.UNKNOWN;

        accSensor = new ACCSensor(this, new OnACCReceivedListener() {

            @Override
            public void onReceived(double x, double y, double z) {
                processAccData(x, y, z);
            }
        });

        DataModel.initWithContext(this);
        net = new MovementNeuralNetwork(DataModel.getInitialWeights(), DataModel.getLayerWeights(), DataModel.getBiases1(), DataModel.getBias2());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");

        if (!isRunning) {
            isRunning = true;

            // Start the measuring
            startMeasure();
        }

        return START_STICKY;
    }

    /**
     * Start measuring acclerometer data
     */
    private void startMeasure() {
        timer.removeCallbacks(switchLists1);
        timer.removeCallbacks(switchLists2);
        resetDataLists1();
        resetDataLists2();
        lastTime = System.currentTimeMillis();
        timer.postDelayed(switchLists1, INTERVAL);
        timer.postDelayed(switchLists2, INTERVAL + INTERVAL / 2); // Windowed moved by 50%
        accSensor.start();
    }

    /**
     * Stop measuring acclerometer data
     */
    private void endMeasure() {
        accSensor.stop();
        timer.removeCallbacks(switchLists1);
        timer.removeCallbacks(switchLists2);
    }

    /**
     * Reset data list 1
     */
    private void resetDataLists1() {
        accData1 = new ArrayList<Double>(300);
        accTime1 = new ArrayList<Long>(300);
    }

    /**
     * Reset data list 2
     */
    private void resetDataLists2() {
        accData2 = new ArrayList<Double>(300);
        accTime2 = new ArrayList<Long>(300);
    }

    /**
     * Save obtained accelerometer data
     *
     * @param x
     * @param y
     * @param z
     */
    private void processAccData(double x, double y, double z) {
        // Ignore acc signal in the same milisecond
        long time = System.currentTimeMillis();
        if (lastTime < time) {
            lastTime = time;

            // Merge 3-axis to one signal
            double mergedACC = mergeAcc(x, y, z);

            // Add to the list
            accData1.add(mergedACC);
            accData2.add(mergedACC);
            accTime1.add(time);
            accTime2.add(time);
        }
    }

    /**
     * Analyze window for features and classify it
     *
     * @param data
     * @param time
     * @return
     */
    private double postProcessAccData(ArrayList<Double> data, ArrayList<Long> time) {
        // ---------- LINEAR INTERPOLATION ----------
        long startTime = time.get(0);
        long[] linTime = new long[WINDOW_SIZE];
        for (int i = 0; i < WINDOW_SIZE; i++) {
            linTime[i] = startTime + i * 8;
        }

        // Convert lists to arrays
        Double[] tmpData = data.toArray(new Double[data.size()]);
        Long[] tmpTime = time.toArray(new Long[time.size()]);

        // Get Linearized data
        double[] linData = interpolateLinear(tmpTime, tmpData, linTime);

        // ---------- SMOOTH DATA ----------
        linData = ema(linData);

        // ---------- FEATURES ----------
        FFT fft = new FFT(WINDOW_SIZE);

        // Prepare arrays for results
        double[] resultFFTreal = linData.clone();
        double[] resultFFTimag = new double[WINDOW_SIZE];

        // Run fft
        fft.fft(resultFFTreal, resultFFTimag);
        resultFFTreal[0] = 0; // Remove huge spike at the beginning, we don't need that noise
        resultFFTimag[0] = 0; // Remove huge spike at the beginning, we don't need that noise

        // First iteration over data for basic features
        double sumForMean = 0;
        double sumForRms = 0;
        double maxFFT = Integer.MIN_VALUE;

        for (int i = 0; i < linData.length; i++) {
            double value = linData[i];

            double fftVal = Math.sqrt(Math.pow(resultFFTreal[i], 2) + Math.pow(resultFFTimag[i], 2)); // ABS value of complex number
            if (fftVal > maxFFT) {
                maxFFT = fftVal; // MAX FFT
            }
            sumForMean += value; // MEAN
            sumForRms += (value * value); // RMS
        }
        // Calculate MEAN and RMS
        double mean = sumForMean / linData.length;
        double rms = Math.sqrt(sumForRms / linData.length);


        // Second iteration over data for advanced features. We already use some of the basic features to calculate more advance ones
        double sumStd = 0;
        double sumKurtosis = 0;
        double sumSkewness = 0;

        for (int i = 0; i < linData.length; i++) {
            double val = linData[i];

            sumStd += Math.pow(val - mean, 2);
            sumKurtosis += Math.pow(val - mean, 4);
            sumSkewness += Math.pow(val - mean, 3);
        }
        // Calculate VARIANCE, STD, KURTOSIS and SKEWNESS
        double variance = (1d / (linData.length - 1)) * sumStd;
        double std = Math.sqrt(variance);
        double kurtosis = ((1d / linData.length) * sumKurtosis) / Math.pow((1d / linData.length) * sumStd, 2);
        double skewness = ((1d / linData.length) * sumSkewness) / Math.pow(Math.sqrt((1d / linData.length) * sumStd), 3);

        // Features that require sorted array
        MergeSort.mergeSort(linData);

        // Calculate MAX, MIN, RANGE, MEDIAN and IRQ
        double max = linData[linData.length - 1];
        double min = linData[0];
        double range = max - min;
        double median = (linData[(WINDOW_SIZE / 2) - 1] + linData[256 / 2]) / 2;
        double q1 = (linData[(WINDOW_SIZE / 4) - 1] + linData[WINDOW_SIZE / 4]) / 2;
        double q2 = (linData[(WINDOW_SIZE / 4) - 1 + (WINDOW_SIZE / 2)] + linData[(WINDOW_SIZE / 4) + (WINDOW_SIZE / 2)]) / 2;
        double iqr = q2 - q1;

        // Array of features for this window
        double[] features = new double[]{maxFFT, max, min, mean, median, kurtosis, skewness, std, rms, variance, range, iqr};

        return net.classifyMovement(features);
    }

    /**
     * Polish neural network result
     *
     * @param netResult
     * @return
     */
    private ClassType getActionClassFromNetResult(double netResult) {
        if (Double.isNaN(netResult)) {
            return null;
        }
        netResult = Math.abs(netResult);

        if (netResult < 1.55) {
            return ClassType.ESCALATOR_STAY;
        } else {
            if (netResult < 2.55) {
                return ClassType.WALKING;
            } else {
                if (netResult < 3.55) {
                    return ClassType.STAIRS_DOWN;
                } else {
                    return ClassType.ESCALATOR_WALKING;
                }
            }
        }
    }

    /**
     * On new classified window
     *
     * @param newClass
     */
    private void onNewClassType(ClassType newClass) {
        if (newClass != null) {
            switch (activeTerrain) {
                case UNKNOWN: {
                    boolean wasChange = checkActionChange(newClass, TerrainType.ESCALATOR);
                    if (wasChange) {
                        Log.d(TAG, "UNKNOWN -> ESCALATOR"); // 2 * because even though it is 4 windows, they 50% overlap
                        meters = step1 - 2 * escalatorWalkSpeedPerWindow;
                        wasStep1 = true;
                    }
                    break;
                }

                case ESCALATOR: {
                    // Subtract meters per window
                    if (newClass == ClassType.ESCALATOR_STAY || newClass == ClassType.STAIRS_DOWN) {
                        meters -= escalatorSpeedPerWindow / 2; // Divide by 2 because windows 50% overlap
                    } else {
                        meters -= escalatorWalkSpeedPerWindow / 2; // Divide by 2 because windows 50% overlap
                    }

                    // Check change
                    boolean wasChange = checkActionChange(newClass, TerrainType.FLOOR);
                    if (wasChange) {
                        Log.d(TAG, "ESCALATOR -> FLOOR");
                        if (wasStep3) {
                            meters = step4 - 2 * escalatorWalkSpeedPerWindow;
                        } else {
                            if (wasStep1) {
                                meters = step2 - 2 * escalatorWalkSpeedPerWindow;
                                wasStep2 = true;
                            }
                        }
                    }
                    break;
                }

                case FLOOR: {
                    // Subtract meters per window
                    meters -= floorSpeedPerWindow / 2; // Divide by 2 because windows 50% overlap

                    // Check change
                    boolean wasChange = checkActionChange(newClass, TerrainType.ESCALATOR);
                    if (wasChange) {
                        Log.d(TAG, "FLOOR -> ESCALATOR");
                        if (wasStep2) {
                            meters = step3 - 2 * escalatorWalkSpeedPerWindow; // Divide by 2 because windows 50% overlap
                            wasStep3 = true;
                        }
                    }
                    break;
                }
            }

            Log.i(TAG, "Meters: " + meters);
            if (meters >= 0) {
                // Publish results to Main activity
                publishResults(meters);
            } else {
                // Publish results to Main activity
                publishResults(meters);
                endMeasure();
                stopForeground(true);
                isRunning = false;
            }
        }
    }

    /**
     * Check wheter there was an action change
     *
     * @param newClass
     * @param wantedClass
     * @return
     */
    private boolean checkActionChange(ClassType newClass, TerrainType wantedClass) {
        boolean ok = false;
        if (wantedClass == TerrainType.ESCALATOR) {
            if (newClass == ClassType.ESCALATOR_STAY || newClass == ClassType.ESCALATOR_WALKING) {
                ok = true;
            }
        } else {
            if (wantedClass == TerrainType.FLOOR) {
                if (newClass == ClassType.WALKING) {
                    ok = true;
                }
            }
        }
        if (ok) {
            classCount++;
            classCountError = 0;
            if (classCount == 4) {
                classCount = 0;
                classCountError = 0;
                activeTerrain = wantedClass;
                return true;
            }
        } else {
            classCountError++;
            if (classCountError == 2) {
                classCount = 0;
                classCountError = 0;
            }
        }
        return false;
    }

    /**
     * Send results to the Main activity
     */
    private void publishResults(double meters) {
        Intent intent = new Intent(UPDATE);
        intent.putExtra(UPDATE_MESSAGE, meters);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        // Returns binder for communication
        return new LocalBinder<MeasureService>(this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        endMeasure();
        stopForeground(true);
        isRunning = false;
    }

    /**
     * Exponential moving-average filter
     *
     * @param unfiltered
     * @return
     */
    private double[] ema(double[] unfiltered) {
        double[] filtered = new double[unfiltered.length];

        double alpha = 0.1;
        double gFilt = unfiltered[0];

        for (int i = 0; i < unfiltered.length; i++) {
            double g = unfiltered[i];
            gFilt = (1 - alpha) * gFilt + alpha * g;
            filtered[i] = gFilt;
        }

        return filtered;
    }

    /**
     * Merge 3-axis signal to 1-axis acceleration
     *
     * @param x
     * @param y
     * @param z
     * @return
     */
    private double mergeAcc(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    private enum TerrainType {
        FLOOR, STAIRS, ESCALATOR, UNKNOWN
    }


}