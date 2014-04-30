package cz.marstaj.metrocatcher.util;

import android.content.Context;
import android.preference.PreferenceManager;

public class PrefManager {

    private static final String LAST_STATION = "LAST_STATION";

    public static int getLastStationID(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(LAST_STATION, -1);
    }

    public static boolean setLastStationID(Context context, int stationID) {
        return PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(LAST_STATION, stationID).commit();
    }
}