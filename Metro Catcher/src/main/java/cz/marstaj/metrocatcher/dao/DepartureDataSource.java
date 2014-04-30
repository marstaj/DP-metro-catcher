package cz.marstaj.metrocatcher.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import cz.marstaj.metrocatcher.model.Station;

/**
 * Created by mastajner on 09/02/14.
 */
public class DepartureDataSource {


    /**
     * Database object
     */
    private SQLiteDatabase database;

    /**
     * Database helper
     */
    private MySQLiteHelper dbHelper;

    public DepartureDataSource(Context context) {
        dbHelper = new MySQLiteHelper(context);
    }

    /**
     * Open connection to the database
     *
     * @throws SQLException
     */
    public void open() throws SQLException {
        Log.w(DepartureDataSource.class.getName(), "Opening DB");
        database = dbHelper.getWritableDatabase();
    }

    /**
     * Close connection to the database
     */
    public void close() {
        Log.w(DepartureDataSource.class.getName(), "Closing DB");
        dbHelper.close();
    }

    /**
     * Get station info by its ID
     *
     * @param stationID
     * @return
     */
    public synchronized Station getStationByID(int stationID) {
        String where = MySQLiteHelper.COLUMN_STATION_ID + " = " + stationID + " LIMIT 1";

        Cursor cursor = database.query(MySQLiteHelper.TABLE_DEPARTURES, null, where, null, null, null, null);
        cursor.moveToFirst();

        Station station = null;
        if (!cursor.isAfterLast()) {
            station = new Station();
            station.setId(cursor.getInt(0));
            station.setName(cursor.getString(1));
            String poct1 = cursor.getString(2);
            String pa1 = cursor.getString(3);
            String so1 = cursor.getString(4);
            String ne1 = cursor.getString(5);
            String poct2 = cursor.getString(6);
            String pa2 = cursor.getString(7);
            String so2 = cursor.getString(8);
            String ne2 = cursor.getString(9);

            if (poct1 != null)
                station.setPoct1string(poct1.split("-"));
            if (pa1 != null)
                station.setPa1string(pa1.split("-"));
            if (so1 != null)
                station.setSo1string(so1.split("-"));
            if (ne1 != null)
                station.setNe1string(ne1.split("-"));

            if (poct2 != null)
                station.setPoct2string(poct2.split("-"));
            if (pa2 != null)
                station.setPa2string(pa2.split("-"));
            if (so2 != null)
                station.setSo2string(so2.split("-"));
            if (ne2 != null)
                station.setNe2string(ne2.split("-"));
        }

        cursor.close();

        return station;
    }

    /**
     * Get station ID by BTS ID
     *
     * @param cid
     * @return
     */
    public synchronized int getStationIDByCellID(int cid) {
        String where = MySQLiteHelper.COLUMN_BTS_ID + " = " + cid + " LIMIT 1";

        Cursor cursor = database.query(MySQLiteHelper.TABLE_BTS_ANTENNAS, null, where, null, null, null, null);
        cursor.moveToFirst();

        int stationId = -1;
        if (!cursor.isAfterLast()) {
            stationId = cursor.getInt(1);
        }

        cursor.close();

        return stationId;
    }
}
