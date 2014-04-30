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

    /**
     * Service TAG
     */
    private final String TAG = ScanService.class.getSimpleName();

    /**
     * Listener for switching between BTS antenas
     */
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

    /**
     * Telephony manager
     */
    private TelephonyManager tm;

    /**
     * Measure service intent
     */
    private Intent measureServiceIntent;

    /**
     * Bounded measure service
     */
    private MeasureService boundedMeasureService;

    /**
     * Flag wheter the service is running
     */
    private boolean isMeasureServiceBound;

    /**
     * Measure service connection
     */
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

    /**
     * When new cell BTS is available
     *
     * @param cid
     */
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

    /**
     * Whether is measureservice running or not
     * @return
     */
    public boolean isMeasureServiceRunning() {
        return MeasureService.isRunning;
    }

    /**
     * Notify user about localization start
     */
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

    /**
     * Start and bind measure service
     */
    private void startAndBindMeasureService() {
        Log.v(TAG, "MeasureService startAndBind");
        startService(measureServiceIntent);
        bindMeasureService();
    }

    /**
     *  Bind measure service
     */
    private void bindMeasureService() {
        Log.v(TAG, "MeasureService bind");
        bindService(measureServiceIntent, measureServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Stop and unbind measure service
     */
    private void stopAndUnbindMeasureService() {
        Log.v(TAG, "MeasureService stopAndUnbindMeasureService");
        unbindMeasureService();
        stopService(measureServiceIntent);
    }

    /**
     * Unbind measure service
     */
    private void unbindMeasureService() {
        Log.v(TAG, "MeasureService unbindMeasureService");
        unbindService(measureServiceConnection);
        isMeasureServiceBound = false;
    }
}