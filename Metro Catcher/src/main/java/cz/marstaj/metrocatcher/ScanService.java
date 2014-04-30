package cz.marstaj.metrocatcher;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import cz.marstaj.metrocatcher.util.LocalBinder;

/**
 * Created by mastajner on 06/04/14.
 */
public class ScanService extends Service {

    private final String TAG = ScanService.class.getSimpleName();
    PhoneStateListener cellListener = new PhoneStateListener() {
        @Override
        public void onCellLocationChanged(CellLocation location) {
            super.onCellLocationChanged(location);
            GsmCellLocation cellLocation = (GsmCellLocation) location;
            Log.d(TAG, "Cell location changed - " + (short) cellLocation.getCid());

            // Connected to new antenna
            onNewCell((short) cellLocation.getCid());
        }
    };
    private TelephonyManager tm;
    private Intent measureServiceIntent;
    private MeasureService boundedMeasureService;
    private boolean isMeasureServiceBound;
    private ServiceConnection measureServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.v(TAG, "MeasureService ServiceConnection onServiceConnected");
            boundedMeasureService = ((LocalBinder<MeasureService>) service).getService();
            isMeasureServiceBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.v(TAG, "MeasureService ServiceConnection onServiceDisconnected");
            boundedMeasureService = null;
            isMeasureServiceBound = false;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
        measureServiceIntent = new Intent(this, MeasureService.class);

        if (isMeasureServiceRunning() && !isMeasureServiceBound) {
            bindMeasureService();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");

        if (tm == null) {
            Log.v(TAG, "Getting new TelephonyManager");
            tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            // Set up checking TBS antennas
            tm.listen(cellListener, PhoneStateListener.LISTEN_CELL_LOCATION);
        }

        return Service.START_STICKY;
    }

    private void onNewCell(int cid) {
        // TODO for other cells and stations
        // 21297 is in Karlovo namesti in the subway station down there :) goes all the way up to vestibule

        if (cid == 21297) {
            if (!isMeasureServiceRunning()) {
                notifyUser();
                startAndBindMeasureService();
            } else {
                if (!isMeasureServiceBound) {
                    bindMeasureService();
                }
            }
        } else {
            if (isMeasureServiceRunning()) {
                stopAndUnbindMeasureService();
            }
        }
    }

    public boolean isMeasureServiceRunning() {
        return MeasureService.isRunning;
    }

    private void notifyUser() {
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        String text = getString(R.string.notification_text);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.NOTIFICATION, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(getText(R.string.notification_title))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(text)).setContentText(text)
                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                        .setAutoCancel(true);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(MyApp.SCAN_NOTIFICATION, mBuilder.build());
    }

    // ------------------------- MEASURE SERVICE -------------------------

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        // Returns binder for communication
        return new LocalBinder<ScanService>(this);
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

        // Stop listening
        if (tm != null) {
            tm.listen(cellListener, PhoneStateListener.LISTEN_NONE);
        }

        if (isMeasureServiceBound) {
            unbindMeasureService();
        }
    }

    private void startAndBindMeasureService() {
        Log.v(TAG, "MeasureService startAndBind");
        startService(measureServiceIntent);
        bindMeasureService();
    }

    private void bindMeasureService() {
        Log.v(TAG, "MeasureService bind");
        bindService(measureServiceIntent, measureServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void stopAndUnbindMeasureService() {
        Log.v(TAG, "MeasureService stopAndUnbindMeasureService");
        unbindMeasureService();
        stopService(measureServiceIntent);
    }

    private void unbindMeasureService() {
        Log.v(TAG, "MeasureService unbindMeasureService");
        unbindService(measureServiceConnection);
        isMeasureServiceBound = false;
    }
}