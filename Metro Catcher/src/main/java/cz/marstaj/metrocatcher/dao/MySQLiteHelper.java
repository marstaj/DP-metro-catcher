package cz.marstaj.metrocatcher.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.IOException;

import cz.marstaj.metrocatcher.util.Util;

/**
 * Created by mastajner on 09/02/14.
 */
public class MySQLiteHelper extends SQLiteOpenHelper {


    /**
     * Departures table name
     */
    public static final String TABLE_DEPARTURES = "departures";
    // Departures table columns
    public static final String COLUMN_STATION_ID = "_id";
    public static final String COLUMN_STATION = "station";
    public static final String COLUMN_DIR_1_PO_CT = "direction1poct";
    public static final String COLUMN_DIR_1_PA = "direction1pa";
    public static final String COLUMN_DIR_1_SO = "direction1so";
    public static final String COLUMN_DIR_1_NE = "direction1ne";
    public static final String COLUMN_DIR_2_PO_CT = "direction2poct";
    public static final String COLUMN_DIR_2_PA = "direction2pa";
    public static final String COLUMN_DIR_2_SO = "direction2so";
    public static final String COLUMN_DIR_2_NE = "direction2ne";

    /**
     * BTS table name
     */
    public static final String TABLE_BTS_ANTENNAS = "bts_antennas";
    // BTS table columns
    public static final String COLUMN_BTS_ID = "cell_id";
    public static final String COLUMN_BTS_STATION_ID = "station_id";


    /**
     * Database name
     */
    private static final String DATABASE_NAME = "departures.db";

    /**
     * Database version
     */
    private static final int DATABASE_VERSION = 1;

    /**
     * Context
     */
    private Context context;

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        try {
            // Parse SQL files and run them
            String departuresSQL = Util.readFromAssets(context, "departures.sql");
            String antennasSQL = Util.readFromAssets(context, "bts_antennas.sql");
            execSQLScript(database, departuresSQL);
            execSQLScript(database, antennasSQL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        onCreate(db);
    }

    private void execSQLScript(SQLiteDatabase database, String sql) {
        for (String str : sql.split(";")) {
            database.execSQL(str + ";");
        }
    }

}
