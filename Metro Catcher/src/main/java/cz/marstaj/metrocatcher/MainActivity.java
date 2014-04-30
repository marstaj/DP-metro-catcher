package cz.marstaj.metrocatcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Calendar;

import cz.marstaj.metrocatcher.dao.DepartureDataSource;
import cz.marstaj.metrocatcher.model.DayType;
import cz.marstaj.metrocatcher.model.MyTime;
import cz.marstaj.metrocatcher.model.Station;
import cz.marstaj.metrocatcher.util.PrefManager;

public class MainActivity extends ActionBarActivity {

    /**
     * Bundle name
     */
    public final static String NOTIFICATION = "NOTIFICATION";
    /**
     * Activity TAG
     */
    private final String TAG = MainActivity.class.getSimpleName();
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
     *
     */
    private DepartureDataSource dataSource;
    // Views
    private TextView textDir1Value1;
    private TextView textDir1Value2;
    private TextView textDir1Value3;
    private TextView textDir2Value1;
    private TextView textDir2Value2;
    private TextView textDir2Value3;
    private TextView dir1val1Rem;
    private TextView dir1val2Rem;
    private TextView dir1val3Rem;
    private TextView dir2val1Rem;
    private TextView dir2val2Rem;
    private TextView dir2val3Rem;
    private View layoutDir1;
    private View layoutDir2;
    private TextView textStationName;
    private TextView textDir1Name;
    private TextView textDir2Name;
    private TextView textNoStationError;
    private View layoutMetroInfo;
    private TextView textCatch;
    /**
     * Telephony manager
     */
    private TelephonyManager tm;
    /**
     * Upcoming departures for direction 1
     */
    private MyTime[] upDepDir1;
    /**
     * Upcoming departures for direction 2
     */
    private MyTime[] upDepDir2;
    private BroadcastReceiver measureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Double meters = bundle.getDouble(MeasureService.UPDATE_MESSAGE);
                if (meters != null) {
                    displayCatch(meters);
                    textCatch.setText(meters + "");
                } else {
                    displayCatch(-1d);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init dataSource
        dataSource = new DepartureDataSource(this);

        // Init Views
        layoutMetroInfo = findViewById(R.id.layoutMetroInfo);
        textNoStationError = (TextView) findViewById(R.id.textNoStationError);
        textStationName = (TextView) findViewById(R.id.textStationName);
        textDir1Name = (TextView) findViewById(R.id.textDir1Name);
        textDir2Name = (TextView) findViewById(R.id.textDir2Name);
        layoutDir1 = findViewById(R.id.layoutDir1);
        layoutDir2 = findViewById(R.id.layoutDir2);
        textDir1Value1 = (TextView) findViewById(R.id.textDir1Value1);
        textDir1Value2 = (TextView) findViewById(R.id.textDir1Value2);
        textDir1Value3 = (TextView) findViewById(R.id.textDir1Value3);
        textDir2Value1 = (TextView) findViewById(R.id.textDir2Value1);
        textDir2Value2 = (TextView) findViewById(R.id.textDir2Value2);
        textDir2Value3 = (TextView) findViewById(R.id.textDir2Value3);
        textCatch = (TextView) findViewById(R.id.textCatch);
        dir1val1Rem = (TextView) findViewById(R.id.textDirection1Value1Remaining);
        dir1val2Rem = (TextView) findViewById(R.id.textDirection1Value2Remaining);
        dir1val3Rem = (TextView) findViewById(R.id.textDirection1Value3Remaining);
        dir2val1Rem = (TextView) findViewById(R.id.textDirection2Value1Remaining);
        dir2val2Rem = (TextView) findViewById(R.id.textDirection2Value2Remaining);
        dir2val3Rem = (TextView) findViewById(R.id.textDirection2Value3Remaining);
        dir1val1Rem.setText("");
        dir1val2Rem.setText("");
        dir1val3Rem.setText("");
        dir2val1Rem.setText("");
        dir2val2Rem.setText("");
        dir2val3Rem.setText("");

        // Find out whether
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey(NOTIFICATION)) {
            // Activity was started from notification
            Log.d(TAG, "Activity was started from notification");
        } else {
            Log.d(TAG, "Activity was not started from notification");
        }

