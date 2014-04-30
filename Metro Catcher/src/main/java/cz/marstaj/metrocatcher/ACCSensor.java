package cz.marstaj.metrocatcher;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class ACCSensor {

    private final SensorManager mSensorManager;
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
    private Context context;
    private Sensor mAccelerometer;
    private OnACCReceivedListener onACCReceivedListener;

    public ACCSensor(Context context, OnACCReceivedListener onACCReceivedListener) {
        this.context = context;
        this.onACCReceivedListener = onACCReceivedListener;
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    public void start() {
        Log.v("ACCSensor", "start");
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(sensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void stop() {
        Log.v("ACCSensor", "stop");
        mSensorManager.unregisterListener(sensorListener);
    }
}