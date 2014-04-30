package cz.marstaj.metrocatcher;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class ACCSensor {

    /**
     * Sensor manager
     */
    private final SensorManager mSensorManager;

    /**
     * Accelerometer data listener
     */
    SensorEventListener sensorListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            double x = sensorEvent.values[0];
            double y = sensorEvent.values[1];
            double z = sensorEvent.values[2];
            onACCReceivedListener.onReceived(x, y, z);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    /**
     * Context
     */
    private Context context;

    /**
     * Accelerometer
     */
    private Sensor mAccelerometer;

    /**
     * Accelerometer data received listener
     */
    private OnACCReceivedListener onACCReceivedListener;

    public ACCSensor(Context context, OnACCReceivedListener onACCReceivedListener) {
        this.context = context;
        this.onACCReceivedListener = onACCReceivedListener;
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    /**
     * Start receiving accelerometer data
     */
    public void start() {
        Log.v("ACCSensor", "start");
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(sensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    /**
     * Start receiving accelerometer data
     */
    public void stop() {
        Log.v("ACCSensor", "stop");
        mSensorManager.unregisterListener(sensorListener);
    }
}