        // Button for activity measure
        findViewById(R.id.buttonScanActivity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, MeasureActivity.class));
            }
        });

        // Check for saved station
        int stationID = PrefManager.getLastStationID(this);
        if (stationID != -1) {
            dataSource.open();
            Station station = dataSource.getStationByID(stationID);
            dataSource.close();
            if (station != null) {
                processStation(station);
            } else {
                textNoStationError.setVisibility(View.VISIBLE);
                layoutMetroInfo.setVisibility(View.GONE);
            }
        } else {
            textNoStationError.setVisibility(View.VISIBLE);
            layoutMetroInfo.setVisibility(View.GONE);
        }

        // Start bts scan service
        startService(new Intent(this, ScanService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set up checking TBS antennas
        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(cellListener, PhoneStateListener.LISTEN_CELL_LOCATION);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop listening
        if (tm != null) {
            tm.listen(cellListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    private void onNewCell(int cid) {
        dataSource.open();
        int stationID = dataSource.getStationIDByCellID(cid);
        if (stationID != -1) { // TODO nezobrazovat znova stejnou stanici.. ?? dobre pro refresh casu ale narocne? && stationID != activeStation) {

            Station station = dataSource.getStationByID(stationID);
            dataSource.close();

            if (station != null) {

                // Save ID for future uses
                PrefManager.setLastStationID(this, stationID);

                processStation(station);
            }
        } else {
            dataSource.close();
        }
    }

    private int[] convertToIntArray(String[] array) {
        if (array == null) {
            return null;
        }
        int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Integer.valueOf(array[i]);
        }
        return result;
    }

    private void processStation(Station station) {
        // Adjust views, because in this point we want to show the station info :)
        textNoStationError.setVisibility(View.GONE);
        layoutMetroInfo.setVisibility(View.VISIBLE);
        int activeStation = station.getId();

        Calendar now = Calendar.getInstance();
        int minutesFromMidnight = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int today = now.get(Calendar.DAY_OF_WEEK);
        Log.d(TAG, "Day of week is " + today);

        DayType dayType;
        if (today == 1) {
            dayType = DayType.NE;
        } else {
            if (today == 6) {
                dayType = DayType.PA;
            } else {
                if (today == 7) {
                    dayType = DayType.SO;
                } else {
                    dayType = DayType.PO_CT;
                }
            }
        }

        MyTime[] upcomingDeparturesDir1 = getUpcommingDepartures(dayType, station, minutesFromMidnight, true);
        MyTime[] upcomingDeparturesDir2 = getUpcommingDepartures(dayType, station, minutesFromMidnight, false);

        textStationName.setText("Stanice - " + station.getName());

        if (upcomingDeparturesDir1 != null) {
            layoutDir1.setVisibility(View.VISIBLE);
            textDir1Value1.setText(upcomingDeparturesDir1[0].getStringBasicTimeWithDelimiter());
            textDir1Value2.setText(upcomingDeparturesDir1[1].getStringBasicTimeWithDelimiter());
            textDir1Value3.setText(upcomingDeparturesDir1[2].getStringBasicTimeWithDelimiter());

            // TODO Nastavit spravne jmeno smeru
            textDir1Name.setText("Směr Černý most");
        } else {
            layoutDir1.setVisibility(View.GONE);
        }

        if (upcomingDeparturesDir2 != null) {
            layoutDir2.setVisibility(View.VISIBLE);
            textDir2Value1.setText(upcomingDeparturesDir2[0].getStringBasicTimeWithDelimiter());
            textDir2Value2.setText(upcomingDeparturesDir2[1].getStringBasicTimeWithDelimiter());
            textDir2Value3.setText(upcomingDeparturesDir2[2].getStringBasicTimeWithDelimiter());

            // TODO Nastavit spravne jmeno smeru
            textDir2Name.setText("Směr Zličín");
        } else {
            layoutDir2.setVisibility(View.GONE);
        }

        this.upDepDir1 = upcomingDeparturesDir1;
        this.upDepDir2 = upcomingDeparturesDir2;
    }

    private MyTime[] getUpcommingDepartures(DayType dayType, Station station, int minutesFromMidnight, boolean isDirection1) {
        int[] departureTimes;
        if (isDirection1) {
            departureTimes = getDepartures1ForDayType(dayType, station);
        } else {
            departureTimes = getDepartures2ForDayType(dayType, station);
        }

        // Check if this is not the last station. (null == last station)
        if (departureTimes != null) {

            MyTime first = new MyTime();
            MyTime second = new MyTime();
            MyTime third = new MyTime();

            // If there are no more departures this day, we have to change daytype and get departures for the new daytype
            if (departureTimes[departureTimes.length - 1] < minutesFromMidnight) {
                dayType = getNextDayTypeFrom(dayType);
                if (isDirection1) {
                    departureTimes = getDepartures1ForDayType(dayType, station);
                } else {
                    departureTimes = getDepartures2ForDayType(dayType, station);
                }
                first.setMinutesFromMidnight(departureTimes[0]);
                second.setMinutesFromMidnight(departureTimes[1]);
                third.setMinutesFromMidnight(departureTimes[2]);
            }

            // Find upcomming departures
            for (int i = 0; i < departureTimes.length; i++) {
                if (departureTimes[i] >= minutesFromMidnight) {

                    first.setMinutesFromMidnight(departureTimes[i]);

                    // If it is the last departure this day
                    if (i == departureTimes.length - 1) {

                        // We have to change daytype and get first 2 departures for the new daytype
                        dayType = getNextDayTypeFrom(dayType);
                        if (isDirection1) {
                            departureTimes = getDepartures1ForDayType(dayType, station);
                        } else {
                            departureTimes = getDepartures2ForDayType(dayType, station);
                        }
                        second.setMinutesFromMidnight(departureTimes[0]);
                        third.setMinutesFromMidnight(departureTimes[1]);

                    } else {

                        // Get the second departure
                        i++;
                        second.setMinutesFromMidnight(departureTimes[i]);

                        // If the second is the last departure this day
                        if (i == departureTimes.length - 1) {

                            // We have to change daytype and get the third departure for the new daytype
                            dayType = getNextDayTypeFrom(dayType);
                            if (isDirection1) {
                                departureTimes = getDepartures1ForDayType(dayType, station);
                            } else {
                                departureTimes = getDepartures2ForDayType(dayType, station);
                            }
                            third.setMinutesFromMidnight(departureTimes[0]);
                        } else {

                            // Get the third departure
                            i++;
                            third.setMinutesFromMidnight(departureTimes[i]);
                        }
                    }
                    break;
                }
            }
            if (isDirection1) {
                Log.d(TAG, "Direction 1 departure int: " + first.getStringBasicTimeWithDelimiter() + ", " + second.getStringBasicTimeWithDelimiter() + ", " + third.getStringBasicTimeWithDelimiter());
            } else {
                Log.d(TAG, "Direction 2 departure int: " + first.getStringBasicTimeWithDelimiter() + ", " + second.getStringBasicTimeWithDelimiter() + ", " + third.getStringBasicTimeWithDelimiter());
            }
            return new MyTime[]{first, second, third};
        } else {
            return null;
        }
    }

    private int[] getDepartures1ForDayType(DayType dayType, Station station) {
        switch (dayType) {
            case PO_CT: {
                return convertToIntArray(station.getPoct1string());
            }
            case PA: {
                return convertToIntArray(station.getPa1string());
            }
            case SO: {
                return convertToIntArray(station.getSo1string());
            }
            case NE: {
                return convertToIntArray(station.getNe1string());
            }
        }
        return null;
    }

    private int[] getDepartures2ForDayType(DayType dayType, Station station) {
        switch (dayType) {
            case PO_CT: {
                return convertToIntArray(station.getPoct2string());
            }
            case PA: {
                return convertToIntArray(station.getPa2string());
            }
            case SO: {
                return convertToIntArray(station.getSo2string());
            }
            case NE: {
                return convertToIntArray(station.getNe2string());
            }
        }
        return null;
    }

    private DayType getNextDayTypeFrom(DayType dayType) {
        switch (dayType) {
            case PO_CT: {
                return DayType.PA;
            }
            case PA: {
                return DayType.SO;
            }
            case SO: {
                return DayType.NE;
            }
            case NE: {
                return DayType.PO_CT;
            }
        }
        return null;
    }

    private String calculateCatch(MyTime depTime, Double meters) {
        Calendar nowCal = Calendar.getInstance();
        nowCal.set(Calendar.YEAR, 1970);
        nowCal.set(Calendar.MONTH, 0);
        nowCal.set(Calendar.DAY_OF_MONTH, 1);

        Calendar depCal = Calendar.getInstance();
        depCal.set(Calendar.YEAR, 1970);
        depCal.set(Calendar.MONTH, 0);
        depCal.set(Calendar.DAY_OF_MONTH, 1);
        depCal.set(Calendar.HOUR_OF_DAY, depTime.getHours());
        depCal.set(Calendar.MINUTE, depTime.getMinutes());
        depCal.set(Calendar.SECOND, depTime.getSeconds());

        long now = nowCal.getTimeInMillis();
        long dep = depCal.getTimeInMillis();
        double result = 0;

//        Log.d(TAG, "1 - Now: " + now + ", Dep: " + dep);

        // TODO This is for Karlak only
        if (meters < 50) {
            if (meters < 20) {
                if (meters < 10) {
                    result += meters / (3.4 / 2); // The speed is per 2 meters
                } else {
                    result += (meters - 10) / (1.5 / 2);
                    result += 10 / (3.4 / 2); // Calc the rest of the way
                }
            } else {
                result += (meters - 20) / (3.4 / 2);
                result += 10 / (1.5 / 2) + 10 / (3.4 / 2); // Calc the rest of the way
            }
        } else {
            result += (meters - 50) / (1.5 / 2);
            result += 30 / (3.4 / 2) + 10 / (1.5 / 2) + 10 / (3.4 / 2); // Calc the rest of the way
        }

        now += result * 1000;

//        Log.d(TAG, "2 - Now: " + now + ", Dep: " + dep);

        if (now >= dep + 10000) {
            return "Nestíháte, jeďte dalším";
        } else {
            if (now >= dep) {
                return "Musíte popoběhnout abyste stihli";
            } else {
                if (now <= dep - 10000) {
                    return "Stíháte v pohodě";
                } else {
                    return "Stíháte jen tak tak, raději trochu zrychlete";
                }
            }
        }
    }

    private void displayCatch(Double meters) {
        if (meters > 0 && upDepDir1 != null && MeasureService.isRunning) {
            dir1val1Rem.setText(calculateCatch(upDepDir1[0], meters));
            dir1val2Rem.setText(calculateCatch(upDepDir1[1], meters));
            dir1val3Rem.setText(calculateCatch(upDepDir1[2], meters));
        } else {
            dir1val1Rem.setText("");
            dir1val2Rem.setText("");
            dir1val3Rem.setText("");
        }

        if (meters > 0 && upDepDir2 != null && MeasureService.isRunning) {
            dir2val1Rem.setText(calculateCatch(upDepDir2[0], meters));
            dir2val2Rem.setText(calculateCatch(upDepDir2[1], meters));
            dir2val3Rem.setText(calculateCatch(upDepDir2[2], meters));
        } else {
            dir2val1Rem.setText("");
            dir2val2Rem.setText("");
            dir2val3Rem.setText("");
        }
    }

    // -------------------------- BROADCAST RECEIVER --------------------------

    private String convertIntDeparture2String(int intDeparture) {
        String stringDeparture = String.valueOf(intDeparture);
        switch (stringDeparture.length()) {
            case 1: {
                stringDeparture = "00:0" + stringDeparture;
                break;
            }
            case 2: {
                stringDeparture = "00:" + stringDeparture;
                break;
            }
            case 3: {
                stringDeparture = "0" + stringDeparture.substring(0, 1) + ":" + stringDeparture.substring(1, 3);
                break;
            }
            case 4: {
                stringDeparture = stringDeparture.substring(0, 2) + ":" + stringDeparture.substring(2, 4);
                break;
            }

        }
        return stringDeparture;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(measureReceiver, new IntentFilter(MeasureService.UPDATE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(measureReceiver);
    }

}
